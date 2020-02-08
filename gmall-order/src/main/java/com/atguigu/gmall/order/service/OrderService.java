package com.atguigu.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVO;
import com.atguigu.gmall.oms.vo.OrderSubmitVO;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVO;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private GmallOmsClient omsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

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

    public void submit(OrderSubmitVO orderSubmitVO) {

        // 1.校验是否重复提交（是：提示， 否：跳转到支付页面，创建订单）
        // 判断redis中有没有，有-说明是第一次提交，放行并删除redis中的orderToken
        String orderToken = orderSubmitVO.getOrderToken();
//        String token = this.redisTemplate.opsForValue().get(TOKEN_PREFIX + orderSubmitVO.getOrderToken());
//        if (StringUtils.isEmpty(token)){
//            return ;
//        }
//        this.redisTemplate.delete(TOKEN_PREFIX + orderSubmitVO.getOrderToken());

        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long flag = (Long)this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(TOKEN_PREFIX + orderSubmitVO.getOrderToken()), orderToken);
        if (flag == 0) {
            throw new OrderException("请不要重复提交订单");
        }

        // 2.验价（总价格是否发生了变化）
        BigDecimal totalPrice = orderSubmitVO.getTotalPrice(); // 获取页面提交的总价格
        // 获取数据库的实时价格
        List<OrderItemVO> items = orderSubmitVO.getItems();
        if (CollectionUtils.isEmpty(items)) {
            throw new OrderException("请勾选要购买的商品！");
        }
        BigDecimal currentTotalPrice = items.stream().map(orderItemVO -> {
            Long skuId = orderItemVO.getSkuId();
            Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity != null) {
                return skuInfoEntity.getPrice().multiply(new BigDecimal(orderItemVO.getCount())); // 获取每一个sku的实时价格 * count
            }
            return new BigDecimal(0);
        }).reduce((a, b) -> a.add(b)).get();

        // 比较价格是否一致
        if(totalPrice.compareTo(currentTotalPrice) != 0) {
            throw new OrderException("页面已过期，请刷新后再试！");
        }

        // 3.验库存并锁定库存（具备原子性，支付完成之后，才是真正的减库存）
        List<SkuLockVO> skuLockVOS = items.stream().map(orderItemVO -> {
            SkuLockVO skuLockVO = new SkuLockVO();
            skuLockVO.setSkuId(orderItemVO.getSkuId());
            skuLockVO.setCount(orderItemVO.getCount());
            skuLockVO.setOrderToken(orderSubmitVO.getOrderToken());
            return skuLockVO;
        }).collect(Collectors.toList());
        Resp<List<SkuLockVO>> skuLockResp = this.wmsClient.checkAndLock(skuLockVOS);
        List<SkuLockVO> lockVOS = skuLockResp.getData();
        if (!CollectionUtils.isEmpty(lockVOS)) {
            throw new OrderException(JSON.toJSONString(lockVOS));
        }

        // 异常 ： 后续订单无法创建，定时释放库存

        // 4.新增订单（订单状态：未付款的状态）
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        try {
            this.omsClient.saveOrder(orderSubmitVO, userInfo.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
            // 订单创建异常应该立马释放库存： feign（阻塞）  消息队列（异步）
            this.amqpTemplate.convertAndSend("ORDER-EXCHANGE", "stock.unlock", orderSubmitVO.getOrderToken());

            throw new OrderException("订单保存失败，服务错误！");
        }

        // 5.删除购物车中的相应记录
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", userInfo.getUserId());
            List<Long> skuIds = items.stream().map(orderItemVO -> orderItemVO.getSkuId()).collect(Collectors.toList());
            map.put("skuIds", JSON.toJSONString(skuIds));
            this.amqpTemplate.convertAndSend("ORDER-EXCHANGE", "cart.delete", map);
        } catch (AmqpException e) {
            e.printStackTrace();
        }
    }
}
