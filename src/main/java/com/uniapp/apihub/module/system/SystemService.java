package com.uniapp.apihub.module.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.system.entity.ApiRoute;
import com.uniapp.apihub.module.system.entity.SystemConfig;
import com.uniapp.apihub.module.system.mapper.ApiRouteMapper;
import com.uniapp.apihub.module.system.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统管理服务 — 系统及路由的CRUD
 */
@Service
@RequiredArgsConstructor
public class SystemService {

    private final SystemConfigMapper systemConfigMapper;
    private final ApiRouteMapper apiRouteMapper;

    /* ==================== 系统配置 ==================== */

    public List<SystemConfig> listSystems() {
        return systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getEnabled, true));
    }

    public SystemConfig getSystem(String sysCode) {
        SystemConfig sys = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getSysCode, sysCode)
                .eq(SystemConfig::getEnabled, true));
        if (sys == null) {
            throw new BusinessException("系统不存在或已禁用: " + sysCode);
        }
        return sys;
    }

    public SystemConfig addSystem(SystemConfig config) {
        systemConfigMapper.insert(config);
        return config;
    }

    public SystemConfig updateSystem(SystemConfig config) {
        systemConfigMapper.updateById(config);
        return config;
    }

    public void deleteSystem(Long id) {
        systemConfigMapper.deleteById(id);
    }

    /* ==================== API路由 ==================== */

    public List<ApiRoute> listRoutes(Long systemId) {
        return apiRouteMapper.selectList(new LambdaQueryWrapper<ApiRoute>()
                .eq(ApiRoute::getSystemId, systemId)
                .eq(ApiRoute::getEnabled, true));
    }

    public ApiRoute getRoute(Long systemId, String routeKey) {
        return apiRouteMapper.selectOne(new LambdaQueryWrapper<ApiRoute>()
                .eq(ApiRoute::getSystemId, systemId)
                .eq(ApiRoute::getRouteKey, routeKey)
                .eq(ApiRoute::getEnabled, true));
    }

    public ApiRoute addRoute(ApiRoute route) {
        apiRouteMapper.insert(route);
        return route;
    }

    public ApiRoute updateRoute(ApiRoute route) {
        apiRouteMapper.updateById(route);
        return route;
    }

    public void deleteRoute(Long id) {
        apiRouteMapper.deleteById(id);
    }
}
