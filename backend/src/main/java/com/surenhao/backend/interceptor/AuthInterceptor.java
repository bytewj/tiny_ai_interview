package com.surenhao.backend.interceptor;

import com.alibaba.fastjson2.JSON;
import com.surenhao.backend.annotation.Public;
import com.surenhao.backend.entity.LoginUser; // 引入新类
import com.surenhao.backend.exception.ServiceException;
import com.surenhao.backend.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        if (handlerMethod.getMethodAnnotation(Public.class) != null ||
                handlerMethod.getBeanType().getAnnotation(Public.class) != null) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            throw new RuntimeException("401");
        }

        String key = "login:token:" + token;
        String userJson = redisTemplate.opsForValue().get(key);

        if (!StringUtils.hasText(userJson)) {
            throw new ServiceException(401, "登录已过期，请重新登录");
        }

        // 【核心修改】解析为 LoginUser 而不是 SysUser
        LoginUser loginUser = JSON.parseObject(userJson, LoginUser.class);

        // 放入 ThreadLocal
        UserContext.set(loginUser);

        redisTemplate.expire(key, 30, TimeUnit.MINUTES);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.remove();
    }
}