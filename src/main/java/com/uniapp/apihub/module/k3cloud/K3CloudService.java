package com.uniapp.apihub.module.k3cloud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingdee.bos.webapi.entity.IdentifyInfo;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.system.SystemService;
import com.uniapp.apihub.module.system.entity.SystemConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 金蝶云星空 SDK 服务 — 直接调用官方 K3CloudApi 操作
 *
 * 替代旧的路由代理模式，使用金蝶官方 SDK 进行认证和数据操作。
 * K3CloudApi 非线程安全（配置变更时），每个请求创建独立实例。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class K3CloudService {

    private final SystemService systemService;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Gson gson = new Gson();
    private static final int LCID_ZH_CN = 2052;

    /**
     * 保存单据（新建或更新）
     *
     * @param sysCode 系统编码（如 k3cloud）
     * @param formId  表单ID（如 PLN_FORECAST）
     * @param jsonData 完整的 Save JSON
     * @return K3Cloud 返回的原始 JSON
     */
    public String save(String sysCode, String formId, String jsonData) {
        K3CloudApi client = buildClient(sysCode);
        try {
            log.info("K3Cloud save: sys={} formId={}", sysCode, formId);
            String result = client.save(formId, jsonData);
            log.debug("K3Cloud save result: {}", result.length() > 200 ? result.substring(0, 200) + "..." : result);
            return result;
        } catch (Exception e) {
            log.error("K3Cloud save失败: {} {}", formId, e.getMessage());
            throw new BusinessException("金蝶保存失败: " + e.getMessage());
        }
    }

    /**
     * 单据查询
     *
     * @param sysCode      系统编码
     * @param formId       表单ID
     * @param fieldKeys    字段列表，逗号分隔（如 "FNumber,FName"）
     * @param filterString 过滤条件 JSON 字符串（可为空）
     * @return 查询结果 List<List<Object>>
     */
    public List<List<Object>> executeBillQuery(String sysCode, String formId, String fieldKeys,
                                                String filterString) {
        K3CloudApi client = buildClient(sysCode);
        try {
            Map<String, Object> queryMap = new LinkedHashMap<>();
            queryMap.put("FormId", formId);
            queryMap.put("FieldKeys", fieldKeys);

            if (filterString != null && !filterString.trim().isEmpty()) {
                try {
                    JsonArray filterArray = JsonParser.parseString(filterString).getAsJsonArray();
                    queryMap.put("FilterString", filterArray);
                } catch (Exception e) {
                    queryMap.put("FilterString", filterString);
                }
            } else {
                queryMap.put("FilterString", "");
            }

            queryMap.put("OrderString", "");
            queryMap.put("TopRowCount", 0);
            queryMap.put("StartRow", 0);
            queryMap.put("Limit", 10000);
            queryMap.put("SubSystemId", "");

            log.debug("K3Cloud query: sys={} formId={} fieldKeys={}", sysCode, formId, fieldKeys);
            return client.executeBillQuery(gson.toJson(queryMap));
        } catch (Exception e) {
            log.error("K3Cloud查询失败: {} {}", formId, e.getMessage());
            throw new BusinessException("金蝶查询失败: " + e.getMessage());
        }
    }

    /**
     * 查看单据详情
     *
     * @param sysCode 系统编码
     * @param formId  表单ID
     * @param billNo  单据编号
     * @return 单据详情 JSON 字符串
     */
    public String view(String sysCode, String formId, String billNo) {
        K3CloudApi client = buildClient(sysCode);
        try {
            JsonObject params = new JsonObject();
            params.addProperty("CreateOrgId", 0);
            params.addProperty("Number", billNo);
            params.addProperty("IsSortBySeq", true);
            log.debug("K3Cloud view: sys={} formId={} billNo={}", sysCode, formId, billNo);
            return client.view(formId, params.toString());
        } catch (Exception e) {
            log.error("K3Cloud查看失败: {} {}", formId, e.getMessage());
            throw new BusinessException("金蝶查看失败: " + e.getMessage());
        }
    }

    /**
     * 提交单据
     *
     * @param sysCode 系统编码
     * @param formId  表单ID
     * @param data    提交数据（如 {"Numbers": ["BILL-001"]}）
     * @return 提交结果 JSON
     */
    public String submit(String sysCode, String formId, String data) {
        K3CloudApi client = buildClient(sysCode);
        try {
            log.info("K3Cloud submit: sys={} formId={}", sysCode, formId);
            return client.submit(formId, data);
        } catch (Exception e) {
            log.error("K3Cloud提交失败: {} {}", formId, e.getMessage());
            throw new BusinessException("金蝶提交失败: " + e.getMessage());
        }
    }

    /**
     * 审核单据
     *
     * @param sysCode 系统编码
     * @param formId  表单ID
     * @param data    审核数据
     * @return 审核结果 JSON
     */
    public String audit(String sysCode, String formId, String data) {
        K3CloudApi client = buildClient(sysCode);
        try {
            log.info("K3Cloud audit: sys={} formId={}", sysCode, formId);
            return client.audit(formId, data);
        } catch (Exception e) {
            log.error("K3Cloud审核失败: {} {}", formId, e.getMessage());
            throw new BusinessException("金蝶审核失败: " + e.getMessage());
        }
    }

    /**
     * 通用 API 调用
     */
    public String execute(String sysCode, String serviceName, Object[] params) {
        K3CloudApi client = buildClient(sysCode);
        try {
            log.debug("K3Cloud execute: sys={} service={}", sysCode, serviceName);
            return client.execute(serviceName, params);
        } catch (Exception e) {
            log.error("K3Cloud execute失败: {} {}", serviceName, e.getMessage());
            throw new BusinessException("金蝶API调用失败: " + e.getMessage());
        }
    }

    /* ==================== 工具方法 ==================== */

    /**
     * 判断保存结果是否成功
     */
    public static boolean isSuccess(String resultJson) {
        try {
            JsonNode root = objectMapper.readTree(resultJson);
            JsonNode status = root.path("Result").path("ResponseStatus");
            return status.path("IsSuccess").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从保存结果中提取单据编号
     */
    public static String extractBillNo(String resultJson) {
        try {
            JsonNode root = objectMapper.readTree(resultJson);
            JsonNode result = root.path("Result");
            if (result.has("Number")) {
                return result.get("Number").asText();
            }
            // 尝试从 SuccessEntitys 中提取
            JsonNode entities = result.path("ResponseStatus").path("SuccessEntitys");
            if (entities.isArray() && entities.size() > 0) {
                return entities.get(0).path("Number").asText();
            }
        } catch (Exception e) {
            log.warn("提取单据编号失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 查询结果取第一条数据第一个单元格
     */
    public static String firstCell(List<List<Object>> result) {
        if (result != null && !result.isEmpty() && !result.get(0).isEmpty()) {
            Object cell = result.get(0).get(0);
            return cell != null ? cell.toString() : null;
        }
        return null;
    }

    /**
     * 从 SystemConfig 构建 K3CloudApi 实例
     */
    private K3CloudApi buildClient(String sysCode) {
        SystemConfig sys = systemService.getSystemForProxy(sysCode);
        if (!"TOKEN".equals(sys.getAuthType())) {
            throw new BusinessException("K3Cloud 仅支持 TOKEN 认证，当前系统配置为: " + sys.getAuthType());
        }

        try {
            JsonNode auth = objectMapper.readTree(sys.getAuthConfig());
            String appId = auth.path("appId").asText();
            String appSec = auth.path("appSec").asText();
            String userName = auth.path("username").asText();
            String acctID = auth.path("acctID").asText();

            if (appId.isEmpty() || appSec.isEmpty()) {
                throw new BusinessException("系统 " + sysCode + " 的 authConfig 缺少 appId 或 appSec");
            }
            if (acctID.isEmpty()) {
                throw new BusinessException("系统 " + sysCode + " 的 authConfig 缺少 acctID");
            }

            IdentifyInfo identifyInfo = new IdentifyInfo();
            identifyInfo.setServerUrl(sys.getBaseUrl());
            identifyInfo.setdCID(acctID);
            identifyInfo.setAppId(appId);
            identifyInfo.setAppSecret(appSec);
            identifyInfo.setUserName(userName);
            identifyInfo.setlCID(LCID_ZH_CN);
            return new K3CloudApi(identifyInfo);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("构建K3CloudApi失败: {}", e.getMessage());
            throw new BusinessException("K3Cloud SDK 初始化失败: " + e.getMessage());
        }
    }
}
