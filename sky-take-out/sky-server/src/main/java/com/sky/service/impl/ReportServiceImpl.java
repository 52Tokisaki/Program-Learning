package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.ReportMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private ReportMapper reportMapper;

    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dataList = new ArrayList<>();
        List<Double> turnoverList = new ArrayList<>();

        while(!begin.equals(end)) {
            dataList.add(begin);
            LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(begin, LocalTime.MAX);
            // select sum(amount) from orders where order_time between beginTime and endTime and status = 5
            Double turnover = reportMapper.turnoverStatistics(beginTime, endTime, Orders.COMPLETED);
            turnoverList.add(turnover != null ? turnover : 0);
            begin = begin.plusDays(1);
        }
        return new TurnoverReportVO(StringUtils.join(dataList, ","), StringUtils.join(turnoverList, ","));
    }
}
