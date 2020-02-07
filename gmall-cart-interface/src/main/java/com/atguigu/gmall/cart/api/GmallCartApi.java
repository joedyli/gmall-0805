package com.atguigu.gmall.cart.api;

import com.atguigu.gmall.cart.pojo.Cart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface GmallCartApi {

    @GetMapping("cart/{userId}")
    public List<Cart> queryCheckedCarts(@PathVariable("userId")Long userId);
}
