package com.uniapp.apihub.module.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.auth.entity.LoginDTO;
import com.uniapp.apihub.module.auth.entity.User;
import com.uniapp.apihub.module.auth.mapper.UserMapper;
import com.uniapp.apihub.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务 — 登录/登出/用户管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;

    /** 默认角色 */
    private static final String DEFAULT_ROLE = "user";
    /** 超级管理员角色 */
    private static final String SUPER_ADMIN_ROLE = "superAdmin";
    /** 默认初始密码 */
    private static final String DEFAULT_PASSWORD = "password123";

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

        // Sa-Token 登录，自动生成JWT
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 判断是否需要修改密码（首次登录）
        boolean mustChangePwd = user.getLastLoginTime() == null;

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", toUserInfo(user));
        result.put("mustChangePassword", mustChangePwd);
        return result;
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

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hash(DEFAULT_PASSWORD, salt);

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(hash);
        admin.setSalt(salt);
        admin.setRealName("超级管理员");
        admin.setRole(SUPER_ADMIN_ROLE);
        admin.setForbidStatus("A");
        admin.setCreateTime(LocalDateTime.now());
        userMapper.insert(admin);

        log.info("初始化超级管理员: admin / {}", DEFAULT_PASSWORD);

        Map<String, Object> result = new HashMap<>();
        result.put("username", "admin");
        result.put("password", DEFAULT_PASSWORD);
        result.put("message", "初始化成功，请妥善保管密码，首次登录后修改");
        return result;
    }

    /**
     * 获取当前登录用户信息
     */
    public Map<String, Object> currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserInfo(user);
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

    private Map<String, Object> toUserInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("userId", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("role", user.getRole());
        info.put("phone", user.getPhone());
        info.put("email", user.getEmail());
        return info;
    }
}
