package com.example.lpsx.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API 分级限流器。
 *
 * <p>当前使用单实例内存滑动窗口。生产多实例部署时，各实例计数不共享，
 * 后续应迁移为 Redis 或云缓存实现。</p>
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Pattern DETAIL_PATH = Pattern.compile(
            "^/api/(?:schools|junior-schools|general-high-schools|primary-schools|kindergartens)/\\d+(?:/.*)?$"
                    + "|^/api/score-query/schools/\\d+(?:/.*)?$"
                    + "|^/api/guide/article/\\d+$"
                    + "|^/api/admission-content/articles/\\d+$"
                    + "|^/api/open-day/\\d+$");
    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private static final long CLEANUP_INTERVAL = 500;

    @Value("${rate-limit.lead:5}") private int leadLimit;
    @Value("${rate-limit.auth:30}") private int authLimit;
    @Value("${rate-limit.public-list:120}") private int publicListLimit;
    @Value("${rate-limit.public-search:90}") private int publicSearchLimit;
    @Value("${rate-limit.public-detail:180}") private int publicDetailLimit;
    @Value("${rate-limit.write:30}") private int writeLimit;
    @Value("${rate-limit.temp-upload:10}") private int tempUploadLimit;
    @Value("${rate-limit.distinct-details:45}") private int distinctDetailLimit;
    @Value("${rate-limit.window-seconds:60}") private long windowSeconds;
    @Value("${rate-limit.sustained-window-seconds:600}") private long sustainedWindowSeconds;
    @Value("${rate-limit.sustained-multiplier:5}") private int sustainedMultiplier;
    @Value("${rate-limit.max-page:200}") private int maxPage;
    @Value("${rate-limit.max-page-size:500}") private int maxPageSize;
    @Value("${rate-limit.trusted-proxies:127.0.0.1/32,::1/128,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16}")
    private String trustedProxies;

    private final Map<String, Deque<Long>> requestRecords = new ConcurrentHashMap<>();
    private final Map<String, Deque<DetailVisit>> detailRecords = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        long now = System.currentTimeMillis();
        long windowStart = now - Math.max(windowSeconds, 1) * 1000;
        long sustainedWindowStart = now - Math.max(sustainedWindowSeconds, windowSeconds) * 1000;
        String path = normalizePath(request.getRequestURI());
        String clientIp = resolveClientIp(request);
        RateGroup group = classify(request.getMethod(), path);

        if (isAbusivePagination(request)) {
            return reject(response, request, clientIp, path, "pagination");
        }

        String key = group.key + ":" + clientIp;
        Deque<Long> timestamps = requestRecords.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            prune(timestamps, sustainedWindowStart);
            long recentCount = timestamps.stream().filter(timestamp -> timestamp >= windowStart).count();
            if (recentCount >= Math.max(group.limit, 1)) {
                return reject(response, request, clientIp, path, group.key);
            }
            long sustainedLimit = (long) Math.max(group.limit, 1) * Math.max(sustainedMultiplier, 1);
            if (timestamps.size() >= sustainedLimit) {
                return reject(response, request, clientIp, path, group.key + "-sustained");
            }
            timestamps.addLast(now);
        }

        if ("public-detail".equals(group.key)
                && isDistinctDetailLimitExceeded(clientIp, path, now, windowStart)) {
            return reject(response, request, clientIp, path, "detail-enumeration");
        }

        if (requestCounter.incrementAndGet() % CLEANUP_INTERVAL == 0) cleanup(sustainedWindowStart);
        return true;
    }

    private RateGroup classify(String method, String path) {
        if (path.startsWith("/api/auth/")) return new RateGroup("auth", authLimit);
        if (path.startsWith("/api/lead/")) return new RateGroup("lead", leadLimit);
        if (path.startsWith("/api/oss/temp/")) return new RateGroup("temp-upload", tempUploadLimit);
        if (!"GET".equalsIgnoreCase(method)) return new RateGroup("write", writeLimit);
        if (DETAIL_PATH.matcher(path).matches()) return new RateGroup("public-detail", publicDetailLimit);
        if (path.startsWith("/api/score-query/") || path.startsWith("/api/guide/") || path.endsWith("/list")) {
            return new RateGroup("public-search", publicSearchLimit);
        }
        return new RateGroup("public-list", publicListLimit);
    }

    private boolean isAbusivePagination(HttpServletRequest request) {
        return exceeds(request.getParameter("page"), maxPage)
                || exceeds(request.getParameter("pageNum"), maxPage)
                || exceeds(request.getParameter("size"), maxPageSize)
                || exceeds(request.getParameter("pageSize"), maxPageSize)
                || exceeds(request.getParameter("limit"), maxPageSize);
    }

    private boolean exceeds(String raw, int maximum) {
        if (raw == null || raw.isBlank()) return false;
        try {
            return Long.parseLong(raw) > maximum;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private boolean isDistinctDetailLimitExceeded(String ip, String path, long now, long windowStart) {
        Deque<DetailVisit> visits = detailRecords.computeIfAbsent(ip, ignored -> new ArrayDeque<>());
        synchronized (visits) {
            while (!visits.isEmpty() && visits.peekFirst().timestamp < windowStart) visits.pollFirst();
            Set<String> distinctPaths = new HashSet<>();
            for (DetailVisit visit : visits) distinctPaths.add(visit.path);
            if (!distinctPaths.contains(path) && distinctPaths.size() >= Math.max(distinctDetailLimit, 1)) return true;
            visits.addLast(new DetailVisit(now, path));
            return false;
        }
    }

    private boolean reject(HttpServletResponse response, HttpServletRequest request, String ip,
                           String path, String reason) throws IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank() || requestId.length() > 128) {
            requestId = UUID.randomUUID().toString();
        }
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", String.valueOf(Math.max(windowSeconds, 1)));
        response.setHeader("X-Request-Id", requestId);
        response.getWriter().write("{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}");
        log.warn("security_event=rate_limit reason={} method={} path={} clientIp={} requestId={}",
                reason, request.getMethod(), path, ip, requestId);
        return false;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddress = normalizeIp(request.getRemoteAddr());
        if (!isTrustedProxy(remoteAddress)) return remoteAddress;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) return remoteAddress;
        String candidate = normalizeIp(forwarded.split(",", 2)[0].trim());
        return isValidIp(candidate) ? candidate : remoteAddress;
    }

    private boolean isTrustedProxy(String address) {
        for (String cidr : trustedProxies.split(",")) {
            if (matchesCidr(address, cidr.trim())) return true;
        }
        return false;
    }

    private boolean matchesCidr(String address, String cidr) {
        if (cidr.isBlank()) return false;
        if ("::1/128".equals(cidr) || "::1".equals(cidr)) return "::1".equals(address);
        String[] parts = cidr.split("/", 2);
        Matcher ipMatcher = IPV4.matcher(address);
        Matcher networkMatcher = IPV4.matcher(parts[0]);
        if (!ipMatcher.matches() || !networkMatcher.matches()) return address.equals(parts[0]);
        int bits;
        try {
            bits = parts.length == 2 ? Integer.parseInt(parts[1]) : 32;
        } catch (NumberFormatException ignored) {
            return false;
        }
        if (bits < 0 || bits > 32) return false;
        long ip = ipv4ToLong(ipMatcher);
        long network = ipv4ToLong(networkMatcher);
        if (ip < 0 || network < 0) return false;
        long mask = bits == 0 ? 0 : (0xffffffffL << (32 - bits)) & 0xffffffffL;
        return (ip & mask) == (network & mask);
    }

    private long ipv4ToLong(Matcher matcher) {
        long value = 0;
        for (int i = 1; i <= 4; i++) {
            int octet = Integer.parseInt(matcher.group(i));
            if (octet > 255) return -1;
            value = (value << 8) | octet;
        }
        return value;
    }

    private boolean isValidIp(String address) {
        if ("::1".equals(address)) return true;
        Matcher matcher = IPV4.matcher(address);
        return matcher.matches() && ipv4ToLong(matcher) >= 0;
    }

    private String normalizeIp(String address) {
        if (address == null || address.isBlank()) return "unknown";
        return address.startsWith("::ffff:") ? address.substring(7) : address;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/";
        return path.length() > 512 ? path.substring(0, 512) : path;
    }

    private void prune(Deque<Long> timestamps, long windowStart) {
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) timestamps.pollFirst();
    }

    private void cleanup(long windowStart) {
        requestRecords.entrySet().removeIf(entry -> {
            Deque<Long> values = entry.getValue();
            synchronized (values) {
                prune(values, windowStart);
                return values.isEmpty();
            }
        });
        detailRecords.entrySet().removeIf(entry -> {
            Deque<DetailVisit> values = entry.getValue();
            synchronized (values) {
                while (!values.isEmpty() && values.peekFirst().timestamp < windowStart) values.pollFirst();
                return values.isEmpty();
            }
        });
    }

    private record RateGroup(String key, int limit) {}
    private record DetailVisit(long timestamp, String path) {}
}
