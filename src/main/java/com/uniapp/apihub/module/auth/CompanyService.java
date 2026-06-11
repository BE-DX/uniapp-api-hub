package com.uniapp.apihub.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uniapp.apihub.common.BusinessException;
import com.uniapp.apihub.module.auth.entity.Company;
import com.uniapp.apihub.module.auth.mapper.CompanyMapper;
import com.uniapp.apihub.security.CurrentUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公司管理服务。
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyMapper companyMapper;
    private final CurrentUserContext currentUserContext;

    public List<Company> listCompanies(String keyword, Boolean enabledOnly) {
        currentUserContext.requireAdmin();
        LambdaQueryWrapper<Company> qw = new LambdaQueryWrapper<>();
        if (enabledOnly != null && enabledOnly) {
            qw.eq(Company::getEnabled, true);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.and(q -> q.like(Company::getCompanyCode, keyword)
                    .or()
                    .like(Company::getCompanyName, keyword));
        }
        qw.orderByAsc(Company::getId);
        return companyMapper.selectList(qw);
    }

    public Company createCompany(Company company) {
        currentUserContext.requireAdmin();
        validateCompany(company);
        Long count = companyMapper.selectCount(new LambdaQueryWrapper<Company>()
                .eq(Company::getCompanyCode, company.getCompanyCode()));
        if (count > 0) {
            throw new BusinessException("公司编码已存在");
        }
        if (company.getEnabled() == null) {
            company.setEnabled(true);
        }
        companyMapper.insert(company);
        return company;
    }

    public Company updateCompany(Company company) {
        currentUserContext.requireAdmin();
        if (company.getId() == null) {
            throw new BusinessException("公司ID不能为空");
        }
        validateCompany(company);

        Company db = companyMapper.selectById(company.getId());
        if (db == null) {
            throw new BusinessException("公司不存在");
        }

        Long count = companyMapper.selectCount(new LambdaQueryWrapper<Company>()
                .eq(Company::getCompanyCode, company.getCompanyCode())
                .ne(Company::getId, company.getId()));
        if (count > 0) {
            throw new BusinessException("公司编码已存在");
        }

        db.setCompanyCode(company.getCompanyCode().trim());
        db.setCompanyName(company.getCompanyName().trim());
        db.setRemark(company.getRemark());
        if (company.getEnabled() != null) {
            db.setEnabled(company.getEnabled());
        }
        companyMapper.updateById(db);
        return companyMapper.selectById(company.getId());
    }

    private void validateCompany(Company company) {
        if (company.getCompanyCode() == null || company.getCompanyCode().trim().isEmpty()) {
            throw new BusinessException("公司编码不能为空");
        }
        if (company.getCompanyName() == null || company.getCompanyName().trim().isEmpty()) {
            throw new BusinessException("公司名称不能为空");
        }
        company.setCompanyCode(company.getCompanyCode().trim());
        company.setCompanyName(company.getCompanyName().trim());
    }
}
