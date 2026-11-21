package com.example.lpsx.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.lpsx.common.interceptor.AdminKeyInterceptor;
import com.example.lpsx.common.interceptor.JwtInterceptor;
import com.example.lpsx.common.interceptor.RateLimitInterceptor;

import jakarta.annotation.Resource;

/**
 * WebMvc 配置 — 注册 JWT 拦截器 + 管理密钥拦截器 + 限流拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private JwtInterceptor jwtInterceptor;

    @Resource
    private AdminKeyInterceptor adminKeyInterceptor;

    @Resource
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 分级限流：写接口 + 公开查询（先限流，后鉴权）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                        "/api/lead/**", "/api/auth/**", "/api/user/**", "/api/schools/**",
                        "/api/junior-schools/**", "/api/general-high-schools/**", "/api/primary-schools/**", "/api/appointment-schools/**",
                        "/api/kindergartens/**", "/api/school-enrollments/**", "/api/school-feature-classes/**",
                        "/api/score-query/**", "/api/guide/**", "/api/admission-content/**", "/api/parent-qa/**", "/api/lottery-guide/**", "/api/live/**",
                        "/api/home/**", "/api/open-day/**", "/api/config/**",
                        "/api/advisor/**",
                        "/api/school-compare/**",
                        "/api/school-materials/**",
                        "/api/rankings/**",
                        "/api/blessing/**", "/api/school-reactions/**", "/api/feedback/**",
                        "/api/favorite/**", "/api/oss/policy", "/api/oss/temp/**"
                );

        // 管理密钥拦截器：校验管理接口 + 管理端 OSS 上传的 X-Admin-Key
        registry.addInterceptor(adminKeyInterceptor)
                .addPathPatterns("/api/admin/**", "/api/oss/upload", "/api/oss/upload-batch", "/api/oss/delete");

        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")           // 拦截所有 /api/**
                .excludePathPatterns(
                        "/api/auth/wx-login",         // 微信登录不拦截；手机号绑定必须鉴权
                        "/api/admin/**",              // 管理接口仅由 X-Admin-Key 鉴权
                        "/api/oss/upload",            // 管理端上传仅由 X-Admin-Key 鉴权
                        "/api/oss/upload-batch",
                        "/api/oss/delete",
                        "/api/oss/temp/**",           // 临时小程序图标上传：无 JWT，固定目录并受限流/文件校验保护
                        "/api/schools/**",            // 学校列表、详情、对比不拦截
                        "/api/home/**",               // 首页不拦截
                        "/api/assessment/**",         // 择校匹配不拦截
                        "/api/advisor/**",            // 免费规则型升学顾问（公开）
                        "/api/config/**",             // 平台配置（公开）不拦截
                        "/api/guide/**",              // 择校指南不拦截
                        "/api/admission-content/**",  // 招生文章公开查询
                        "/api/parent-qa/**",          // 家长问答公开查询
                        "/api/lottery-guide/**",     // 摇号指南公开查询
                        "/api/open-day/**",           // 学校开放日（公开）不拦截
                        "/api/live/**",               // 视频号直播（公开）不拦截
                        "/api/channels-videos/**",   // 视频号短视频（公开）不拦截
                        "/api/content-links/**",      // 公众号与直播链接（公开只读）
                        "/api/score-query/**",        // 查分工具（公开）不拦截
                        "/api/school-compare/**",     // 体制内学校搜索与对比（公开）
                        "/api/general-high-schools/**", // 一般高中列表、详情（公开）
                        "/api/junior-schools/**",     // 初中学校查询（公开）
                        "/api/primary-schools/**",    // 小学学校查询（公开）
                        "/api/kindergartens/**",     // 幼儿园学校查询（公开）
                        "/api/school-enrollments/**", // 民办小初招生摇号数据（公开）
                        "/api/appointment-schools/**",
                        "/api/school-feature-classes/**",
                        "/api/school-materials",    // 资料列表公开；下载接口必须登录以记录用户
                        "/api/school-materials/*",  // 资料详情公开（不匹配更深一层的 /download）
                        "/api/rankings/**",        // 学校排行榜（公开）
                        "/api/blessing/list",         // 祝福墙列表（公开）不拦截
                        "/api/duikou/**",             // 查对口资料（公开只读）不拦截
                        "/v3/api-docs/**",            // Knife4j 文档
                        "/swagger-ui/**",
                        "/doc.html",
                        "/swagger-ui.html"
                );
    }
}
