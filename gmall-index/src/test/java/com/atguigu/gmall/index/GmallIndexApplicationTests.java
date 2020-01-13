package com.atguigu.gmall.index;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class GmallIndexApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    void contextLoads() {
        this.redisTemplate.opsForValue().set("name", "柳岩");

//        this.redisTemplate.setKeySerializer();

        System.out.println(this.redisTemplate.opsForValue().get("name"));
    }

}
