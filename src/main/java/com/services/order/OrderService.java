package com.services.order;

import com.mappers.order.OrderMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public List<Map<String, Object>> getOrderList() {
        return orderMapper.getOrderList();
    }

    public int insertOrder(Map<String, Object> param) {
        return orderMapper.insertOrder(param);
    }

    public int updateOrder(Map<String, Object> param) {
        return orderMapper.updateOrder(param);
    }

    public int updateOrderStatus(Map<String, Object> param) {
        return orderMapper.updateOrderStatus(param);
    }

    public int deleteOrder(Map<String, Object> param) {
        return orderMapper.deleteOrder(param);
    }
}