package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ReportMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
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
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dataList = new ArrayList<>();
        List<Double> turnoverList = new ArrayList<>();

        while(!begin.equals(end)) {
            dataList.add(begin);
            LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(begin, LocalTime.MAX);
            // select sum(amount) from orders where order_time between beginTime and endTime and status = 5
            Double turnover = orderMapper.turnoverStatistics(beginTime, endTime, Orders.COMPLETED);
            turnoverList.add(turnover != null ? turnover : 0);
            begin = begin.plusDays(1);
        }
        return new TurnoverReportVO(StringUtils.join(dataList, ","), StringUtils.join(turnoverList, ","));
    }

    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dataList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        while (!begin.equals(end)) {
            dataList.add(begin);
            begin = begin.plusDays(1);
            LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(begin, LocalTime.MAX);
            // select sum(*) from user where create_time between beginTime and endTime
            Integer totalUser = userMapper.countUser(null, null);
            totalUserList.add(totalUser != null ? totalUser : 0);
            Integer newUser = userMapper.countUser(beginTime, endTime);
            newUserList.add(newUser != null ? newUser : 0);
        }
        return new UserReportVO(StringUtils.join(dataList, ","), StringUtils.join(totalUserList, ","), StringUtils.join(newUserList, ","));
    }

    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dataList = new ArrayList<>();
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        while (!begin.equals(end)) {
            dataList.add(begin);
            begin = begin.plusDays(1);
            LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(begin, LocalTime.MAX);
            // select count(*) from orders where order_time between beginTime and endTime
            Integer orderCount = orderMapper.countOrdersByStatus(beginTime, endTime, null);
            orderCountList.add(orderCount != null ? orderCount : 0);
            // select count(*) from orders where order_time between beginTime and endTime and status = 5
            Integer validOrderCount = orderMapper.countOrdersByStatus(beginTime, endTime, Orders.COMPLETED);
            validOrderCountList.add(validOrderCount != null ? validOrderCount : 0);
        }
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer totalValidOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        Double orderCompletionRate = totalOrderCount != 0 ? (double) totalValidOrderCount / totalOrderCount : 0;
        return new OrderReportVO(
                StringUtils.join(dataList, ","),
                StringUtils.join(orderCountList, ","),
                StringUtils.join(validOrderCountList, ","),
                totalOrderCount,
                totalValidOrderCount,
                orderCompletionRate);

    }
}
