package com.uniapp.apihub.module.k3cloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.hub.SystemAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 金蝶云星空适配器。
 *
 * 这里把中台统一的 operation 转成金蝶官方 SDK 调用。
 */
@Component
@RequiredArgsConstructor
public class K3CloudAdapter implements SystemAdapter {

    private final K3CloudService k3CloudService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String sysCode() {
        return "k3cloud";
    }

    @Override
    public Object execute(String operation, Map<String, Object> payload) {
        if (payload == null) {
            throw new BusinessException("请求体不能为空");
        }

        switch (operation) {
            case "save":
                return parseResult(k3CloudService.save(sysCode(), requiredText(payload, "formId"), toJson(dataOrPayload(payload))));
            case "query":
                return query(payload);
            case "view":
                return parseResult(k3CloudService.view(sysCode(), requiredText(payload, "formId"), requiredText(payload, "billNo")));
            case "submit":
                return parseResult(k3CloudService.submit(sysCode(), requiredText(payload, "formId"), toJson(dataOrPayload(payload))));
            case "audit":
                return parseResult(k3CloudService.audit(sysCode(), requiredText(payload, "formId"), toJson(dataOrPayload(payload))));
            case "execute":
                return executeSdk(payload);
            default:
                throw new BusinessException("金蝶云星空暂不支持该操作: " + operation);
        }
    }

    private Object query(Map<String, Object> payload) {
        List<List<Object>> rows = k3CloudService.executeBillQuery(sysCode(), payload);

        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("total", rows != null ? rows.size() : 0);
        result.put("startRow", intValue(payload.get("startRow"), 0));
        result.put("limit", intValue(payload.get("limit"), 200));
        return result;
    }

    private Object executeSdk(Map<String, Object> payload) {
        String serviceName = requiredText(payload, "serviceName");
        Object params = payload.get("params");
        Object[] args;
        if (params instanceof List) {
            args = ((List<?>) params).toArray();
        } else if (params instanceof Object[]) {
            args = (Object[]) params;
        } else {
            throw new BusinessException("params 必须是数组");
        }
        return k3CloudService.execute(sysCode(), serviceName, args);
    }

    private Object dataOrPayload(Map<String, Object> payload) {
        Object data = payload.get("data");
        return data != null ? data : payload;
    }

    private String requiredText(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            throw new BusinessException(key + " 为必填项");
        }
        return value.toString();
    }

    private String toJson(Object value) {
        try {
            return value instanceof String ? (String) value : objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("请求体序列化失败: " + e.getMessage());
        }
    }

    private Object parseResult(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return json;
        }
    }

    private int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
