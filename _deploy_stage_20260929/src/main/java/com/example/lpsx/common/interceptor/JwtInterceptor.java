package com.example.lpsx.common.interceptor;

import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.utils.JwtUtils;
import com.example.lpsx.entity.User;
import com.example.lpsx.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 鉴权拦截器
 * <p>
 * 从 Authorization Header 提取 Token → 校验 → 注入 userId 到 Request
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private UserMapper userMapper;

    private final Map<Integer, Long> activeWriteTimes = new ConcurrentHashMap<>();
    private static final long ACTIVE_WRITE_INTERVAL_MS = 5 * 60 * 1000L;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "未授权，请先登录");
        }

        try {
            Integer userId = jwtUtils.getUserIdFromToken(token);
            User user = userMapper.selectById(userId);
            if (user == null) {
                // 常见于切换数据库、清理测试数据后仍携带旧 JWT。
                // 这不是“账号被停用”，应提示前端清除 token 并重新登录。
                throw new BusinessException(401, "登录状态已失效，请重新登录");
            }
            User canonicalUser = resolveCanonicalUser(user);
            if (Integer.valueOf(0).equals(canonicalUser.getStatus())) {
                throw new BusinessException(403, "账号已被停用");
            }
            Integer canonicalUserId = canonicalUser.getId();
            request.setAttribute("userId", canonicalUserId);
            updateLastActiveIfNeeded(canonicalUserId);
            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(401, "Token 无效或已过期，请重新登录");
        }
    }

    /**
     * 业务规则：手机号相同视为同一客户。历史上同一手机号可能对应多个 OpenID，
     * 所有已绑定手机号的请求统一归到最早创建的用户记录，避免行为和留资继续分裂。
     */
    private User resolveCanonicalUser(User user) {
        if (!StringUtils.hasText(user.getPhone())) return user;
        User canonical = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, user.getPhone())
                .orderByAsc(User::getId)
                .last("LIMIT 1"));
        return canonical == null ? user : canonical;
    }

    private void updateLastActiveIfNeeded(Integer userId) {
        long now = System.currentTimeMillis();
        Long previous = activeWriteTimes.putIfAbsent(userId, now);
        if (previous != null && now - previous < ACTIVE_WRITE_INTERVAL_MS) return;
        activeWriteTimes.put(userId, now);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getLastActiveTime, LocalDateTime.now()));
    }

    /** 从请求头提取 Bearer Token */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
