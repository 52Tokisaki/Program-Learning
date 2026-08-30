package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    @Transactional // 开启事务
    public void save(DishDTO dishDTO) {
        Long categoryId = dishDTO.getCategoryId();
        String key = "dish_" + categoryId;
        // 清除Redis中对应菜品分类的缓存数据
        cleanCache(key);
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);
        Long id = dish.getId();
        List<DishFlavor> flavors = dishDTO.getFlavors();
        flavors.forEach(flavor -> flavor.setDishId(id));
        dishFlavorMapper.insertBatch(flavors);
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.query(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        // 若存在已启用的菜品，则抛出异常
        List<Dish> dishList = dishMapper.getByIds(ids);
        dishList.forEach(dish -> {
            if (dish.getStatus().equals(StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        });
        // 若存在套餐跟菜品关联，则抛出异常
        List<SetmealDish> setmealDishList = setmealDishMapper.getByDishIds(ids);
        if (setmealDishList != null && !setmealDishList.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        // 清除Redis中对应菜品分类的缓存数据
        cleanCache("dish_*");

        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByDishIds(ids);
    }

    @Override
    public DishVO getById(Long id) {
        Dish dish = dishMapper.getById(id);

        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    @Override
    @Transactional
    public void update(DishDTO dishDTO) {
        // 先删除原有 flavors
        dishFlavorMapper.deleteByDishIds(Collections.singletonList(dishDTO.getId()));
        // 再插入新的 flavors
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            dishFlavorMapper.insertBatch(flavors.stream().peek(flavor -> flavor.setDishId(dishDTO.getId())).collect(Collectors.toList()));
        }
        cleanCache("dish_*");
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);
    }

    @Override
    public List<DishVO> getByCategoryId(Long categoryId) {
        List<DishVO> dishList = dishMapper.getByCategoryId(categoryId);
        return dishList;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        cleanCache("dish_*");
        dishMapper.updateStatusById(id, status);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    // 根据pattern清除缓存
    private void cleanCache(String pattern) {
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
