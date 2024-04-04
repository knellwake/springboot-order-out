package com.sky.service;

import com.sky.result.Result;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {

    /**
     * 统计日期内 每天的营业额数据
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO getTurnoverReportVOResult(LocalDate begin, LocalDate end);

    /**
     * 统计每日用户总量与新增用户
     * @param begin
     * @param end
     * @return
     */
    UserReportVO getUserReportVO(LocalDate begin, LocalDate end);

    /**
     * 订单数据统计,总订单，已完成订单数，订单完成率，当日订单数，当日已完成订单数
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO getOrderReportVO(LocalDate begin, LocalDate end);

    /**
     * 销售前十的商品（菜品/套餐）数据统计
     * @param begin
     * @param end
     * @return
     */
    SalesTop10ReportVO getSalesTop10ReportVO(LocalDate begin, LocalDate end);

    /**
     * 导出近30天的运营数据报表
     * @param response
     */
    void exportBusinessData(HttpServletResponse response);
}
