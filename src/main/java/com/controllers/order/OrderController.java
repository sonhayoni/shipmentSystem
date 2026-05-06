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

    // 주문 목록 조회
    @ResponseBody
    @PostMapping("/list")
    public List<Map<String, Object>> getOrderList() {
        return orderService.getOrderList();
    }

    // 주문 등록
    @ResponseBody
    @PostMapping("/insert")
    public int insertOrder(@RequestBody Map<String, Object> param) {
        return orderService.insertOrder(param);
    }

    // 주문 수정
    @ResponseBody
    @PostMapping("/update")
    public int updateOrder(@RequestBody Map<String, Object> param) {
        return orderService.updateOrder(param);
    }

    // 주문 삭제
    @ResponseBody
    @PostMapping("/delete")
    public int deleteOrder(@RequestBody Map<String, Object> param) {
        return orderService.deleteOrder(param);
    }

    // 주문 상태 변경
    @ResponseBody
    @PostMapping("/status")
    public int updateOrderStatus(@RequestBody Map<String, Object> param) {
        return orderService.updateOrderStatus(param);
    }
}