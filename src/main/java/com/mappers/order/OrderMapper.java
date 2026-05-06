package com.mappers.order;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    List<Map<String, Object>> getOrderList();

    int insertOrder(Map<String, Object> param);

    int updateOrder(Map<String, Object> param);

    int deleteOrder(Map<String, Object> param);

    int updateOrderStatus(Map<String, Object> param);
}