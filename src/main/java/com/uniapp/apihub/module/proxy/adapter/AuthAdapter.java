package com.uniapp.apihub.module.proxy.adapter;

import com.uniapp.apihub.module.system.entity.SystemConfig;
import okhttp3.Request;

/**
 * 认证适配器 — 为不同下游系统提供统一的认证头注入
 */
public interface AuthAdapter {

    /** 支持的认证方式 */
    String authType();

    /**
     * 给请求添加认证信息
     * @param requestBuilder 请求构建器
     * @param system 系统配置（含认证参数）
     */
    void apply(Request.Builder requestBuilder, SystemConfig system);
}
