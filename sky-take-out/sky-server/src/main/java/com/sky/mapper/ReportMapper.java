package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface ReportMapper {

    @Select("select sum(amount) from orders where order_time between #{beginTime} and #{endTime} and status = #{status}")
    Double turnoverStatistics(LocalDateTime beginTime, LocalDateTime endTime, Integer status);
}
