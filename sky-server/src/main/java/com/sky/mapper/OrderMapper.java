package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /**
     * 分页查询订单显示
     *
     * @param ordersPageQueryDTO
     * @return
     */
    Page<OrderVO> selectOrdersByCondition(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    OrderStatisticsVO countStatus();

    /**
     * 定时处理
     *
     * @param pendingPayment
     * @param plusMinutes
     * @return
     */
    @Select("select * from orders where status=#{status} and order_time < #{plusMinutes}")
    List<Orders> getByStatusAndOrderTime(@Param("status") Integer pendingPayment, LocalDateTime plusMinutes);

    /**
     * @param outTradeNo
     * @param userId
     * @return
     */
    @Select("select * from orders where number=#{outTradeNo} and user_id =#{userId};")
    Orders getByNumberAndUserId(String outTradeNo, Long userId);

    /**
     * 统计每日营业额
     * @param map
     * @return
     */
    Double sumAmountByMap(Map map);

    /**
     * 统计订单数
     * @param map
     * @return
     */
    Integer countOrderByMap(Map map);

    /**
     * 商品列表 商品名称，商品数量
     * @param beginTime
     * @param endTime
     * @return
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime beginTime, LocalDateTime endTime);
}
