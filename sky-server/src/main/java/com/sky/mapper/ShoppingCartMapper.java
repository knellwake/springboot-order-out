package com.sky.mapper;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    /**
     * 根据传递信息 查询显示获取购物车信息
     * 通用查询 根据XX查询购物车数据
     *
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 根据用户Id更新购物车物品数量
     *
     * @param
     */
    @Update("update shopping_cart set number=#{number} where id=#{id}")
    void updateByUserId(ShoppingCart shoppingCart);

    /**
     * 插入一条购物车信息
     *
     * @param shoppingCart
     */
    @Insert("insert into shopping_cart (name, user_id, dish_id, setmeal_id, dish_flavor, number, amount, image, create_time) " +
            " values (#{name},#{userId},#{dishId},#{setmealId},#{dishFlavor},#{number},#{amount},#{image},#{createTime})")
    void insert(ShoppingCart shoppingCart);

    /**
     * 清空购物车
     *
     * @param userId
     */
    @Delete("delete from shopping_cart where user_id=#{userId}")
    void delectAllByUserId(Long userId);

    /**
     * 删除一条购物车数据
     * @param cart
     */
    void deletOne(ShoppingCart cart);
}
