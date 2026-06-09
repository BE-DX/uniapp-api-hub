package com.uniapp.apihub.module.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.system.entity.SystemConfig;
import com.uniapp.apihub.module.system.mapper.SystemConfigMapper;
import com.uniapp.apihub.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 系统管理服务 — 系统及路由的CRUD
 *
 * authConfig 安全策略：
 * - 写入DB前用 AES-256 加密
 * - 返回前端时脱敏（密码类字段替换为 ******）
 * - ProxyService 调用 getAuthConfigPlain() 获取解密明文
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemService {

    private final SystemConfigMapper systemConfigMapper;
    private final CryptoUtil cryptoUtil;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 需要脱敏的 JSON key 名
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "appSec", "password", "pwd", "secret", "token", "apiKey",
            "cookieValue", "accessToken", "refreshToken", "privateKey",
            "clientSecret", "client_secret", "app_secret", "appSecret"
    ));
    private static final Pattern MASK_PATTERN = Pattern.compile(
            "(\"(?i)(" + String.join("|", SENSITIVE_KEYS) + ")\")\\s*:\\s*\"([^\"]+)\""
    );

    /* ==================== 系统配置 ==================== */

    /**
     * 列出所有系统（前端用，authConfig已脱敏）
     */
    public List<SystemConfig> listSystems() {
        List<SystemConfig> list = systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getEnabled, true));
        for (SystemConfig sys : list) {
            sys.setAuthConfig(maskAuthConfig(sys.getAuthConfig()));
        }
        return list;
    }

    /**
     * 获取单个系统（前端用，authConfig已脱敏）
     */
    public SystemConfig getSystem(String sysCode) {
        SystemConfig sys = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getSysCode, sysCode)
                .eq(SystemConfig::getEnabled, true));
        if (sys == null) {
            throw new BusinessException("系统不存在或已禁用: " + sysCode);
        }
        sys.setAuthConfig(maskAuthConfig(sys.getAuthConfig()));
        return sys;
    }

    /**
     * 获取解密后的明文 authConfig（仅内部ProxyService调用）
     */
    public String getAuthConfigPlain(String sysCode) {
        SystemConfig sys = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getSysCode, sysCode)
                .eq(SystemConfig::getEnabled, true));
        if (sys == null) {
            throw new BusinessException("系统不存在或已禁用: " + sysCode);
        }
        String raw = sys.getAuthConfig();
        if (raw == null || raw.isEmpty()) return raw;
        // 兼容旧数据：已加密则解密，否则直接返回
        return cryptoUtil.isEncrypted(raw) ? cryptoUtil.decrypt(raw) : raw;
    }

    /**
     * 获取系统（内部代理用，authConfig解密不脱敏）
     */
    public SystemConfig getSystemForProxy(String sysCode) {
        SystemConfig sys = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getSysCode, sysCode)
                .eq(SystemConfig::getEnabled, true));
        if (sys == null) {
            throw new BusinessException("系统不存在或已禁用: " + sysCode);
        }
        // 解密 authConfig 供AuthAdapter使用
        String raw = sys.getAuthConfig();
        if (raw != null && !raw.isEmpty() && cryptoUtil.isEncrypted(raw)) {
            sys.setAuthConfig(cryptoUtil.decrypt(raw));
        }
        return sys;
    }

    /**
     * 新增系统 — authConfig 加密后存储
     */
    public SystemConfig addSystem(SystemConfig config) {
        // 每种类型系统仅支持一个配置
        Long count = systemConfigMapper.selectCount(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getSysCode, config.getSysCode()));
        if (count > 0) {
            throw new BusinessException("系统类型「" + config.getSysName() + "」已存在，每种类型仅支持一个配置");
        }

        // 加密 authConfig 后写入
        String raw = config.getAuthConfig();
        if (raw != null && !raw.isEmpty()) {
            config.setAuthConfig(cryptoUtil.encrypt(raw));
        }
        systemConfigMapper.insert(config);
        // 返回前脱敏
        config.setAuthConfig(raw != null ? maskAuthConfig(raw) : null);
        log.warn("AUDIT: 新增API系统 sysCode={} sysName={} authType={}",
                config.getSysCode(), config.getSysName(), config.getAuthType());
        return config;
    }

    /**
     * 更新系统 — authConfig 加密后存储
     */
    public SystemConfig updateSystem(SystemConfig config) {
        SystemConfig db = systemConfigMapper.selectById(config.getId());
        if (db == null) {
            throw new BusinessException("系统不存在");
        }

        // 基础字段更新
        db.setSysName(config.getSysName());
        db.setBaseUrl(config.getBaseUrl());
        db.setAuthType(config.getAuthType());
        db.setRemark(config.getRemark());
        db.setEnabled(config.getEnabled());

        // authConfig 处理：如果前端传了非脱敏值（非全******），则更新并加密
        String newAuth = config.getAuthConfig();
        if (newAuth != null && !newAuth.isEmpty() && !isAllMasked(newAuth)) {
            db.setAuthConfig(cryptoUtil.encrypt(newAuth));
        }
        // 如果传空或全脱敏，保持原值不变

        systemConfigMapper.updateById(db);
        log.warn("AUDIT: 更新API系统 id={} sysCode={} sysName={} authType={}",
                db.getId(), db.getSysCode(), db.getSysName(), db.getAuthType());

        // 返回时脱敏
        db.setAuthConfig(maskAuthConfig(newAuth != null ? newAuth : ""));
        return db;
    }

    /**
     * 删除系统
     */
    public void deleteSystem(Long id) {
        SystemConfig db = systemConfigMapper.selectById(id);
        if (db != null) {
            log.warn("AUDIT: 删除API系统 id={} sysCode={} sysName={}",
                    db.getId(), db.getSysCode(), db.getSysName());
        }
        systemConfigMapper.deleteById(id);
    }

    /* ==================== 脱敏工具 ==================== */

    /**
     * 脱敏 authConfig JSON：将敏感字段值替换为 ******
     */
    private String maskAuthConfig(String authConfigJson) {
        if (authConfigJson == null || authConfigJson.isEmpty()) return authConfigJson;
        try {
            // 先解密（如果是加密的），再脱敏
            String plain = cryptoUtil.isEncrypted(authConfigJson)
                    ? cryptoUtil.decrypt(authConfigJson) : authConfigJson;
            // 正则替换敏感字段值
            String masked = MASK_PATTERN.matcher(plain).replaceAll("$1:\"******\"");
            return masked;
        } catch (Exception e) {
            log.warn("authConfig脱敏失败, 返回原始值: {}", e.getMessage());
            return authConfigJson;
        }
    }

    /**
     * 判断字符串是否全部是脱敏占位符（前端没改密码时返回 ******）
     */
    private boolean isAllMasked(String value) {
        if (value == null) return false;
        try {
            Map<String, Object> map = objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (SENSITIVE_KEYS.contains(entry.getKey())) {
                    Object v = entry.getValue();
                    if (v != null && v.toString().contains("****")) {
                        continue; // 已脱敏，跳过
                    }
                    return false; // 有敏感字段未脱敏
                }
            }
            return true; // 所有敏感字段都是 ******
        } catch (Exception e) {
            return false;
        }
    }
}
