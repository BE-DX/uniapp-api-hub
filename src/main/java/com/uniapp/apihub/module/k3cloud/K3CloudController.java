package com.uniapp.apihub.module.k3cloud;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniapp.apihub.common.ApiResponse;
import com.uniapp.apihub.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 金蝶云星空 API 控制器 — SDK 直连模式
 *
 * 替代旧的 ProxyController，直接调用 K3CloudApi SDK
 */
@Slf4j
@RestController
@RequestMapping("/api/k3cloud")
@SaCheckLogin
@RequiredArgsConstructor
public class K3CloudController {

    private final K3CloudService k3CloudService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 保存单据
     */
    @PostMapping("/save/{sysCode}/{formId}")
    public ApiResponse<Object> save(@PathVariable String sysCode,
                                     @PathVariable String formId,
                                     @RequestBody String jsonData) {
        String result = k3CloudService.save(sysCode, formId, jsonData);
        return wrapResult(result);
    }

    /**
     * 单据查询（通用）
     */
    @PostMapping("/query/{sysCode}")
    public ApiResponse<Object> query(@PathVariable String sysCode,
                                      @RequestBody Map<String, Object> body) {
        String formId = (String) body.get("formId");
        String fieldKeys = (String) body.get("fieldKeys");
        String filterString = (String) body.get("filterString");

        if (formId == null || fieldKeys == null) {
            throw new BusinessException("formId 和 fieldKeys 为必填项");
        }

        List<List<Object>> lists = k3CloudService.executeBillQuery(
                sysCode, formId, fieldKeys, filterString);

        // 转换为前端友好的格式
        Map<String, Object> result = new HashMap<>();
        result.put("rows", lists);
        result.put("total", lists != null ? lists.size() : 0);
        return ApiResponse.ok(result);
    }

    /**
     * 查看单据详情
     */
    @PostMapping("/view/{sysCode}/{formId}")
    public ApiResponse<Object> view(@PathVariable String sysCode,
                                     @PathVariable String formId,
                                     @RequestBody Map<String, String> body) {
        String billNo = body.get("billNo");
        if (billNo == null || billNo.isEmpty()) {
            throw new BusinessException("billNo 为必填项");
        }
        String result = k3CloudService.view(sysCode, formId, billNo);
        return wrapResult(result);
    }

    /**
     * 提交单据
     */
    @PostMapping("/submit/{sysCode}/{formId}")
    public ApiResponse<Object> submit(@PathVariable String sysCode,
                                       @PathVariable String formId,
                                       @RequestBody String data) {
        String result = k3CloudService.submit(sysCode, formId, data);
        return wrapResult(result);
    }

    /**
     * 审核单据
     */
    @PostMapping("/audit/{sysCode}/{formId}")
    public ApiResponse<Object> audit(@PathVariable String sysCode,
                                      @PathVariable String formId,
                                      @RequestBody String data) {
        String result = k3CloudService.audit(sysCode, formId, data);
        return wrapResult(result);
    }

    private ApiResponse<Object> wrapResult(String json) {
        try {
            return ApiResponse.ok(objectMapper.readTree(json));
        } catch (Exception e) {
            return ApiResponse.ok(json);
        }
    }
}
