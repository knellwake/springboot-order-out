package com.sky.service;

import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.*;
import org.springframework.stereotype.Service;

import java.util.List;


public interface OrderService {
    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 查询历史订单
     *
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult queryHistoryOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    OrderVO showDetails(Long id);

    /**
     * 取消订单
     *
     * @param id
     */
    void cancelOrder(Long id);

    /**
     * 再来一单
     *
     * @param id
     */
    void repetitionOrder(Long id);

    /**
     * 分页查询显示
     *
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult queryPage(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 查看订单详情
     * @param id
     * @return
     */
    OrderVO queryOrderDetails(Long id);

    /**
     * 各个状态的订单数量统计
     * @return
     */
    OrderStatisticsVO countOrderStatus();

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    void updateConfirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 接单
     * @param ordersRejectionDTO
     */
    void updateRejection(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    void updateCancel(OrdersCancelDTO ordersCancelDTO);

    /**
     * 派送订单
     * @param id
     */
    void updateDelivery(Long id);

    /**
     * 完成订单
     * @param id
     */
    void updateComplete(Long id);

    /**
     * 催单提醒
     * @param id
     */
    void reminder(Long id);
}
