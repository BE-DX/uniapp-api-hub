package com.uniapp.apihub.module.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.session.SaSession;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.auth.entity.LoginDTO;
import com.uniapp.apihub.module.auth.entity.User;
import com.uniapp.apihub.module.auth.mapper.UserMapper;
import com.uniapp.apihub.security.CurrentUserContext;
import com.uniapp.apihub.security.UserRoles;
import com.uniapp.apihub.module.system.AppConfigService;
import com.uniapp.apihub.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * 认证服务 — 登录/登出/用户管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final DateTimeFormatter LOGIN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserMapper userMapper;
    private final AppConfigService appConfigService;
    private final CurrentUserContext currentUserContext;
    private final LoginSessionNoticeService loginSessionNoticeService;

    /**
     * 用户登录
     */
    public Map<String, Object> login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if ("B".equals(user.getForbidStatus())) {
            throw new BusinessException("账号已被禁用");
        }
        if (!PasswordUtil.verify(dto.getPassword(), user.getSalt(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String clientType = normalizeClientType(dto.getClientType());
        String clientLabel = clientTypeLabel(clientType);
        String loginIp = getClientIp();
        LocalDateTime loginTime = LocalDateTime.now();
        List<String> oldTokens = new ArrayList<>(StpUtil.getTokenValueListByLoginId(user.getId(), clientType));

        if (!oldTokens.isEmpty() && !Boolean.TRUE.equals(dto.getForceLogin())) {
            throw new LoginConflictException(clientLabel + "有人正在使用", buildLoginConflictData(clientType, clientLabel, oldTokens.get(0)));
        }

        for (String oldToken : oldTokens) {
            loginSessionNoticeService.put(oldToken, buildKickedNotice(clientType, clientLabel, loginIp, loginTime));
        }
        StpUtil.logout(user.getId(), clientType);
        StpUtil.login(user.getId(), clientType);
        String token = StpUtil.getTokenValue();
        StpUtil.getTokenSession().set("clientType", clientType)
                .set("clientLabel", clientLabel)
                .set("loginIp", loginIp)
                .set("loginTime", loginTime);

        // 判断是否需要修改密码（首次登录 或 使用默认密码）
        boolean mustChangePwd = user.getLastLoginTime() == null
                || PasswordUtil.verify(appConfigService.getDefaultPassword(), user.getSalt(), user.getPassword());

        // 更新最后登录时间
        user.setLastLoginTime(loginTime);
        userMapper.updateById(user);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("clientType", clientType);
        result.put("userInfo", currentUserContext.toUserInfo(user));
        result.put("mustChangePassword", mustChangePwd);
        return result;
    }

    private Map<String, Object> buildLoginConflictData(String clientType, String clientLabel, String oldToken) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "LOGIN_CONFLICT");
        data.put("clientType", clientType);
        data.put("clientLabel", clientLabel);
        data.put("message", clientLabel + "有人正在使用这个账号。");
        try {
            SaSession session = StpUtil.getTokenSessionByToken(oldToken);
            data.put("ip", session.get("loginIp"));
            data.put("loginTime", formatLoginTime(session.get("loginTime")));
        } catch (Exception ignored) {
        }
        return data;
    }

    private Map<String, Object> buildKickedNotice(String clientType, String clientLabel, String loginIp, LocalDateTime loginTime) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "SESSION_REPLACED");
        data.put("clientType", clientType);
        data.put("clientLabel", clientLabel);
        data.put("ip", loginIp);
        String formattedTime = formatLoginTime(loginTime);
        data.put("loginTime", formattedTime);
        data.put("message", "你的账号已在" + clientLabel + "登录。IP：" + loginIp + "，时间：" + formattedTime);
        return data;
    }

    private String formatLoginTime(Object value) {
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(LOGIN_TIME_FORMATTER);
        }
        return value == null ? null : String.valueOf(value).replace('T', ' ');
    }

    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.trim().isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String normalizeClientType(String clientType) {
        if (clientType == null || clientType.trim().isEmpty()) {
            return "pc";
        }
        String normalized = clientType.trim().toLowerCase(Locale.ROOT);
        if ("h5-pc".equals(normalized) || "desktop".equals(normalized) || "web".equals(normalized)) {
            return "pc";
        }
        if ("h5-mobile".equals(normalized) || "app".equals(normalized) || "phone".equals(normalized)) {
            return "mobile";
        }
        if ("mp".equals(normalized) || "mini".equals(normalized) || "wechat-miniapp".equals(normalized)) {
            return "miniapp";
        }
        if ("pc".equals(normalized) || "mobile".equals(normalized) || "miniapp".equals(normalized)) {
            return normalized;
        }
        return "pc";
    }

    private String clientTypeLabel(String clientType) {
        if ("mobile".equals(clientType)) {
            return "手机端";
        }
        if ("miniapp".equals(clientType)) {
            return "小程序端";
        }
        return "电脑端";
    }

    /**
     * 登出
     */
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 初始化超级管理员（仅首次使用）
     */
    public Map<String, Object> initAdmin() {
        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "admin"));
        if (exist != null) {
            throw new BusinessException("管理员账号已存在");
        }

        String password = appConfigService.getDefaultPassword();
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hash(password, salt);

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(hash);
        admin.setSalt(salt);
        admin.setRealName("超级管理员");
        admin.setRole(UserRoles.SUPER_ADMIN);
        admin.setForbidStatus("A");
        admin.setCreateTime(LocalDateTime.now());
        userMapper.insert(admin);

        log.info("初始化超级管理员: admin / {}", password);

        Map<String, Object> result = new HashMap<>();
        result.put("username", "admin");
        result.put("password", password);
        result.put("message", "初始化成功，请妥善保管密码，首次登录后修改");
        return result;
    }

    /**
     * 获取当前登录用户信息
     */
    public Map<String, Object> currentUser() {
        return currentUserContext.toUserInfo(currentUserContext.currentUser());
    }

    /**
     * 修改密码
     */
    public void changePassword(String oldPwd, String newPwd) {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (!PasswordUtil.verify(oldPwd, user.getSalt(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        String salt = PasswordUtil.generateSalt();
        user.setPassword(PasswordUtil.hash(newPwd, salt));
        user.setSalt(salt);
        userMapper.updateById(user);
    }

}
