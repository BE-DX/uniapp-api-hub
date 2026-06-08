package com.galaxy.apihub.common;

import lombok.Data;

/**
 * 统一响应体
 */
@Data
public class ApiResponse<T> {

    private int code;
    private String msg;
    private T data;

    private ApiResponse() {}

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> fail(int code, String msg) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> ApiResponse<T> fail(String msg) {
        return fail(500, msg);
    }
}
