package com.clip.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clip.platform.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
