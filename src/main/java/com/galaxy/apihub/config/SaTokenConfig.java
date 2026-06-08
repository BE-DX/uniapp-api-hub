package com.galaxy.apihub.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 配置 — JWT 模式
 */
@Configuration
public class SaTokenConfig {

    @Bean
    public StpLogic stpLogic() {
        return new StpLogicJwtForSimple();
    }
}
