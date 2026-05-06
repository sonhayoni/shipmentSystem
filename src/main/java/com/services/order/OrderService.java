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

/*    public List<Map<String, Object>> getOrderList() {
        return orderMapper.getOrderList();
    }

    public int insertOrder(Map<String, Object> param) {
        return orderMapper.insertOrder(param);
    }*/
}