package com.uniapp.apihub.module.auth.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公司信息。
 */
@Data
@TableName("sys_company")
public class Company {

    @TableId(type = IdType.AUTO)
    @JsonProperty("FCompanyID")
    @JsonAlias({"id", "companyId"})
    private Long id;

    @JsonProperty("FCompanyCode")
    @JsonAlias("companyCode")
    private String companyCode;

    @JsonProperty("FCompanyName")
    @JsonAlias("companyName")
    private String companyName;

    @JsonProperty("FEnabled")
    @JsonAlias("enabled")
    private Boolean enabled;

    @JsonProperty("FRemark")
    @JsonAlias("remark")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @JsonProperty("FCreateTime")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonIgnore
    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
