package com.atguigu.gmall.order.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVO;
import com.atguigu.gmall.order.config.AlipayTemplate;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVO;
import com.atguigu.gmall.order.vo.PayAsyncVo;
import com.atguigu.gmall.order.vo.PayVo;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("confirm")
    public Resp<OrderConfirmVO> confirm(){

        OrderConfirmVO orderConfirmVO = this.orderService.confirm();
        return Resp.ok(orderConfirmVO);
    }

    /**
     * 提交订单
     * 结束之后，直接弹出一个支付页面（调用支付宝的支付接口）
     * @param orderSubmitVO
     * @return
     */
    @PostMapping("submit")
    public Resp<Object> submit(@RequestBody OrderSubmitVO orderSubmitVO){

        OrderEntity orderEntity = this.orderService.submit(orderSubmitVO);
        if (orderEntity != null) {
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderEntity.getOrderSn());
            payVo.setTotal_amount(orderEntity.getTotalAmount().toString());
            payVo.setSubject("谷粒商城");
            payVo.setBody("谷粒商城支付平台");
            try {
                String form = alipayTemplate.pay(payVo);
                System.out.println("支付页面表单：" + form);
            } catch (AlipayApiException e) {
                e.printStackTrace();
            }
        }
        return Resp.ok(null);
    }

    /**
     * 支付宝支付成功后的异步回调接口
     * 想让别人回调你的接口：
     * 1.自己的独立ip（电信公司：企业专线）
     * 2.买域名
     * 这里解决方案：内网穿透（花生壳/哲西云，众筹）
     * @return
     */
    @PostMapping("pay/success")
    public Resp<Object> paySuccess(PayAsyncVo payAsyncVo){

        // 修改订单状态
        this.amqpTemplate.convertAndSend("ORDER-EXCHANGE", "order.pay", payAsyncVo.getOut_trade_no());
        return Resp.ok(null);
    }

    /**
     * 信号量限流
     * 减redis中的库存
     * 发送消息异步创建订单
     * @param skuId
     * @return
     */
    @PostMapping("seckill/{skuId}")
    public Resp<Object> seckill(@PathVariable("skuId")Long skuId) {

        String num = this.redisTemplate.opsForValue().get("seckill:num:" + skuId);

        RSemaphore semaphore = this.redissonClient.getSemaphore("seckill:" + skuId);
        semaphore.trySetPermits(Integer.valueOf(num));
        try {
            semaphore.acquire(1);

            this.redisTemplate.opsForValue().decrement("seckill:num:" + skuId);

            UserInfo userInfo = LoginInterceptor.getUserInfo();

            String timeId = IdWorker.getTimeId();
            SkuLockVO skuLockVO = new SkuLockVO();
            skuLockVO.setOrderToken(timeId);
            skuLockVO.setCount(1);
//        skuLockVO.set  添加一个userId字段

            this.amqpTemplate.convertAndSend("order-exchange", "order:seckill", skuLockVO);

            RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("seckill:latch:" + userInfo.getUserId());
            countDownLatch.trySetCount(1);
            // 在oms微服务中创建完订单之后调用countdown

            semaphore.release();
            return Resp.ok(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Resp.fail("秒杀失败！");
        }
    }

    @GetMapping
    public Resp<OrderEntity> querySeckill() throws InterruptedException {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("seckill:latch:" + userInfo.getUserId());
        countDownLatch.await();

        // 查询订单业务流程
        return Resp.ok(null);
    }

}
