package com.sky.service;

import com.sky.vo.TurnoverReportVO;

import java.time.LocalDate;

public interface ReportService {

    /**
     * 统计日期内 每天的营业额数据
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO getTurnoverReportVOResult(LocalDate begin, LocalDate end);
}
