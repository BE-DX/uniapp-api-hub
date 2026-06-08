package com.galaxy.apihub.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.galaxy.apihub.module.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
