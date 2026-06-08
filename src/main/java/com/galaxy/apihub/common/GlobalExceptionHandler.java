package com.galaxy.apihub.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<?> handleNotLogin(NotLoginException e) {
        return ApiResponse.fail(401, "未登录或Token已过期");
    }

    @ExceptionHandler(NotPermissionException.class)
    public ApiResponse<?> handleNotPermission(NotPermissionException e) {
        return ApiResponse.fail(403, "无权限访问");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.fail("系统异常: " + e.getMessage());
    }
}
