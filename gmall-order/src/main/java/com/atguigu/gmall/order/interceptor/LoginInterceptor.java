package com.atguigu.gmall.order.interceptor;

import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.utils.CookieUtils;
import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.order.config.JwtProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

// 目的获取userId以及userKey
@Component
@EnableConfigurationProperties({JwtProperties.class})
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties properties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserInfo userInfo = new UserInfo();
        String token = CookieUtils.getCookieValue(request, this.properties.getCookieName());

        if (StringUtils.isEmpty(token)) {
            return false;
        }

        // 存在token信息
        try {
            Map<String, Object> infoFromToken = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());
            Long id = Long.valueOf(infoFromToken.get("id").toString());
            userInfo.setUserId(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 把userInfo传递给后续的业务。
        THREAD_LOCAL.set(userInfo);

        return true;
    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 防止内存泄漏   线程池：请求结束不代表线程结束
        THREAD_LOCAL.remove();
    }
}
