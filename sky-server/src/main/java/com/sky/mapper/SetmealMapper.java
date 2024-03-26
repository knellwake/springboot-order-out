package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SetmealMapper {
    /**
     * 根据分类ID统计套餐数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id=#{categoryId}")
    Integer countByCategoryId(Long id);

    /**
     * 插入套餐数据
     * @param setmeal
     */
    @AutoFill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> pagequery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 修改套餐
     * @param setmeal
     */
    @AutoFill(OperationType.UPDATE)
    void update(Setmeal setmeal);

    /**
     * 根据ID查询套餐数据
     * @param id
     * @return
     */
    @Select("select * from setmeal where id=#{id}")
    SetmealDTO getById(Long id);
}
