package com.sky.controller.admin;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "管理端-订单管理")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单搜索 ：按条件查询订单 分页显示
     *
     * @return
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("查询订单分页显示")
    public Result<PageResult> queryPage(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageResult pageResult = orderService.queryPage(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 查看订单详情
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("查看订单详情")
    public Result<OrderVO> queryOrderDetails(@PathVariable Long id){
        OrderVO orderVO =  orderService.queryOrderDetails(id);
        return Result.success(orderVO);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @GetMapping("/statistics")
    @ApiOperation("各个状态的订单数量统计")
    public Result<OrderStatisticsVO> countOrderStatus(){
        OrderStatisticsVO orderStatisticsVO = orderService.countOrderStatus();
        return Result.success(orderStatisticsVO);
    }
}