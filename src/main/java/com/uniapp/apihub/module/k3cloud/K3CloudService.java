package com.uniapp.apihub.module.k3cloud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.kingdee.bos.webapi.entity.IdentifyInfo;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.system.SystemService;
import com.uniapp.apihub.module.system.entity.SystemConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 金蝶云星空 SDK 服务。
 *
 * 每次请求都基于系统配置创建独立 K3CloudApi，避免不同系统配置之间互相污染。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class K3CloudService {

    private final SystemService systemService;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Gson gson = new Gson();
    private static final int DEFAULT_LCID = 2052;

    public String save(String sysCode, String formId, String jsonData) {
        K3CloudApi client = buildClient(sysCode);
        try {
            log.info("K3Cloud save: sys={} formId={}", sysCode, formId);
            return client.save(formId, jsonData);
        } catch (Exception e) {
            log.error("K3Cloud 保存失败: {} {}", formId, e.getMessage());
            throw new BusinessException("金蝶保存失败: " + e.getMessage());
        }
    }

    /**
     * 单据查询。
     *
     * queryParams 支持金蝶 ExecuteBillQuery 的完整参数：
     * FormId、FieldKeys、FilterString、OrderString、TopRowCount、StartRow、Limit、SubSystemId。
     */
    public List<List<Object>> executeBillQuery(String sysCode, Map<String, Object> queryParams) {
        K3CloudApi client = buildClient(sysCode);
        Map<String, Object> queryMap = buildQueryMap(queryParams);
        String formId = String.valueOf(queryMap.get("FormId"));

        try {
            String queryJson = gson.toJson(queryMap);
            log.debug("K3Cloud query: sys={} params={}", sysCode, queryJson);
            String rawResult = client.executeBillQueryJson(queryJson);
            List<List<Object>> rows = parseBillQueryRows(rawResult);
            assertQuerySuccess(rows);
            return rows;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("K3Cloud 查询失败: {}", formId, e);
            throw new BusinessException("金蝶查询失败: " + errorMessage(e));
        }
    }

    /**
     * 兼容旧调用签名。
     */
    public List<List<Object>> executeBillQuery(String sysCode, String formId, String fieldKeys, String filterString) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("FormId", formId);
        params.put("FieldKeys", fieldKeys);
        params.put("FilterString", filterString);
        return executeBillQuery(sysCode, params);
    }

    public String view(String sysCode, String formId, String billNo) {
        K3CloudApi client = buildClient(sysCode);
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("CreateOrgId", 0);
            params.put("Number", billNo);
            params.put("IsSortBySeq", true);
            log.debug("K3Cloud view: sys={} formId={} billNo={}", sysCode, formId, billNo);
            return client.view(formId, gson.toJson(params));
        } catch (Exception e) {
            log.error("K3Cloud 查看失败: {} {}", formId, e.getMessage());
            throw new BusinessException("金蝶查看失败: " + e.getMessage());
        }
    }

    public String submit(String sysCode, String formId, String data) {
        K3CloudApi client = buildClient(sysCode);
        try {
            log.info("K3Cloud submit: sys={} formId={}", sysCode, formId);
            return client.submit(formId, data);
        } catch (Exception e) {
            log.error("K3Cloud 提交失败: {} {}", formId, e.getMessage());
            throw new BusinessException("金蝶提交失败: " + e.getMessage());
        }
    }

    public String audit(String sysCode, String formId, String data) {
        K3CloudApi client = buildClient(sysCode);
        try {
            log.info("K3Cloud audit: sys={} formId={}", sysCode, formId);
            return client.audit(formId, data);
        } catch (Exception e) {
            log.error("K3Cloud 审核失败: {} {}", formId, e.getMessage());
            throw new BusinessException("金蝶审核失败: " + e.getMessage());
        }
    }

    public String execute(String sysCode, String serviceName, Object[] params) {
        K3CloudApi client = buildClient(sysCode);
        try {
            log.debug("K3Cloud execute: sys={} service={}", sysCode, serviceName);
            return client.execute(serviceName, params);
        } catch (Exception e) {
            log.error("K3Cloud 通用调用失败: {} {}", serviceName, e.getMessage());
            throw new BusinessException("金蝶 API 调用失败: " + e.getMessage());
        }
    }

    public static boolean isSuccess(String resultJson) {
        try {
            JsonNode root = objectMapper.readTree(resultJson);
            JsonNode status = root.path("Result").path("ResponseStatus");
            return status.path("IsSuccess").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    public static String extractBillNo(String resultJson) {
        try {
            JsonNode root = objectMapper.readTree(resultJson);
            JsonNode result = root.path("Result");
            if (result.has("Number")) {
                return result.get("Number").asText();
            }
            JsonNode entities = result.path("ResponseStatus").path("SuccessEntitys");
            if (entities.isArray() && entities.size() > 0) {
                return entities.get(0).path("Number").asText();
            }
        } catch (Exception e) {
            log.warn("提取单据编号失败: {}", e.getMessage());
        }
        return null;
    }

    public static String firstCell(List<List<Object>> result) {
        if (result != null && !result.isEmpty() && !result.get(0).isEmpty()) {
            Object cell = result.get(0).get(0);
            return cell != null ? cell.toString() : null;
        }
        return null;
    }

    private List<List<Object>> parseBillQueryRows(String rawResult) {
        if (rawResult == null || rawResult.trim().isEmpty()) {
            throw new BusinessException("金蝶查询返回为空");
        }
        if (rawResult.startsWith("response_error:")) {
            throw new BusinessException("金蝶查询失败: " + rawResult);
        }
        try {
            return objectMapper.readValue(rawResult, new TypeReference<List<List<Object>>>() {
            });
        } catch (Exception e) {
            log.error("K3Cloud 查询结果解析失败，原始返回: {}", rawResult, e);
            throw new BusinessException("金蝶查询结果解析失败: " + errorMessage(e));
        }
    }

    private Map<String, Object> buildQueryMap(Map<String, Object> params) {
        Map<String, Object> queryMap = new LinkedHashMap<>();
        queryMap.put("FormId", required(params, "FormId", "formId"));
        queryMap.put("FieldKeys", required(params, "FieldKeys", "fieldKeys"));
        queryMap.put("FilterString", normalizeFilterString(valueOrDefault(params, "", "FilterString", "filterString")));
        queryMap.put("OrderString", valueOrDefault(params, "", "OrderString", "orderString"));
        queryMap.put("TopRowCount", intValue(valueOrDefault(params, 0, "TopRowCount", "topRowCount"), 0));
        queryMap.put("StartRow", intValue(valueOrDefault(params, 0, "StartRow", "startRow"), 0));
        queryMap.put("Limit", normalizeLimit(valueOrDefault(params, 200, "Limit", "limit")));
        queryMap.put("SubSystemId", valueOrDefault(params, "", "SubSystemId", "subSystemId"));
        return queryMap;
    }

    private Object normalizeFilterString(Object value) {
        if (!(value instanceof String)) {
            return value;
        }
        String text = ((String) value).trim();
        if (text.isEmpty()) {
            return "";
        }
        if (!text.startsWith("[") && !text.startsWith("{")) {
            return text;
        }
        try {
            return objectMapper.readValue(text, Object.class);
        } catch (Exception e) {
            throw new BusinessException("FilterString 不是合法 JSON: " + e.getMessage());
        }
    }

    private Object required(Map<String, Object> params, String... keys) {
        Object value = valueOrDefault(params, null, keys);
        if (value == null || value.toString().trim().isEmpty()) {
            throw new BusinessException(String.join("/", keys) + " 为必填项");
        }
        return value;
    }

    private Object valueOrDefault(Map<String, Object> params, Object defaultValue, String... keys) {
        if (params == null) {
            return defaultValue;
        }
        for (String key : keys) {
            if (params.containsKey(key) && params.get(key) != null) {
                return params.get(key);
            }
        }
        return defaultValue;
    }

    private int normalizeLimit(Object value) {
        int limit = intValue(value, 200);
        if (limit <= 0) {
            return 200;
        }
        return Math.min(limit, 10000);
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

    /**
     * executeBillQuery 异常时，SDK 可能返回一行错误对象而不是抛异常。
     */
    @SuppressWarnings("unchecked")
    private void assertQuerySuccess(List<List<Object>> rows) {
        if (rows == null || rows.isEmpty() || rows.get(0).isEmpty()) {
            return;
        }
        Object firstCell = rows.get(0).get(0);
        if (!(firstCell instanceof Map)) {
            return;
        }

        Map<String, Object> root = (Map<String, Object>) firstCell;
        Object resultObj = root.get("Result");
        if (!(resultObj instanceof Map)) {
            return;
        }

        Map<String, Object> result = (Map<String, Object>) resultObj;
        Object statusObj = result.get("ResponseStatus");
        if (!(statusObj instanceof Map)) {
            return;
        }

        Map<String, Object> status = (Map<String, Object>) statusObj;
        Object success = status.get("IsSuccess");
        if (Boolean.TRUE.equals(success)) {
            return;
        }

        throw new BusinessException("金蝶查询失败: " + extractErrorMessage(status));
    }

    @SuppressWarnings("unchecked")
    private String extractErrorMessage(Map<String, Object> status) {
        Object errorsObj = status.get("Errors");
        if (errorsObj instanceof List && !((List<?>) errorsObj).isEmpty()) {
            Object first = ((List<?>) errorsObj).get(0);
            if (first instanceof Map) {
                Object message = ((Map<String, Object>) first).get("Message");
                if (message != null && !message.toString().trim().isEmpty()) {
                    return message.toString();
                }
            }
        }
        Object code = status.get("ErrorCode");
        return code == null ? "未知错误" : "错误码 " + code;
    }

    private K3CloudApi buildClient(String sysCode) {
        SystemConfig sys = systemService.getSystemForProxy(sysCode);
        if (!"TOKEN".equalsIgnoreCase(sys.getAuthType())) {
            throw new BusinessException("K3Cloud 仅支持 TOKEN 认证，当前系统配置为: " + sys.getAuthType());
        }

        try {
            JsonNode auth = objectMapper.readTree(sys.getAuthConfig());
            String appId = text(auth, "appId", "appID", "AppId", "X-KDApi-AppId");
            String appSecret = text(auth, "appSec", "appSecret", "app_secret", "AppSecret", "X-KDApi-AppSecret");
            String userName = text(auth, "username", "userName", "UserName", "X-KDApi-UserName");
            String acctId = text(auth, "acctID", "acctId", "dCID", "dcId", "X-KDApi-AcctID");
            int lcid = intValue(text(auth, "lcid", "lCID", "Lcid", "X-KDApi-Lcid"), DEFAULT_LCID);

            if (isBlank(appId) || isBlank(appSecret)) {
                throw new BusinessException("系统 " + sysCode + " 的认证信息缺少 appId 或 appSecret/appSec");
            }
            if (isBlank(acctId)) {
                throw new BusinessException("系统 " + sysCode + " 的认证信息缺少 acctID/acctId");
            }
            if (isBlank(userName)) {
                throw new BusinessException("系统 " + sysCode + " 的认证信息缺少 username/userName");
            }

            IdentifyInfo identifyInfo = new IdentifyInfo();
            identifyInfo.setServerUrl(sys.getBaseUrl());
            identifyInfo.setdCID(acctId);
            identifyInfo.setAppId(appId);
            identifyInfo.setAppSecret(appSecret);
            identifyInfo.setUserName(userName);
            identifyInfo.setlCID(lcid);
            return new K3CloudApi(identifyInfo);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("构建 K3CloudApi 失败: {}", e.getMessage());
            throw new BusinessException("K3Cloud SDK 初始化失败: " + e.getMessage());
        }
    }

    private String text(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull() && !value.asText().trim().isEmpty()) {
                return value.asText().trim();
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String errorMessage(Exception e) {
        if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }
}
