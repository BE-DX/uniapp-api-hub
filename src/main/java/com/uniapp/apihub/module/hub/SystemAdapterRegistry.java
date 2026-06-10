package com.uniapp.apihub.module.hub;

import com.uniapp.apihub.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 下游系统适配器注册表。
 *
 * 启动时收集所有 SystemAdapter，后续根据 sysCode 找到对应系统的实现。
 */
@Slf4j
@Component
public class SystemAdapterRegistry {

    private final Map<String, SystemAdapter> adapters = new HashMap<>();

    public SystemAdapterRegistry(List<SystemAdapter> adapterList) {
        for (SystemAdapter adapter : adapterList) {
            SystemAdapter exists = adapters.put(adapter.sysCode(), adapter);
            if (exists != null) {
                throw new IllegalStateException("系统适配器重复注册: " + adapter.sysCode());
            }
            log.info("已注册系统适配器: {}", adapter.sysCode());
        }
    }

    public Object execute(String sysCode, String operation, Map<String, Object> payload) {
        SystemAdapter adapter = adapters.get(sysCode);
        if (adapter == null) {
            throw new BusinessException("暂不支持该系统适配器: " + sysCode);
        }
        return adapter.execute(operation, payload);
    }
}
