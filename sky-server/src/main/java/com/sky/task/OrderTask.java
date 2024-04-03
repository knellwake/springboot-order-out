package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理未支付的订单(下单时间过15分钟后还未付款的) --> 成为 已取消订单
     */
    @Scheduled(cron = "0 0/2 * * * ?") // 每两分钟执行一次
    public void processCancelOrder() {
        log.info("处理超时订单：{}", LocalDateTime.now());
        // select * from orders where status=? and order_time < (localTime.now - 15)
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));

        if (ordersList.size() > 0 && ordersList != null) {
            for (Orders orders : ordersList) {
                orders.setCancelTime(LocalDateTime.now());
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");

                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理还在派送中的订单 --> 为 已完成订单
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行一次
    public void processDeliveryOrder() {
        log.info("定时处理还在派送中的订单，{}", LocalDateTime.now());
        // select * from orders where status=? and order_time < (localTime.now - 15)
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));

        if (ordersList.size() > 0 && ordersList != null) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);

                orderMapper.update(orders);
            }
        }
    }
}