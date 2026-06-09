package com.uniapp.apihub.module.system;

import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.system.entity.AppConfig;
import com.uniapp.apihub.module.system.mapper.AppConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用配置服务 — 键值对读写
 */
@Service
@RequiredArgsConstructor
public class AppConfigService {

    private final AppConfigMapper appConfigMapper;

    /**
     * 获取全部配置
     */
    public Map<String, String> getAllConfig() {
        List<AppConfig> list = appConfigMapper.selectList(null);
        Map<String, String> result = new HashMap<>();
        for (AppConfig c : list) {
            result.put(c.getConfigKey(), c.getConfigValue());
        }
        return result;
    }

    /**
     * 获取单个配置值
     */
    public String getConfig(String key, String defaultValue) {
        AppConfig config = appConfigMapper.selectById(key);
        return config != null ? config.getConfigValue() : defaultValue;
    }

    /**
     * 设置配置值
     */
    public void setConfig(String key, String value) {
        AppConfig config = appConfigMapper.selectById(key);
        if (config != null) {
            config.setConfigValue(value);
            appConfigMapper.updateById(config);
        } else {
            config = new AppConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            appConfigMapper.insert(config);
        }
    }

    /**
     * 批量保存配置
     */
    public Map<String, String> saveConfig(Map<String, String> newConfig) {
        for (Map.Entry<String, String> entry : newConfig.entrySet()) {
            String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                throw new BusinessException("配置值不能为空: " + entry.getKey());
            }
            setConfig(entry.getKey(), value);
        }
        return getAllConfig();
    }

    /**
     * 获取默认密码
     */
    public String getDefaultPassword() {
        return getConfig("defaultPassword", "password123");
    }
}
