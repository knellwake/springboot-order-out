package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
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
import org.springframework.web.bind.annotation.*;

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

    /**
     * 接单
     * @return
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result updateConfirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        orderService.updateConfirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 拒单
     * @return
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result updateRejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO){
        orderService.updateRejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 取消订单
     * @return
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result updateCancel(@RequestBody OrdersCancelDTO ordersCancelDTO){
        orderService.updateCancel(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 派送订单
     * @return
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result updateDelivery(@PathVariable Long id){
        orderService.updateDelivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     * @return
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result updateComplete(@PathVariable Long id){
        orderService.updateComplete(id);
        return Result.success();
    }
}