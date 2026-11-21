package com.example.lpsx.common.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitInterceptorTest {
    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor();
        ReflectionTestUtils.setField(interceptor, "leadLimit", 2);
        ReflectionTestUtils.setField(interceptor, "authLimit", 2);
        ReflectionTestUtils.setField(interceptor, "publicListLimit", 2);
        ReflectionTestUtils.setField(interceptor, "publicSearchLimit", 2);
        ReflectionTestUtils.setField(interceptor, "publicDetailLimit", 10);
        ReflectionTestUtils.setField(interceptor, "writeLimit", 2);
        ReflectionTestUtils.setField(interceptor, "distinctDetailLimit", 2);
        ReflectionTestUtils.setField(interceptor, "windowSeconds", 60L);
        ReflectionTestUtils.setField(interceptor, "sustainedWindowSeconds", 600L);
        ReflectionTestUtils.setField(interceptor, "sustainedMultiplier", 5);
        ReflectionTestUtils.setField(interceptor, "maxPage", 200);
        ReflectionTestUtils.setField(interceptor, "maxPageSize", 500);
        ReflectionTestUtils.setField(interceptor, "trustedProxies", "127.0.0.1/32,10.0.0.0/8");
    }

    @Test
    void returnsRealHttp429WithRetryAfter() throws Exception {
        assertTrue(call("/api/schools/list", "203.0.113.8", null).allowed);
        assertTrue(call("/api/schools/list", "203.0.113.8", null).allowed);
        CallResult rejected = call("/api/schools/list", "203.0.113.8", null);
        assertFalse(rejected.allowed);
        assertEquals(429, rejected.response.getStatus());
        assertEquals("60", rejected.response.getHeader("Retry-After"));
        assertTrue(rejected.response.getContentAsString().contains("\"code\":429"));
    }

    @Test
    void ignoresSpoofedForwardedHeaderFromUntrustedClient() throws Exception {
        assertTrue(call("/api/home/overview", "203.0.113.9", "198.51.100.1").allowed);
        assertTrue(call("/api/home/overview", "203.0.113.9", "198.51.100.2").allowed);
        assertFalse(call("/api/home/overview", "203.0.113.9", "198.51.100.3").allowed);
    }

    @Test
    void trustsForwardedHeaderFromConfiguredProxy() throws Exception {
        assertTrue(call("/api/home/overview", "10.0.0.5", "198.51.100.1").allowed);
        assertTrue(call("/api/home/overview", "10.0.0.5", "198.51.100.2").allowed);
        assertTrue(call("/api/home/overview", "10.0.0.5", "198.51.100.3").allowed);
    }

    @Test
    void blocksManyDifferentSchoolDetails() throws Exception {
        assertTrue(call("/api/schools/1", "203.0.113.10", null).allowed);
        assertTrue(call("/api/schools/2", "203.0.113.10", null).allowed);
        CallResult rejected = call("/api/schools/3", "203.0.113.10", null);
        assertFalse(rejected.allowed);
        assertEquals(429, rejected.response.getStatus());
    }

    @Test
    void blocksAbusivePaginationImmediately() throws Exception {
        MockHttpServletRequest request = request("/api/general-high-schools", "203.0.113.11", null);
        request.setParameter("size", "501");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(429, response.getStatus());
    }

    @Test
    void allowsOptionsWithoutCountingIt() throws Exception {
        MockHttpServletRequest request = request("/api/schools/list", "203.0.113.12", null);
        request.setMethod("OPTIONS");
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void limitsWriteEndpointsSeparately() throws Exception {
        assertTrue(call("/api/school-reactions/1", "POST", "203.0.113.13", null).allowed);
        assertTrue(call("/api/school-reactions/1", "POST", "203.0.113.13", null).allowed);
        assertFalse(call("/api/school-reactions/1", "POST", "203.0.113.13", null).allowed);
        assertTrue(call("/api/home/overview", "GET", "203.0.113.13", null).allowed);
    }

    private CallResult call(String path, String remoteIp, String forwardedFor) throws Exception {
        return call(path, "GET", remoteIp, forwardedFor);
    }

    private CallResult call(String path, String method, String remoteIp, String forwardedFor) throws Exception {
        MockHttpServletRequest request = request(path, remoteIp, forwardedFor);
        request.setMethod(method);
        MockHttpServletResponse response = new MockHttpServletResponse();
        return new CallResult(interceptor.preHandle(request, response, new Object()), response);
    }

    private MockHttpServletRequest request(String path, String remoteIp, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr(remoteIp);
        if (forwardedFor != null) request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }

    private record CallResult(boolean allowed, MockHttpServletResponse response) {}
}
