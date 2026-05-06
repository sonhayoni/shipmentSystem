package com.mappers.order;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    // 주문 목록 조회
    List<Map<String, Object>> getOrderList();

    // 주문 등록
    int insertOrder(Map<String, Object> param);
}