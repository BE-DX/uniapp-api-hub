package com.uniapp.apihub.module.hub;

import java.util.Map;

/**
 * 下游系统适配器。
 *
 * 每个外部系统自己的协议细节都放在这里处理：
 * SDK 调用、HTTP 签名、Token 换取、响应归一化等。
 */
public interface SystemAdapter {

    /**
     * 系统编码，对应 sys_system_config.sys_code。
     */
    String sysCode();

    /**
     * 执行一个归一化后的系统能力。
     */
    Object execute(String operation, Map<String, Object> payload);
}
