package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 日期内 每天营业额数据统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverReportVOResult(LocalDate begin, LocalDate end) {
        // 当前集合 用于 存放从begin到end范围内每天的日期
        List<LocalDate> dates = new ArrayList<>();

        dates.add(begin);
        while (!begin.equals(end)) {
            // 日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dates.add(begin);
        }

        // 根据每天的日期，计算当天的营业额: 状态为“已完成”的订单金额总和
        // select sum(amount) from orders where order_time between ? and ? and status = 5;
        List<Double> turnovers = new ArrayList<>();
        for (LocalDate date : dates) {
            // 一天中的0点 到 23:59:59.999999999 MIN --> MAX
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.CANCELLED);

            Double turnover = orderMapper.sumAmountByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnovers.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dates, ","))
                .turnoverList(StringUtils.join(turnovers, ","))
                .build();
    }

    /**
     * 统计每日用户总量与新增用户
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserReportVO(LocalDate begin, LocalDate end) {
        // 日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        // 用户总量 直到当天结束之前的所有用户
        // select sum(id) from users where create_time < endTime
        List<Integer> userAllList = new ArrayList<>();

        // 新增用户
        List<Integer> userNewAddList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);

            // 求用户总量
            Integer userAllNum = userMapper.sumUserByMap(map);
            userAllList.add(userAllNum);

            //求当天新增用户量
            map.put("end", endTime);
            Integer userNum = userMapper.sumUserByMap(map);
            userNum = userNum == null ? 0 : userNum;
            userNewAddList.add(userNum);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(userAllList, ","))
                .newUserList(StringUtils.join(userNewAddList, ","))
                .build();
    }

    /**
     * 订单数据统计,总订单，已完成订单数，订单完成率，当日订单数，当日已完成订单数
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderReportVO(LocalDate begin, LocalDate end) {
        // 日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        // 每日订单数
        List<Integer> orderList = new ArrayList<>();

        // 每日有效订单数
        List<Integer> orderDoneList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Integer orderNum = getOrderCount(beginTime, endTime, null);
            orderList.add(orderNum);

            Integer orderDoneNum = getOrderCount(beginTime, endTime, Orders.COMPLETED);
            orderDoneList.add(orderDoneNum);
        }
        // 时间区间内订单总数
        Integer totalOrder = orderList.stream().reduce(Integer::sum).get();

        // 时间区间内有效订单数
        Integer validOrder = orderDoneList.stream().reduce(Integer::sum).get();

        // 订单完成率
        Double orderRate = 0.0;
        if (totalOrder != 0) {
            orderRate = validOrder.doubleValue() / totalOrder;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderList, ","))
                .validOrderCountList(StringUtils.join(orderDoneList, ","))
                .totalOrderCount(totalOrder)
                .validOrderCount(validOrder)
                .orderCompletionRate(orderRate)
                .build();
    }

    /**
     * 销售前十的商品（菜品/套餐）数据统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10ReportVO(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime, endTime);
        // 商品名称列表
        List<String> nameList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String names = StringUtils.join(nameList, ",");
        // 销量列表
        List<Integer> numberList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numbers = StringUtils.join(numberList, ",");

        return SalesTop10ReportVO.builder()
                .nameList(names)
                .numberList(numbers)
                .build();
    }

    /**
     * 根据时间区间统计指定状态的订单数量
     *
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map map = new HashMap();
        map.put("status", status);
        map.put("begin", beginTime);
        map.put("end", endTime);
        return orderMapper.countOrderByMap(map);
    }
}