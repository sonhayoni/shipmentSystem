package com.controllers.order;

import com.services.order.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/v1/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 화면 이동
    @GetMapping("/page")
    public String orderPage() {
        return "order/list";
    }
/*
    // 주문 목록 조회 API
    @ResponseBody
    @GetMapping
    public List<Map<String, Object>> getOrderList() {
        return orderService.getOrderList();
    }

    // 주문 등록 API
    @ResponseBody
    @PostMapping
    public int insertOrder(@RequestBody Map<String, Object> param) {
        return orderService.insertOrder(param);
    }*/
}