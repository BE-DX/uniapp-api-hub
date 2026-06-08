package com.galaxy.apihub.module.proxy.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galaxy.apihub.module.system.entity.SystemConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Token认证适配器 — 适用于金蝶K3Cloud等需要appID签名认证的系统
 *
 * authConfig格式: {"appId":"xxx", "appSec":"xxx", "acctId":"xxx", "userName":"xxx", "lcid":"2052"}
 */
@Slf4j
@Component
public class TokenAuthAdapter implements AuthAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String authType() {
        return "TOKEN";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void apply(Request.Builder requestBuilder, SystemConfig system) {
        try {
            Map<String, String> authConfig = objectMapper.readValue(system.getAuthConfig(), Map.class);

            String appId = authConfig.get("appId");
            String appSec = authConfig.get("appSec");
            String acctId = authConfig.getOrDefault("acctId", "");
            String userName = authConfig.getOrDefault("userName", "");
            String lcid = authConfig.getOrDefault("lcid", "2052");

            // 构造 K3Cloud 风格的认证头（可按需扩展支持其他Token类型的签名）
            String appData = acctId + "," + userName + "," + lcid;
            String timestamp = String.valueOf(System.currentTimeMillis());

            requestBuilder.header("X-Kd-Appdata", appData);
            requestBuilder.header("X-Kd-Signature", appSec);
            requestBuilder.header("X-Kd-Appid", appId);
            requestBuilder.header("X-Kd-Timestamp", timestamp);

            log.debug("TokenAuth: system={}, appId={}, acctId={}", system.getSysCode(), appId, acctId);
        } catch (Exception e) {
            log.error("Token认证头注入失败: {}", e.getMessage());
        }
    }
}
