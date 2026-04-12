package com.innowise.order.controller;

import com.innowise.order.service.dto.OrderDto;
import com.innowise.order.service.dto.OrderWithUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OrderController {
    /**
     * Creates a new order.
     * The total price is automatically calculated from order items.
     *
     * @param orderDto the order data (userId and items)
     * @return created order with user details, HTTP 201 status
     */
    ResponseEntity<OrderWithUserResponse> createOrder(OrderDto orderDto);

    /**
     * Retrieves an order by its ID.
     *
     * @param orderId the order UUID
     * @return the order with user details, HTTP 200 status
     */
    ResponseEntity<OrderWithUserResponse> getById(UUID orderId);

    /**
     * Retrieves orders with optional filtering by date range and status.
     *
     * @param startDate filter orders created after this date (optional)
     * @param endDate   filter orders created before this date (optional)
     * @param statuses    filter orders by list of statuses (CREATED, CANCELED, etc.) (optional)
     * @param pageable  pagination parameters (page, size, sort)
     * @return a page of orders with user details, HTTP 200 status
     */
    ResponseEntity<Page<OrderWithUserResponse>> getOrdersWithDateRangeAndStatuses(
            LocalDate startDate,
            LocalDate endDate,
            List<String> statuses,
            Pageable pageable
    );

    /**
     * Retrieves all orders for a specific user.
     *
     * @param userId   the user UUID
     * @param pageable pagination parameters
     * @return a page of orders for the user with user details, HTTP 200 status
     */
    ResponseEntity<Page<OrderWithUserResponse>> getOrderByUserId(UUID userId, Pageable pageable);

    /**
     * Performs soft delete (or restore) of an order.
     * Toggles the deleted flag of the order.
     *
     * @param orderId the order UUID
     * @return true if order was deleted, false if restored, HTTP 200 status
     */
    ResponseEntity<Boolean> softDeleteByOrderId(UUID orderId);

    /**
     * Performs hard delete of an order.
     * Removes the order completely from the database.
     *
     * @param orderId the order UUID
     * @return HTTP 204 status (No Content)
     */
    ResponseEntity<Void> deleteByOrderId(UUID orderId);

    /**
     * Updates an existing order by its ID.
     * Replaces the order items and recalculates total price.
     *
     * @param orderId  the order UUID
     * @param orderDto the updated order data
     * @return the updated order with user details, HTTP 200 status
     */
    ResponseEntity<OrderWithUserResponse> updateByOrderId(UUID orderId, OrderDto orderDto);
}
