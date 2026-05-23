package com.clip.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clip.platform.entity.MediaTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MediaTaskMapper extends BaseMapper<MediaTask> {

    @Select("SELECT COUNT(*) FROM media_tasks WHERE status = #{status}")
    long countByStatus(String status);
}
