package com.uniapp.apihub.module.system.enums;

import java.util.*;

/**
 * API系统类型枚举 — 控制前端可选的系统类型
 *
 * 新增下游系统在此枚举加一项，前端自动出现该选项
 */
public enum SystemTypeEnum {

    K3CLOUD("k3cloud", "金蝶云星空企业版", "TOKEN",
            "金蝶云星空企业版，通过 K3CloudApi SDK 直连"),
    JDY("jdy", "金蝶云星辰", "TOKEN",
            "金蝶云星辰（精斗云），HTTP 原生调用");

    private final String code;
    private final String name;
    private final String authType;
    private final String desc;

    SystemTypeEnum(String code, String name, String authType, String desc) {
        this.code = code;
        this.name = name;
        this.authType = authType;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getAuthType() { return authType; }
    public String getDesc() { return desc; }

    /**
     * 所有枚举转为前端下拉列表用的 VO 列表
     */
    public static List<Map<String, String>> toVoList() {
        List<Map<String, String>> list = new ArrayList<>();
        for (SystemTypeEnum type : values()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", type.code);
            item.put("name", type.name);
            item.put("authType", type.authType);
            item.put("desc", type.desc);
            list.add(item);
        }
        return list;
    }

    /**
     * 根据编码查找枚举
     */
    public static SystemTypeEnum fromCode(String code) {
        for (SystemTypeEnum type : values()) {
            if (type.code.equals(code)) return type;
        }
        throw new IllegalArgumentException("未知系统类型: " + code);
    }
}
