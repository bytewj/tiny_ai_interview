package com.surenhao.backend.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    // 注册拦截器，拦截所有请求，校验是否登录
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {

                    // ⚠️⚠️⚠️ 关键修改点：如果是 OPTIONS 请求，直接放行，不校验登录
                    // (这是解决跨域请求 401/500 报错的核心)
                    SaRouter.match(SaHttpMethod.OPTIONS).stop();

                    // 其他请求，必须登录
                    StpUtil.checkLogin();

                }))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/login",  // 登录接口
                        "/api/chat/**",     // WebSocket
                        "/error",           // 错误页
                        "/favicon.ico"      // 图标
                );
    }
}