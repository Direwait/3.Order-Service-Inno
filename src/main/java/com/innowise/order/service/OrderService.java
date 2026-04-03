package com.innowise.order.service;

import com.innowise.order.service.dto.OrderDto;
import com.innowise.order.service.dto.OrderWithUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface OrderService {
    OrderWithUserResponse createOrder(OrderDto orderDto);

    OrderWithUserResponse getOrderByOrderId(UUID id);

    OrderWithUserResponse updateByOrderId(UUID orderId, OrderDto orderDto);

    Page<OrderWithUserResponse> getOrdersWithDateRangeAndStatuses(
            LocalDate startDate,
            LocalDate endDate,
            String status,
            Pageable pageable
    );

    Page<OrderWithUserResponse> getOrdersByUserId(UUID userId, Pageable pageable);

    void deleteByOrderId(UUID orderId);

    boolean sofDeleteByOrderId(UUID orderId);
}
