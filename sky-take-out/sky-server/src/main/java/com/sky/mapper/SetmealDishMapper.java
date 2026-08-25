package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    List<SetmealDish> getByDishIds(List<Long> dishIds);

    void insertBatch(List<SetmealDish> setmealDishes);

    void deleteBatchBySetmealIds(List<Long> setmealIds);

    @Select("select * from setmeal_dish where setmeal_id = #{id}")
    List<SetmealDish> getBySetmealId(Long id);
}