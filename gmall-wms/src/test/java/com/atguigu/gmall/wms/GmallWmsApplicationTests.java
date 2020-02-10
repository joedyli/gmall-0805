package com.atguigu.gmall.wms;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class GmallWmsApplicationTests {

    @Autowired
    private AmqpTemplate template;

    @Test
    void contextLoads() {
        // 创建订单
        this.template.convertAndSend("ORDER-EXCHANGE", "order.ttl", "test ttl");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
