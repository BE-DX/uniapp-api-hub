package com.uniapp.apihub.module.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.auth.entity.User;
import com.uniapp.apihub.module.auth.mapper.UserMapper;
import com.uniapp.apihub.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户管理服务 — 操作中台自有 sys_user 表
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    private static final String DEFAULT_PASSWORD = "password123";
    private static final String SUPER_ADMIN = "superAdmin";

    /**
     * 用户列表
     */
    public Page<User> listUsers(String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(User::getUsername, keyword).or().like(User::getRealName, keyword);
        }
        qw.orderByDesc(User::getId);
        return userMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    /**
     * 创建用户
     */
    public User createUser(User user) {
        // 用户名唯一性
        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername()));
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }
        // 生成密码
        String salt = PasswordUtil.generateSalt();
        user.setPassword(PasswordUtil.hash(DEFAULT_PASSWORD, salt));
        user.setSalt(salt);
        if (user.getRole() == null) user.setRole("user");
        if (user.getForbidStatus() == null) user.setForbidStatus("A");
        userMapper.insert(user);
        user.setPassword(null);
        user.setSalt(null);
        return user;
    }

    /**
     * 更新用户
     */
    public User updateUser(User user) {
        User db = userMapper.selectById(user.getId());
        if (db == null) throw new BusinessException("用户不存在");
        if (SUPER_ADMIN.equals(db.getRole())) throw new BusinessException("无法编辑超级管理员");

        // 只更新允许的字段
        db.setRealName(user.getRealName());
        db.setRole(user.getRole());
        db.setPhone(user.getPhone());
        db.setEmail(user.getEmail());
        userMapper.updateById(db);
        db.setPassword(null);
        db.setSalt(null);
        return db;
    }

    /**
     * 删除用户
     */
    public void deleteUser(Long id) {
        User db = userMapper.selectById(id);
        if (db == null) throw new BusinessException("用户不存在");
        if (SUPER_ADMIN.equals(db.getRole())) throw new BusinessException("无法删除超级管理员");
        long currentUserId = StpUtil.getLoginIdAsLong();
        if (id.equals(currentUserId)) throw new BusinessException("不能删除自己");
        userMapper.deleteById(id);
    }

    /**
     * 设置启用/禁用
     */
    public void setUserStatus(Long id, String status) {
        User db = userMapper.selectById(id);
        if (db == null) throw new BusinessException("用户不存在");
        if (SUPER_ADMIN.equals(db.getRole())) throw new BusinessException("无法操作超级管理员");
        long currentUserId = StpUtil.getLoginIdAsLong();
        if (id.equals(currentUserId)) throw new BusinessException("不能禁用自己");
        db.setForbidStatus(status);
        userMapper.updateById(db);
    }

    /**
     * 管理员重置密码
     */
    public void resetPassword(Long id) {
        User db = userMapper.selectById(id);
        if (db == null) throw new BusinessException("用户不存在");
        if (SUPER_ADMIN.equals(db.getRole())) throw new BusinessException("无法重置超级管理员密码");
        String salt = PasswordUtil.generateSalt();
        db.setPassword(PasswordUtil.hash(DEFAULT_PASSWORD, salt));
        db.setSalt(salt);
        db.setLastLoginTime(null); // 清空登录时间，下次登录强制改密
        userMapper.updateById(db);
    }

    /**
     * 获取用户详情
     */
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
            user.setSalt(null);
        }
        return user;
    }
}
