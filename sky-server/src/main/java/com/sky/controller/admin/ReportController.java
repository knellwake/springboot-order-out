package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

@RestController
@Slf4j
@Api(tags = "数据统计报表相关接口")
@RequestMapping("/admin/report/")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 营业额数据统计
     *
     * @param begin
     * @param end
     * @return
     */
    @ApiOperation("营业额数据统计")
    @GetMapping("/turnoverStatistics")
    public Result<TurnoverReportVO> turnoverReportVOResult(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                           @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("营业额数据统计，开始日期：{},结束日期：{}", begin, end);
        return Result.success(reportService.getTurnoverReportVOResult(begin, end));
    }

    /**
     * 用户数据统计
     *
     * @param begin
     * @param end
     * @return
     */
    @ApiOperation("用户数据统计")
    @GetMapping("/userStatistics")
    public Result<UserReportVO> userReportVO(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                             @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("用户数据统计，开始日期：{},结束日期：{}", begin, end);
        return Result.success(reportService.getUserReportVO(begin, end));
    }

    /**
     * 订单数据统计
     *
     * @param begin
     * @param end
     * @return
     */
    @ApiOperation("订单数据统计")
    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> orderReportVOResult(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                     @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("订单数据统计，开始日期：{},结束日期：{}", begin, end);
        return Result.success(reportService.getOrderReportVO(begin, end));
    }

    /**
     * 销售前十的商品（菜品/套餐）数据统计
     *
     * @param begin
     * @param end
     * @return
     */
    @ApiOperation("销售前十的商品数据统计")
    @GetMapping("/top10")
    public Result<SalesTop10ReportVO> salesTop10ReportVOResult(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                               @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("销售前十的商品数据统计，开始日期：{},结束日期：{}", begin, end);
        return Result.success(reportService.getSalesTop10ReportVO(begin, end));
    }

    /**
     * 文件导出Excel
     */
    @ApiOperation("文件导出Excel")
    @GetMapping("/export")
    public void exportExcel(HttpServletResponse response){
        reportService.exportBusinessData(response);
    }
}