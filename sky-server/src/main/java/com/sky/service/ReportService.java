package com.sky.service;

import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

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
}
