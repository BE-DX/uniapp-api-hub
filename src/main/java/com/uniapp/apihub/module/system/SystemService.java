package com.uniapp.apihub.module.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.security.CurrentUser;
import com.uniapp.apihub.security.CurrentUserContext;
import com.uniapp.apihub.module.system.entity.SystemConfig;
import com.uniapp.apihub.module.system.enums.SystemTypeEnum;
import com.uniapp.apihub.module.system.mapper.SystemConfigMapper;
import com.uniapp.apihub.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API 系统配置服务。
 *
 * 安全策略：
 * - authConfig 入库前统一 AES 加密；
 * - 返回前端时整体脱敏，只返回 ******，不暴露任何字段结构和值；
 * - 内部适配器调用 getSystemForProxy() 时才拿到解密后的明文配置；
 * - authType 由 sysCode 对应的系统类型决定，前端不需要让管理员维护认证方式。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemService {

    private final SystemConfigMapper systemConfigMapper;
    private final CryptoUtil cryptoUtil;
    private final CurrentUserContext currentUserContext;

    private static final String AUTH_CONFIG_MASK = "******";

    /**
     * 列出所有启用的 API 系统，认证配置整体脱敏。
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
     * 列出登录用户可见的业务系统摘要。
     *
     * 这里只返回前端业务入口需要的信息，不暴露 Base URL 和认证配置。
     */
    public List<Map<String, Object>> listAvailableSystems() {
        List<SystemConfig> list = systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getEnabled, true));
        CurrentUser currentUser = currentUserContext.currentUser();
        List<Map<String, Object>> result = new ArrayList<>();
        for (SystemConfig sys : list) {
            if (!canAccessSystem(currentUser, sys.getSysCode())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sysCode", sys.getSysCode());
            item.put("sysName", sys.getSysName());
            item.put("enabled", sys.getEnabled());
            item.put("remark", sys.getRemark());
            result.add(item);
        }
        return result;
    }

    private boolean canAccessSystem(CurrentUser user, String sysCode) {
        if (user.getPermissions().contains("*:*") || user.getPermissions().contains(sysCode + ":*")) {
            return true;
        }
        String prefix = sysCode + ":";
        return user.getPermissions().stream().anyMatch(permission -> permission.startsWith(prefix));
    }

    /**
     * 获取单个 API 系统，认证配置整体脱敏。
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
     * 获取解密后的明文 authConfig。
     *
     * 仅供内部适配器或二次验证后的敏感信息查看接口使用。
     */
    public String getAuthConfigPlain(String sysCode) {
        SystemConfig sys = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getSysCode, sysCode)
                .eq(SystemConfig::getEnabled, true));
        if (sys == null) {
            throw new BusinessException("系统不存在或已禁用: " + sysCode);
        }
        String raw = sys.getAuthConfig();
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        return cryptoUtil.isEncrypted(raw) ? cryptoUtil.decrypt(raw) : raw;
    }

    /**
     * 获取内部调用使用的系统配置，authConfig 会解密为明文。
     */
    public SystemConfig getSystemForProxy(String sysCode) {
        SystemConfig sys = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getSysCode, sysCode)
                .eq(SystemConfig::getEnabled, true));
        if (sys == null) {
            throw new BusinessException("系统不存在或已禁用: " + sysCode);
        }

        String raw = sys.getAuthConfig();
        if (raw != null && !raw.isEmpty() && cryptoUtil.isEncrypted(raw)) {
            sys.setAuthConfig(cryptoUtil.decrypt(raw));
        }
        return sys;
    }

    /**
     * 新增 API 系统。
     */
    public SystemConfig addSystem(SystemConfig config) {
        applySystemType(config);

        Long count = systemConfigMapper.selectCount(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getSysCode, config.getSysCode()));
        if (count > 0) {
            throw new BusinessException("系统类型「" + config.getSysName() + "」已存在，每种类型仅支持一个配置");
        }

        String raw = config.getAuthConfig();
        if (raw != null && !raw.isEmpty()) {
            config.setAuthConfig(cryptoUtil.encrypt(raw));
        }

        systemConfigMapper.insert(config);
        config.setAuthConfig(maskAuthConfig(raw));

        log.warn("AUDIT: 新增API系统 sysCode={} sysName={} authType={}",
                config.getSysCode(), config.getSysName(), config.getAuthType());
        return config;
    }

    /**
     * 更新 API 系统。
     */
    public SystemConfig updateSystem(SystemConfig config) {
        SystemConfig db = systemConfigMapper.selectById(config.getId());
        if (db == null) {
            throw new BusinessException("系统不存在");
        }

        applySystemType(db);
        db.setBaseUrl(config.getBaseUrl());
        db.setRemark(config.getRemark());
        db.setEnabled(config.getEnabled());

        String newAuth = config.getAuthConfig();
        if (newAuth != null && !newAuth.isEmpty() && !isMasked(newAuth)) {
            db.setAuthConfig(cryptoUtil.encrypt(newAuth));
        }

        systemConfigMapper.updateById(db);

        log.warn("AUDIT: 更新API系统 id={} sysCode={} sysName={} authType={}",
                db.getId(), db.getSysCode(), db.getSysName(), db.getAuthType());

        db.setAuthConfig(maskAuthConfig(db.getAuthConfig()));
        return db;
    }

    /**
     * 删除 API 系统。
     */
    public void deleteSystem(Long id) {
        SystemConfig db = systemConfigMapper.selectById(id);
        if (db != null) {
            log.warn("AUDIT: 删除API系统 id={} sysCode={} sysName={}",
                    db.getId(), db.getSysCode(), db.getSysName());
        }
        systemConfigMapper.deleteById(id);
    }

    /**
     * 根据系统类型枚举补齐系统名称和认证方式。
     */
    private void applySystemType(SystemConfig config) {
        try {
            SystemTypeEnum type = SystemTypeEnum.fromCode(config.getSysCode());
            config.setSysName(type.getName());
            config.setAuthType(type.getAuthType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("未知系统类型: " + config.getSysCode());
        }
    }

    /**
     * 认证配置返回前端时整体脱敏。
     */
    private String maskAuthConfig(String authConfig) {
        if (authConfig == null || authConfig.isEmpty()) {
            return authConfig;
        }
        return AUTH_CONFIG_MASK;
    }

    /**
     * 判断前端传回来的认证配置是否只是脱敏占位符。
     */
    private boolean isMasked(String value) {
        return value != null && value.trim().matches("\\*+");
    }
}
