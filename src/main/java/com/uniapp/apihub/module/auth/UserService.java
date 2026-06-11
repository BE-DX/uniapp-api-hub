package com.uniapp.apihub.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.auth.entity.Company;
import com.uniapp.apihub.module.auth.entity.User;
import com.uniapp.apihub.module.auth.mapper.CompanyMapper;
import com.uniapp.apihub.module.auth.mapper.UserMapper;
import com.uniapp.apihub.security.CurrentUserContext;
import com.uniapp.apihub.security.UserRoles;
import com.uniapp.apihub.module.system.AppConfigService;
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
    private final CompanyMapper companyMapper;
    private final AppConfigService appConfigService;
    private final CurrentUserContext currentUserContext;

    /**
     * 用户列表
     */
    public Page<User> listUsers(String keyword, String role, String status, Long companyId, int pageNum, int pageSize) {
        currentUserContext.requireAdmin();
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(q -> q.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getPhone, keyword)
                    .or()
                    .like(User::getEmail, keyword));
        }
        if (role != null && !role.isEmpty()) {
            qw.eq(User::getRole, role);
        }
        if (status != null && !status.isEmpty()) {
            qw.eq(User::getForbidStatus, status);
        }
        if (companyId != null) {
            qw.eq(User::getCompanyId, companyId);
        }
        qw.orderByDesc(User::getId);
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        enrichCompanyName(page.getRecords());
        return page;
    }

    /**
     * 创建用户
     */
    public User createUser(User user) {
        currentUserContext.requireAdmin();
        normalizeRole(user);
        // 不允许创建超级管理员（只能通过 initAdmin 初始化）
        if (UserRoles.SUPER_ADMIN.equals(user.getRole())) {
            throw new BusinessException("不能创建超级管理员，超级管理员仅可通过系统初始化生成");
        }
        if (UserRoles.ADMIN.equals(user.getRole()) && !UserRoles.isSuperAdmin(currentUserContext.currentUser().getRole())) {
            throw new BusinessException("只有超级管理员可以创建管理员");
        }
        // 用户名唯一性
        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername()));
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }
        // 生成密码
        String salt = PasswordUtil.generateSalt();
        user.setPassword(PasswordUtil.hash(appConfigService.getDefaultPassword(), salt));
        user.setSalt(salt);
        if (user.getRole() == null) user.setRole(UserRoles.USER);
        if (user.getForbidStatus() == null) user.setForbidStatus("A");
        userMapper.insert(user);
        enrichCompanyName(user);
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

        Long currentUserId = currentUserContext.currentUserId();
        boolean updatingSelf = user.getId().equals(currentUserId);
        if (updatingSelf) {
            if (user.getRole() != null && !user.getRole().equals(db.getRole())) {
                throw new BusinessException("不能修改自己的角色");
            }
            db.setRealName(user.getRealName());
            db.setPhone(user.getPhone());
            db.setEmail(user.getEmail());
            userMapper.updateById(db);
            enrichCompanyName(db);
            db.setPassword(null);
            db.setSalt(null);
            return db;
        }

        currentUserContext.requireAdmin();
        normalizeRole(user);
        if (UserRoles.SUPER_ADMIN.equals(db.getRole())) throw new BusinessException("无法编辑超级管理员");
        if (UserRoles.ADMIN.equals(db.getRole()) && !UserRoles.isSuperAdmin(currentUserContext.currentUser().getRole())) {
            throw new BusinessException("只有超级管理员可以编辑管理员");
        }
        // 不能把普通用户改成超级管理员
        if (UserRoles.SUPER_ADMIN.equals(user.getRole()) && !UserRoles.SUPER_ADMIN.equals(db.getRole())) {
            throw new BusinessException("不能将用户提升为超级管理员");
        }
        if (UserRoles.ADMIN.equals(user.getRole()) && !UserRoles.isSuperAdmin(currentUserContext.currentUser().getRole())) {
            throw new BusinessException("只有超级管理员可以将用户设为管理员");
        }

        // 只更新允许的字段
        db.setRealName(user.getRealName());
        db.setRole(user.getRole());
        db.setPhone(user.getPhone());
        db.setEmail(user.getEmail());
        db.setCompanyId(user.getCompanyId());
        userMapper.updateById(db);
        enrichCompanyName(db);
        db.setPassword(null);
        db.setSalt(null);
        return db;
    }

    /**
     * 删除用户
     */
    public void deleteUser(Long id) {
        currentUserContext.requireAdmin();
        User db = userMapper.selectById(id);
        if (db == null) throw new BusinessException("用户不存在");
        if (UserRoles.SUPER_ADMIN.equals(db.getRole())) throw new BusinessException("无法删除超级管理员");
        if (UserRoles.ADMIN.equals(db.getRole()) && !UserRoles.isSuperAdmin(currentUserContext.currentUser().getRole())) {
            throw new BusinessException("只有超级管理员可以删除管理员");
        }
        if (id.equals(currentUserContext.currentUserId())) throw new BusinessException("不能删除自己");
        userMapper.deleteById(id);
    }

    /**
     * 设置启用/禁用
     */
    public void setUserStatus(Long id, String status) {
        currentUserContext.requireAdmin();
        User db = userMapper.selectById(id);
        if (db == null) throw new BusinessException("用户不存在");
        if (UserRoles.SUPER_ADMIN.equals(db.getRole())) throw new BusinessException("无法操作超级管理员");
        if (UserRoles.ADMIN.equals(db.getRole()) && !UserRoles.isSuperAdmin(currentUserContext.currentUser().getRole())) {
            throw new BusinessException("只有超级管理员可以禁用管理员");
        }
        if (id.equals(currentUserContext.currentUserId())) throw new BusinessException("不能禁用自己");
        db.setForbidStatus(status);
        userMapper.updateById(db);
    }

    /**
     * 管理员重置密码
     */
    public void resetPassword(Long id) {
        currentUserContext.requireAdmin();
        User db = userMapper.selectById(id);
        if (db == null) throw new BusinessException("用户不存在");
        if (UserRoles.SUPER_ADMIN.equals(db.getRole())) throw new BusinessException("无法重置超级管理员密码");
        if (UserRoles.ADMIN.equals(db.getRole()) && !UserRoles.isSuperAdmin(currentUserContext.currentUser().getRole())) {
            throw new BusinessException("只有超级管理员可以重置管理员密码");
        }
        String salt = PasswordUtil.generateSalt();
        db.setPassword(PasswordUtil.hash(appConfigService.getDefaultPassword(), salt));
        db.setSalt(salt);
        db.setLastLoginTime(null); // 清空登录时间，下次登录强制改密
        userMapper.updateById(db);
    }

    /**
     * 获取用户详情
     */
    public User getUserById(Long id) {
        currentUserContext.requireSelfOrAdmin(id);
        User user = userMapper.selectById(id);
        if (user != null) {
            enrichCompanyName(user);
            user.setPassword(null);
            user.setSalt(null);
        }
        return user;
    }

    private void enrichCompanyName(User user) {
        if (user == null || user.getCompanyId() == null) {
            return;
        }
        Company company = companyMapper.selectById(user.getCompanyId());
        if (company != null) {
            user.setCompanyName(company.getCompanyName());
        }
    }

    private void enrichCompanyName(java.util.List<User> users) {
        if (users == null) {
            return;
        }
        for (User user : users) {
            enrichCompanyName(user);
        }
    }

    private void normalizeRole(User user) {
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole(UserRoles.USER);
            return;
        }
        if (!UserRoles.isValid(user.getRole())) {
            throw new BusinessException("不支持的用户角色: " + user.getRole());
        }
    }
}
