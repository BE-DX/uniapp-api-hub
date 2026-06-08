package com.uniapp.apihub.module.proxy.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniapp.apihub.module.system.entity.SystemConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;

/**
 * Basic Auth认证适配器 — 适用于HTTP Basic认证的系统
 *
 * authConfig格式: {"username":"admin", "password":"123456"}
 */
@Slf4j
@Component
public class BasicAuthAdapter implements AuthAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String authType() {
        return "BASIC";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void apply(Request.Builder requestBuilder, SystemConfig system) {
        try {
            Map<String, String> authConfig = objectMapper.readValue(system.getAuthConfig(), Map.class);
            String username = authConfig.get("username");
            String password = authConfig.get("password");
            if (username != null && password != null) {
                String credentials = username + ":" + password;
                String encoded = Base64.getEncoder().encodeToString(credentials.getBytes("UTF-8"));
                requestBuilder.header("Authorization", "Basic " + encoded);
            }
            log.debug("BasicAuth: system={}, username={}", system.getSysCode(), username);
        } catch (Exception e) {
            log.error("BasicAuth认证头注入失败: {}", e.getMessage());
        }
    }
}
