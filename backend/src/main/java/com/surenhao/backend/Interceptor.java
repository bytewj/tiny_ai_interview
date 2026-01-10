package com.surenhao.backend;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class Interceptor implements HandlerInterceptor, WebMvcConfigurer {
}
