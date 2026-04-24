package com.innowise.order.service.impl;

import com.innowise.order.dao.enums.Status;
import com.innowise.order.dao.model.OrderModel;
import com.innowise.order.dao.repository.OrderRepository;
import com.innowise.order.dao.repository.specification.OrderSpecification;
import com.innowise.order.service.OrderService;
import com.innowise.order.service.dto.OrderDto;
import com.innowise.order.service.dto.OrderWithUserResponse;
import com.innowise.order.service.dto.action.ActionPaymentInfo;
import com.innowise.order.service.dto.feignClient.UserDto;
import com.innowise.order.service.impl.feignWraper.UserServiceFeignWrapper;
import com.innowise.order.service.impl.paymentCheckalidator.PaymentCheckValidator;
import com.innowise.order.service.impl.totalPriceCalculator.OrderTotalPriceCalculator;
import com.innowise.order.service.mapper.OrderMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderTotalPriceCalculator orderTotalPriceCalculator;
    private final UserServiceFeignWrapper userServiceFeignWrapper;

    private final PaymentCheckValidator paymentCheckValidator;

    @Transactional
    @Override
    public OrderWithUserResponse createOrder(OrderDto orderDto) {
        OrderModel orderModel = orderMapper.dtoToModel(orderDto);
        orderModel.setStatus(Status.CREATED);

        prepareOrderForSave(orderModel);
        OrderModel savedOrder = orderRepository.save(orderModel);
        UserDto userDto = userServiceFeignWrapper.getUserWithCircuitBreakerByEmail(orderDto.getUserEmail());

        return OrderWithUserResponse.builder()
                .orderDto(orderMapper.modelToDto(savedOrder))
                .userDto(userDto)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public OrderWithUserResponse getOrderByOrderId(UUID orderId) {
        OrderModel orderModel = findOrderByOrderId(orderId);
        UserDto userDto = userServiceFeignWrapper.getUserWithCircuitBreaker(orderModel.getUserId());

        return OrderWithUserResponse.builder()
                .orderDto(orderMapper.modelToDto(orderModel))
                .userDto(userDto)
                .build();
    }

    @Override
    @Transactional
    public OrderWithUserResponse updateByOrderId(UUID orderId, OrderDto orderDto) {
        OrderModel orderModel = findOrderByOrderId(orderId);
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
    @Transactional(readOnly = true)
    public Page<OrderWithUserResponse> getOrdersWithDateRangeAndStatuses(
            LocalDate startDate,
            LocalDate endDate,
            List<String> statuses,
            Pageable pageable
    ) {

        List<Status> statusEnums = null;
        if (statuses != null && !statuses.isEmpty()) {
            statusEnums = new ArrayList<>();
            for (String status : statuses) {
                if (status != null && !status.isBlank()) {
                    try {
                        statusEnums.add(Status.valueOf(status.toUpperCase().trim()));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid status value: {}, ignoring", status);
                    }
                }
            }
            if (statusEnums.isEmpty()) {
                statusEnums = null;
            }
        }
        Specification<OrderModel> spec = OrderSpecification.filterByDateRangeAndStatus(
                startDate, endDate, statusEnums
        );

        Page<OrderModel> orderPage = orderRepository.findAll(spec, pageable);

        List<UUID> userIds = orderPage.getContent().stream()
                .map(OrderModel::getUserId)
                .distinct()
                .toList();

        Map<UUID, UserDto> userMap = userServiceFeignWrapper.getUsersByIds(userIds).stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity()));

        return orderPage.map(order -> {
            UserDto userDto = userMap.get(order.getUserId());
            return OrderWithUserResponse.builder()
                    .orderDto(orderMapper.modelToDto(order))
                    .userDto(userDto)
                    .build();
        });
    }

    @Transactional(readOnly = true)
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

    @KafkaListener(topics = "${kafka.topics.payments-status}")
    @Override
    @Transactional
    public void updateOrderStatusFromPaymentTopic(ActionPaymentInfo actionPaymentInfo) {
        UUID orderId = UUID.fromString(actionPaymentInfo.orderId());

        OrderModel orderByOrderId = findOrderByOrderId(orderId);
        if (orderByOrderId.getStatus().equals(Status.SUCCESS))
            return;

        Status chekedStatus = actionPaymentInfo.status();
        if(chekedStatus.equals(Status.SUCCESS)) {
            chekedStatus = paymentCheckValidator.resolveStatus(actionPaymentInfo, orderByOrderId);
        }
        orderByOrderId.setStatus(chekedStatus);
    }


    private void prepareOrderForSave(OrderModel orderModel) {
        orderMapper.setOrderItemsRelation(orderModel);
        BigDecimal totalPrice = orderTotalPriceCalculator.calculateTotalPrice(orderModel.getItems());
        orderModel.setTotalPrice(totalPrice);
    }

    private OrderModel findOrderByOrderId(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Order not found with id: %s", orderId)
                ));
    }
}