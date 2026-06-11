package com.uniapp.apihub.module.auth;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.uniapp.apihub.common.ApiResponse;
import com.uniapp.apihub.module.auth.entity.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @SaCheckRole(value = {"superAdmin", "admin"}, mode = SaMode.OR)
    @GetMapping("/list")
    public ApiResponse<List<Company>> list(@RequestParam(defaultValue = "") String keyword,
                                           @RequestParam(defaultValue = "false") Boolean enabledOnly) {
        return ApiResponse.ok(companyService.listCompanies(keyword, enabledOnly));
    }

    @SaCheckRole(value = {"superAdmin", "admin"}, mode = SaMode.OR)
    @PostMapping("/create")
    public ApiResponse<Company> create(@RequestBody Company company) {
        return ApiResponse.ok(companyService.createCompany(company));
    }

    @SaCheckRole(value = {"superAdmin", "admin"}, mode = SaMode.OR)
    @PutMapping("/update")
    public ApiResponse<Company> update(@RequestBody Company company) {
        return ApiResponse.ok(companyService.updateCompany(company));
    }
}
