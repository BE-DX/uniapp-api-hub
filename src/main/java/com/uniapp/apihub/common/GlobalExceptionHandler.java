package com.uniapp.apihub.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.uniapp.apihub.module.auth.LoginConflictException;
import com.uniapp.apihub.module.auth.LoginSessionNoticeService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final LoginSessionNoticeService loginSessionNoticeService;

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(LoginConflictException.class)
    public ApiResponse<?> handleLoginConflict(LoginConflictException e) {
        return ApiResponse.fail(409, e.getMessage(), e.getData());
    }

    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<?> handleNotLogin(NotLoginException e, HttpServletRequest request) {
        Map<String, Object> notice = loginSessionNoticeService.get(extractToken(request));
        if (notice != null) {
            Object msg = notice.get("message");
            return ApiResponse.fail(401, msg == null ? "当前会话已失效" : String.valueOf(msg), notice);
        }
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

    private String extractToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader("Authorization");
        if (token == null || token.trim().isEmpty()) {
            token = request.getHeader("satoken");
        }
        if (token == null) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return token.trim();
    }
}
