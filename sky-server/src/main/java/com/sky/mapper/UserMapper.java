package com.sky.mapper;

import com.sky.entity.*;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    /**
     * 根据openid获取用户表对象
     *
     * @param openid
     * @return
     */
    @Select("select * from user where openid=#{openid}")
    User getByOpenid(String openid);

    /**
     * 插入新用户
     * @param user
     */
    void insert(User user);

    /**
     * 用户Id
     * @param userId
     * @return
     */
    @Select("select * from user where id=#{userId}")
    User getById(Long userId);
}
