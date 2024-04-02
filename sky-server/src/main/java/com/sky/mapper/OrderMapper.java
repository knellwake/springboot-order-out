package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper {
    /**
     * 插入一条订单表数据
     *
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     *
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     *
     * @param orders
     */
    void update(Orders orders);


    /**
     * 查询全部订单信息
     *
     * @param ordersPageQueryDTO
     * @return
     */
    Page<OrderVO> selectOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 查询订单
     *
     * @param id
     * @return
     */
    @Select("select * from orders where id=#{id}")
    OrderVO selectOrdersById(Long id);
}