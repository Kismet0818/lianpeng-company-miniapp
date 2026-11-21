package com.example.lpsx.common.exception;

import com.example.lpsx.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 * <p>
 * 统一拦截所有异常，返回标准的 Result JSON
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 请求路径存在，但 HTTP 方法不正确（例如用浏览器 GET 打开 POST 登录接口）。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                   HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        return Result.error(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "请求方式不正确，请使用 " + String.join("/", e.getSupportedMethods()));
    }

    /** 不存在的静态资源或接口按 404 返回，不记录为系统异常。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException e, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return Result.error(HttpServletResponse.SC_NOT_FOUND, "请求地址不存在");
    }

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletResponse response) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        if (e.getCode() >= 400 && e.getCode() <= 599) response.setStatus(e.getCode());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 其他未捕获异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletResponse response) {
        log.error("系统异常", e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return Result.error(500, "服务器内部错误，请稍后再试");
    }
}
