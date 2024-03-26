package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐
     *
     * @param setmealDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        setmealMapper.insert(setmeal);
        Long setmealId = setmeal.getId();

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0) {
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
        }
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> setmealVOPage = setmealMapper.pagequery(setmealPageQueryDTO);
        return new PageResult(setmealVOPage.getTotal(), setmealVOPage.getResult());
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        setmealMapper.update(setmeal);
        Long setmealId = setmeal.getId();

        // 根据SetmealId查询与之相关的第三方表全部Id
        List<SetmealDish> setmealDishList = setmealDishMapper.getBySetmealId(setmealId);
        if (setmealDishList != null && setmealDishList.size() > 0) {
            setmealDishList.forEach(setmealDish -> {
                // 根据获取的ID 批量删除setmealDish已存在的相关数据
                setmealDishMapper.delete(setmealDish.getId());

            });
        }
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        // 重新插入修改后的数据
        setmealDishMapper.insertBatch(setmealDTO.getSetmealDishes());

    }

    /**
     * 根据ID查询套餐数据
     *
     * @param id
     * @return
     */
    @Override
    public SetmealDTO getById(Long id) {
        SetmealDTO setmealDTO = setmealMapper.getById(id);
        List<SetmealDish> setmealDishList = setmealDishMapper.getBySetmealId(id);
        setmealDTO.setSetmealDishes(setmealDishList);
        return setmealDTO;
    }
}