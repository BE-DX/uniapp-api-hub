package com.galaxy.apihub.common;

/**
 * 业务异常
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String msg) {
        super(msg);
        this.code = 500;
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
