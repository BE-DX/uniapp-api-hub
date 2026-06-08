package com.uniapp.apihub.module.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.proxy.adapter.AuthAdapter;
import com.uniapp.apihub.module.system.SystemService;
import com.uniapp.apihub.module.system.entity.ApiRoute;
import com.uniapp.apihub.module.system.entity.SystemConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 代理引擎 — 核心路由+转发逻辑
 *
 * 工作流：查系统配置 → 查路由 → 注入认证 → 请求转换 → 发送 → 响应转换
 */
@Slf4j
@Service
public class ProxyService {

    private final SystemService systemService;
    private final Map<String, AuthAdapter> authAdapters;
    private final ObjectMapper objectMapper;
    private OkHttpClient httpClient;

    @Value("${proxy.connect-timeout:30}")
    private int connectTimeout;
    @Value("${proxy.read-timeout:60}")
    private int readTimeout;
    @Value("${proxy.max-idle-connections:20}")
    private int maxIdleConnections;
    @Value("${proxy.keep-alive-duration:300}")
    private int keepAliveDuration;

    public ProxyService(SystemService systemService,
                        List<AuthAdapter> adapterList,
                        ObjectMapper objectMapper) {
        this.systemService = systemService;
        this.objectMapper = objectMapper;
        // AuthAdapter Map: authType → adapter实例
        java.util.HashMap<String, AuthAdapter> map = new java.util.HashMap<>();
        for (AuthAdapter adapter : adapterList) {
            map.put(adapter.authType(), adapter);
        }
        this.authAdapters = map;
    }

    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.SECONDS))
                .build();
        log.info("代理引擎初始化完成: connectTimeout={}s, readTimeout={}s", connectTimeout, readTimeout);
    }

    @PreDestroy
    public void destroy() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }

    /**
     * 执行代理请求
     *
     * @param sysCode   系统编码，如 k3cloud
     * @param routeKey  路由标识，如 save / query
     * @param params    前端传来的请求参数(JSON字符串)
     * @return 下游返回的原始JSON字符串
     */
    public String execute(String sysCode, String routeKey, String params) {
        // 1. 查系统配置
        SystemConfig sys = systemService.getSystem(sysCode);

        // 2. 查路由
        ApiRoute route = systemService.getRoute(sys.getId(), routeKey);
        if (route == null) {
            throw new BusinessException("未找到路由: " + sysCode + "/" + routeKey);
        }

        // 3. 构造目标URL
        String targetUrl = buildTargetUrl(sys.getBaseUrl(), route.getTargetPath());
        log.info("代理转发: {} {} → {}", sysCode, routeKey, targetUrl);

        // 4. 构造请求
        Request.Builder requestBuilder = new Request.Builder().url(targetUrl);

        // 注入认证头
        AuthAdapter authAdapter = authAdapters.get(sys.getAuthType());
        if (authAdapter != null) {
            authAdapter.apply(requestBuilder, sys);
        } else if (!"NONE".equals(sys.getAuthType())) {
            log.warn("未知认证方式: {}, 跳过认证头注入", sys.getAuthType());
        }

        // 5. 请求体转换
        String body = transformRequest(params, route.getReqTransform());

        // 设置请求方法和请求体
        String httpMethod = route.getHttpMethod() != null ? route.getHttpMethod().toUpperCase() : "POST";
        if ("GET".equals(httpMethod)) {
            requestBuilder.get();
        } else {
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(body != null ? body : "", mediaType);
            if ("POST".equals(httpMethod)) {
                requestBuilder.post(requestBody);
            } else if ("PUT".equals(httpMethod)) {
                requestBuilder.put(requestBody);
            } else if ("DELETE".equals(httpMethod)) {
                requestBuilder.delete(requestBody);
            } else {
                requestBuilder.post(requestBody);
            }
        }

        // 6. 发送请求
        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("代理响应: {} {} → HTTP {} ({} bytes)",
                    sysCode, routeKey, response.code(), responseBody.length());

            if (!response.isSuccessful()) {
                log.warn("下游返回非200: {} {} → HTTP {} body={}",
                        sysCode, routeKey, response.code(),
                        responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody);
            }

            // 7. 响应转换
            return transformResponse(responseBody, route.getRespTransform());

        } catch (IOException e) {
            log.error("代理请求失败: {} {} → {}", sysCode, routeKey, e.getMessage());
            throw new BusinessException("请求下游系统失败: " + e.getMessage());
        }
    }

    /**
     * 构造完整的目标URL
     */
    private String buildTargetUrl(String baseUrl, String targetPath) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = targetPath.startsWith("/") ? targetPath : "/" + targetPath;
        return base + path;
    }

    /**
     * 请求转换 — 根据模板重写参数
     * 模板格式: {"FormId":"${formId}", "FieldKeys":"${fieldKeys}", ...}
     * 变量从params中取值，未匹配到的变量使用原值
     */
    @SuppressWarnings("unchecked")
    private String transformRequest(String params, String transformTemplate) {
        if (transformTemplate == null || transformTemplate.isEmpty()) {
            return params; // 无模板 → 透传
        }
        try {
            JsonNode paramsNode = objectMapper.readTree(params);
            JsonNode templateNode = objectMapper.readTree(transformTemplate);
            // 简单替换：模板中的 ${xxx} 用 params 中的 xxx 值替换
            String result = transformTemplate;
            if (paramsNode.isObject()) {
                java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = paramsNode.fields();
                while (fields.hasNext()) {
                    java.util.Map.Entry<String, JsonNode> field = fields.next();
                    result = result.replace("${" + field.getKey() + "}", field.getValue().asText());
                }
            }
            // 清理未替换的占位符
            result = result.replaceAll("\\$\\{[^}]+\\}", "");
            return result;
        } catch (Exception e) {
            log.warn("请求转换失败，使用原始参数: {}", e.getMessage());
            return params;
        }
    }

    /**
     * 响应转换 — 目前直接透传，预留扩展点
     */
    private String transformResponse(String responseBody, String transformTemplate) {
        if (transformTemplate == null || transformTemplate.isEmpty()) {
            return responseBody;
        }
        // TODO: 支持响应字段映射/重命名/过滤
        return responseBody;
    }
}
