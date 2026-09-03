package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /*
     * 定时任务，处理超时订单
    * */
    @Scheduled(cron = "0 * * * * ?") // 每分钟执行一次
    public void processOrderTimeout() {
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
        if (ordersList != null && ordersList.size() > 0) {
            log.info("超时订单列表：{}", ordersList);
            for (Orders orders : ordersList) {
                log.info("取消订单：{}", orders);
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelTime(LocalDateTime.now());
                orders.setCancelReason("订单超时取消");
                orderMapper.update(orders);
            }
        }
    }

    /*
    * 定时任务，处理派送完成订单，每天1点执行
    * */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryInProgressOrder() {
        log.info("处理派送完成订单...");
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));

        if (ordersList != null && ordersList.size() > 0) {
            log.info("派送完成订单列表：{}", ordersList);
            for (Orders orders : ordersList) {
                log.info("完成订单：{}", orders);
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }
}
