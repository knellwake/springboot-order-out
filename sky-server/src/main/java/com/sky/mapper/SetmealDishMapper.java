package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品DishIds 查询对应有包含的套餐的SetmealIds
     *
     * @param ids
     * @return
     */
    //select setmeal_id from setmeal_dish where dish_id in (1,2,3,4)
    List<Long> getSetmealIdsByDishIds(List<Long> ids);

    /**
     * 批量插入N条数据
     *
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐ID查询第三方表的数据
     *
     * @param setmealId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id=#{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);

    /**
     * 删除
     * @param id
     */
    @Delete("delete from setmeal_dish where id = #{id}")
    void delete(Long id);
}
