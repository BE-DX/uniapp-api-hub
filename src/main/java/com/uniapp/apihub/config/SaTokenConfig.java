package com.uniapp.apihub.config;

import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 配置。
 *
 * 当前使用默认有状态会话模式：
 * - 登录态保存在服务端；
 * - 后端服务重启后，内存中的登录态会失效；
 * - 前端旧 token 再访问受保护接口时会收到 401，并被踢回登录页。
 */
@Configuration
public class SaTokenConfig {
}
