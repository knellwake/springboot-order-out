package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品DishIds 查询对应有包含的套餐的SetmealIds
     * @param ids
     * @return
     */
    //select setmeal_id from setmeal_dish where dish_id in (1,2,3,4)
    List<Long> getSetmealIdsByDishIds(List<Long> ids);

    /**
     * 插入数据
     * @param setmealDish
     */
    void insert(SetmealDish setmealDish);
}
