package com.innowise.order.controller.impl;

import com.innowise.order.controller.OrderController;
import com.innowise.order.service.OrderService;
import com.innowise.order.service.dto.OrderDto;
import com.innowise.order.service.dto.OrderWithUserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderControllerImpl implements OrderController {

    private final OrderService orderService;

    @PostMapping()
    @Override
    public ResponseEntity<OrderWithUserResponse> createOrder(@Valid @RequestBody OrderDto orderDto) {
        var orderWithUser = orderService.createOrder(orderDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderWithUser);
    }

    @GetMapping("/{orderId}")
    @Override
    public ResponseEntity<OrderWithUserResponse> getById(@PathVariable UUID orderId) {
        var byId = orderService.getOrderByOrderId(orderId);
        return ResponseEntity.ok(byId);
    }

    @GetMapping
    @Override
    public ResponseEntity<Page<OrderWithUserResponse>> getOrdersWithDateRangeAndStatuses(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) List<String> statuses,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        var ordersWithDateRangeAndStatuses = orderService.getOrdersWithDateRangeAndStatuses(
                startDate, endDate,
                statuses, pageable
        );
        return ResponseEntity.ok(ordersWithDateRangeAndStatuses);
    }

    @GetMapping("/users/{userId}")
    @Override
    public ResponseEntity<Page<OrderWithUserResponse>> getOrderByUserId(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        var ordersByUserId = orderService.getOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(ordersByUserId);
    }

    @DeleteMapping("/{orderId}")
    @Override
    public ResponseEntity<Void> deleteByOrderId(@PathVariable UUID orderId) {
        orderService.deleteByOrderId(orderId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{orderId}")
    @Override
    public ResponseEntity<OrderWithUserResponse> updateByOrderId(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderDto orderDto
    ) {
        var orderWithUserResponse = orderService.updateByOrderId(orderId, orderDto);
        return ResponseEntity.ok(orderWithUserResponse);
    }
}
