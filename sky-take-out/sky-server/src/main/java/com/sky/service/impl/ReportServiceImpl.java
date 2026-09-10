package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ReportMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

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

    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> goodsSalesDTOS = orderMapper.top10(beginTime, endTime);
        List<String> nameList = goodsSalesDTOS.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = goodsSalesDTOS.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        return new SalesTop10ReportVO(StringUtils.join(nameList, ","), StringUtils.join(numberList, ","));
    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0);
            XSSFRow row = sheet.getRow(1);
            row.getCell(1).setCellValue(begin + "至" + end);
            row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                // 日期	"	营业额"	"	有效订单"	订单完成率	平均客单价	新增用户数
                row = sheet.getRow(i + 6);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }
            ServletOutputStream outputStream = response.getOutputStream(); // 获取输出流,通过输出流将文件下载到客户端浏览器中
            workbook.write(outputStream); // 将工作簿写入输出流
            outputStream.flush(); // 刷新输出流
            outputStream.close(); // 关闭输出流
            workbook.close(); // 关闭工作簿
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
