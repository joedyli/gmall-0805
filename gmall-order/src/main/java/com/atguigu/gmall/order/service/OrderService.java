package com.atguigu.gmall.order.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVO;
import com.atguigu.gmall.order.vo.OrderItemVO;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    private static final String TOKEN_PREFIX = "order:token:";

    public OrderConfirmVO confirm() {
        OrderConfirmVO orderConfirmVO = new OrderConfirmVO();

        // 获取用户的登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        // 获取用户地址信息（远程接口）
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            Resp<List<MemberReceiveAddressEntity>> addressResp = this.umsClient.queryAddressesByUserId(userInfo.getUserId());
            List<MemberReceiveAddressEntity> addresses = addressResp.getData();
            orderConfirmVO.setAddresses(addresses);
        }, threadPoolExecutor);

        // 获取订单详情列表（远程接口）
        // 获取购物车中选中的购物车记录（远程调用通过feign）
        CompletableFuture<Void> itemsFuture = CompletableFuture.supplyAsync(() -> {
            return this.cartClient.queryCheckedCarts(userInfo.getUserId());
        }).thenAcceptAsync(carts -> {
            List<OrderItemVO> orderItems = carts.stream().map(cart -> {
                Long skuId = cart.getSkuId();
                Integer count = cart.getCount();
                OrderItemVO orderItemVO = new OrderItemVO();

                orderItemVO.setCount(count);
                orderItemVO.setSkuId(skuId);

                // 查询sku相关信息
                CompletableFuture<Void> skuFuture = CompletableFuture.runAsync(() -> {
                    Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuById(skuId);
                    SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
                    if (skuInfoEntity != null) {
                        orderItemVO.setPrice(skuInfoEntity.getPrice());
                        orderItemVO.setImage(skuInfoEntity.getSkuDefaultImg());
                        orderItemVO.setSkuTitle(skuInfoEntity.getSkuTitle());
                        orderItemVO.setWeight(skuInfoEntity.getWeight());
                    }
                }, threadPoolExecutor);

                // 查询商品的库存信息
                CompletableFuture<Void> storeFuture = CompletableFuture.runAsync(() -> {
                    Resp<List<WareSkuEntity>> wareSkuResp = this.wmsClient.queryWareSkuBySkuId(skuId);
                    List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        orderItemVO.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                    }
                }, threadPoolExecutor);

                // 查询销售属性
                CompletableFuture<Void> saleAttrFuture = CompletableFuture.runAsync(() -> {
                    Resp<List<SkuSaleAttrValueEntity>> saleAttrResp = this.pmsClient.querySaleAttrValueBySkuId(skuId);
                    List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = saleAttrResp.getData();
                    orderItemVO.setSaleAttrs(skuSaleAttrValueEntities);
                }, threadPoolExecutor);

                // 查询营销信息
                CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {
                    Resp<List<ItemSaleVO>> resp = this.smsClient.queryItemSaleVOBySkuId(skuId);
                    List<ItemSaleVO> itemSaleVOS = resp.getData();
                    orderItemVO.setSales(itemSaleVOS);
                }, threadPoolExecutor);

                CompletableFuture.allOf(skuFuture, storeFuture, saleAttrFuture, salesFuture).join();

                return orderItemVO;
            }).collect(Collectors.toList());
            orderConfirmVO.setOrderItems(orderItems);
        }, threadPoolExecutor);

        // 获取用户积分信息（远程接口ums）
        CompletableFuture<Void> boundsFuture = CompletableFuture.runAsync(() -> {
            Resp<MemberEntity> memberEntityResp = this.umsClient.queryMemberById(userInfo.getUserId());
            MemberEntity memberEntity = memberEntityResp.getData();
            if (memberEntity != null) {
                orderConfirmVO.setBounds(memberEntity.getIntegration());
            }
        }, threadPoolExecutor);

        // 防止重复提交的唯一标志
        // uuid 可读性很差
        // redis incr  id长度不一致
        // 分布式id生成器（mybatis-plus提供）
        CompletableFuture<Void> tokenFuture = CompletableFuture.runAsync(() -> {
            String orderToken = IdWorker.getTimeId();
            orderConfirmVO.setOrderToken(orderToken); // 浏览器一份
            this.redisTemplate.opsForValue().set(TOKEN_PREFIX + orderToken, orderToken, 3, TimeUnit.HOURS);// 保存redis一份
        }, threadPoolExecutor);

        CompletableFuture.allOf(addressFuture, itemsFuture, boundsFuture, tokenFuture).join();

        return orderConfirmVO;
    }

}
