package com.uniapp.apihub.module.permission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_business_document")
public class BusinessDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long systemId;

    private String moduleCode;

    private String moduleName;

    private String documentCode;

    private String documentName;

    private Boolean enabled;

    private Integer sortNo;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
