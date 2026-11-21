package com.example.lpsx.common.interceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.lpsx.common.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 后台管理密钥拦截器
 * <p>
 * 校验 /api/admin/** 和 /api/oss/** 请求头中的 X-Admin-Key，
 * 防止管理接口被任意访问（轻量方案 A，后续可升级为账号体系）
 */
@Component
public class AdminKeyInterceptor implements HandlerInterceptor {

    @Value("${admin.key}")
    private String adminKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String key = request.getHeader("X-Admin-Key");
        if (!StringUtils.hasText(key) || !adminKey.equals(key)) {
            throw new BusinessException(401, "无权限访问管理接口");
        }
        return true;
    }
}
