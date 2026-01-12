package com.surenhao.backend.interceptor;

import com.alibaba.fastjson2.JSON;
import com.surenhao.backend.entity.SysUser;
import com.surenhao.backend.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 处理 OPTIONS 预检请求 (跨域必须放行)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 获取 Token (前端 Header 传 Authorization)
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            return true; // 没 Token 也放行，交给 AOP 决定是否拦截
        }

        // 3. 查 Redis
        String key = "login:token:" + token;
        String userJson = redisTemplate.opsForValue().get(key);

        if (StringUtils.hasText(userJson)) {
            // 4. 解析并存入 ThreadLocal
            SysUser user = JSON.parseObject(userJson, SysUser.class);
            UserContext.set(user);

            // 5. 续期 30 分钟
            redisTemplate.expire(key, 30, TimeUnit.MINUTES);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // ⚠️ 极其重要：防止内存泄漏
        UserContext.remove();
    }
}