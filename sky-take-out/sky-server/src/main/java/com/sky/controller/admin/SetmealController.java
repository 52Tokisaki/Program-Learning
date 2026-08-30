package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.Result;
import com.sky.service.SetmealService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Api(tags = "套餐管理")
public class SetmealController {

    @Autowired
    private SetmealService service;

    @Autowired
    private SetmealService setmealService;

    @PostMapping
    @ApiOperation("新增套餐")
    @CachePut(cacheNames = "setmealCache", key = "#setmealDTO.categoryId") // 向缓存中插入分类id为categoryId的套餐数据
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐, {}", setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("套餐分页查询, {}", setmealPageQueryDTO);
        return Result.success(setmealService.page(setmealPageQueryDTO));
    }

    @DeleteMapping
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result deleteByIds(@RequestParam List<Long> ids) {
        log.info("删除套餐：{}", ids);
        setmealService.deleteByIds(ids);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        log.info("根据id查询套餐：{}", id);
        return Result.success(setmealService.getById(id));
    }

    @PutMapping
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐, {}", setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("套餐起售停售")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result enableOrDisable(@PathVariable Integer status, Long id) {
        log.info("套餐起售停售, {}, {}", id, status);
        setmealService.enableOrDisable(id, status);
        return Result.success();
    }
}
