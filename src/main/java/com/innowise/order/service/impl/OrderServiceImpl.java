package com.innowise.order.service.impl;

import com.innowise.order.dao.enums.Status;
import com.innowise.order.dao.model.OrderModel;
import com.innowise.order.dao.repository.OrderRepository;
import com.innowise.order.dao.repository.specification.OrderSpecification;
import com.innowise.order.service.OrderService;
import com.innowise.order.service.dto.OrderDto;
import com.innowise.order.service.dto.OrderWithUserResponse;
import com.innowise.order.service.dto.feignClient.UserDto;
import com.innowise.order.service.impl.feignWraper.UserServiceFeignWrapper;
import com.innowise.order.service.impl.totalPriceCalculator.OrderTotalPriceCalculator;
import com.innowise.order.service.mapper.OrderMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderTotalPriceCalculator orderTotalPriceCalculator;
    private final UserServiceFeignWrapper userServiceFeignWrapper;

    @Transactional
    @Override
    public OrderWithUserResponse createOrder(OrderDto orderDto) {
        OrderModel orderModel = orderMapper.dtoToModel(orderDto);
        orderModel.setStatus(Status.CREATED);

        prepareOrderForSave(orderModel);
        OrderModel savedOrder = orderRepository.save(orderModel);
        UserDto userDto = userServiceFeignWrapper.getUserWithCircuitBreaker(savedOrder.getUserId());

        return OrderWithUserResponse.builder()
                .orderDto(orderMapper.modelToDto(savedOrder))
                .userDto(userDto)
                .build();
    }

    @Override
    public OrderWithUserResponse getOrderByOrderId(UUID orderId) {
        OrderModel orderModel = findOrderById(orderId);
        UserDto userDto = userServiceFeignWrapper.getUserWithCircuitBreaker(orderModel.getUserId());

        return OrderWithUserResponse.builder()
                .orderDto(orderMapper.modelToDto(orderModel))
                .userDto(userDto)
                .build();
    }

    @Override
    @Transactional
    public OrderWithUserResponse updateByOrderId(UUID orderId, OrderDto orderDto) {
        OrderModel orderModel = findOrderById(orderId);
        orderMapper.updateFromDto(orderDto, orderModel);
        prepareOrderForSave(orderModel);

        OrderModel updatedOrder = orderRepository.save(orderModel);
        UserDto userDto = userServiceFeignWrapper.getUserWithCircuitBreaker(updatedOrder.getUserId());

        return OrderWithUserResponse.builder()
                .orderDto(orderMapper.modelToDto(updatedOrder))
                .userDto(userDto)
                .build();
    }

    @Override
    public Page<OrderWithUserResponse> getOrdersWithDateRangeAndStatuses(
            LocalDate startDate,
            LocalDate endDate,
            String status,
            Pageable pageable
    ) {
        Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Status.valueOf(status.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status value: {}, ignoring filter", status);
            }
        }
        Specification<OrderModel> spec = OrderSpecification.filterByDateRangeAndStatus(
                startDate, endDate, statusEnum
        );
        return orderRepository.findAll(spec, pageable)
                .map(order -> {
                    UserDto userDto = userServiceFeignWrapper.getUserWithCircuitBreaker(order.getUserId());
                    return OrderWithUserResponse.builder()
                            .orderDto(orderMapper.modelToDto(order))
                            .userDto(userDto)
                            .build();
                });
    }

    @Override
    public Page<OrderWithUserResponse> getOrdersByUserId(UUID userId, Pageable pageable) {
        UserDto userDto = userServiceFeignWrapper.getUserWithCircuitBreaker(userId);

        var orderPage = orderRepository.findAllByUserId(userId, pageable);

        return orderPage.map(order -> OrderWithUserResponse.builder()
                .orderDto(orderMapper.modelToDto(order))
                .userDto(userDto)
                .build());
    }

    @Override
    @Transactional
    public void deleteByOrderId(UUID orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new EntityNotFoundException("Order not found with orderId " + orderId);
        }
        orderRepository.deleteById(orderId);
    }

    @Override
    @Transactional
    public boolean sofDeleteByOrderId(UUID orderId) {
        OrderModel order = findOrderById(orderId);

        if (order.isDeleted()) {
            order.setDeleted(false);
            log.info("Order with id {} was restored", orderId);
        } else {
            order.setDeleted(true);
            log.info("Order with id {} was marked as deleted", orderId);
        }
        return order.isDeleted();
    }

    private void prepareOrderForSave(OrderModel orderModel) {
        orderMapper.setOrderItemsRelation(orderModel);
        BigDecimal totalPrice = orderTotalPriceCalculator.calculateTotalPrice(orderModel.getItems());
        orderModel.setTotalPrice(totalPrice);
    }

    private OrderModel findOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Order not found with id: %s", orderId)
                ));
    }
}