package com.innowise.order.service.impl;

import com.innowise.order.dao.enums.Status;
import com.innowise.order.dao.model.OrderModel;
import com.innowise.order.dao.repository.OrderRepository;
import com.innowise.order.service.dto.OrderDto;
import com.innowise.order.service.dto.OrderWithUserResponse;
import com.innowise.order.service.dto.feignClient.UserDto;
import com.innowise.order.service.impl.feignWraper.UserServiceFeignWrapper;
import com.innowise.order.service.impl.totalPriceCalculator.OrderTotalPriceCalculator;
import com.innowise.order.service.mapper.OrderMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderTotalPriceCalculator orderTotalPriceCalculator;

    @Mock
    private UserServiceFeignWrapper userServiceFeignWrapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID testOrderId;
    private UUID testUserId;
    private OrderModel testOrderModel;
    private OrderDto testOrderDto;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        testUserId = UUID.randomUUID();

        testOrderModel = OrderModel.builder()
                .id(testOrderId)
                .userId(testUserId)
                .status(Status.CREATED)
                .totalPrice(BigDecimal.valueOf(100.00))
                .deleted(false)
                .build();

        testOrderDto = OrderDto.builder()
                .userId(testUserId)
                .userEmail("test@email.com")
                .build();

        testUserDto = UserDto.builder()
                .id(testUserId)
                .name("Test User")
                .surname("Test Surname")
                .email("test@email.com")
                .active(true)
                .build();
    }


    @Test
    void createOrder_ShouldCreateOrderSuccessfully() {
        when(orderMapper.dtoToModel(testOrderDto)).thenReturn(testOrderModel);
        when(orderRepository.save(any(OrderModel.class))).thenReturn(testOrderModel);
        when(userServiceFeignWrapper.getUserWithCircuitBreakerByEmail("test@email.com")).thenReturn(testUserDto);
        when(orderMapper.modelToDto(testOrderModel)).thenReturn(testOrderDto);

        OrderWithUserResponse response = orderService.createOrder(testOrderDto);

        assertThat(response).isNotNull();
        assertThat(response.getOrderDto()).isEqualTo(testOrderDto);
        assertThat(response.getUserDto()).isEqualTo(testUserDto);
        assertThat(testOrderModel.getStatus()).isEqualTo(Status.CREATED);

        verify(orderMapper).dtoToModel(testOrderDto);
        verify(orderRepository).save(testOrderModel);
        verify(userServiceFeignWrapper).getUserWithCircuitBreakerByEmail("test@email.com");
    }

    @Test
    void createOrder_ShouldCalculateTotalPrice() {
        BigDecimal expectedTotal = BigDecimal.valueOf(150.00);
        testOrderDto.setUserEmail("test@email.com");

        when(orderMapper.dtoToModel(testOrderDto)).thenReturn(testOrderModel);
        when(orderTotalPriceCalculator.calculateTotalPrice(testOrderModel.getItems()))
                .thenReturn(expectedTotal);
        when(orderRepository.save(any(OrderModel.class))).thenReturn(testOrderModel);
        when(userServiceFeignWrapper.getUserWithCircuitBreakerByEmail("test@email.com")).thenReturn(testUserDto);
        when(orderMapper.modelToDto(testOrderModel)).thenReturn(testOrderDto);

        orderService.createOrder(testOrderDto);

        verify(orderTotalPriceCalculator).calculateTotalPrice(testOrderModel.getItems());
        assertThat(testOrderModel.getTotalPrice()).isEqualTo(expectedTotal);
    }


    @Test
    void getOrderByOrderId_ShouldReturnOrderWhenExists() {
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrderModel));
        when(userServiceFeignWrapper.getUserWithCircuitBreaker(testUserId)).thenReturn(testUserDto);
        when(orderMapper.modelToDto(testOrderModel)).thenReturn(testOrderDto);

        OrderWithUserResponse response = orderService.getOrderByOrderId(testOrderId);

        assertThat(response).isNotNull();
        assertThat(response.getOrderDto()).isEqualTo(testOrderDto);
        assertThat(response.getUserDto()).isEqualTo(testUserDto);

        verify(orderRepository).findById(testOrderId);
        verify(userServiceFeignWrapper).getUserWithCircuitBreaker(testUserId);
    }

    @Test
    void getOrderByOrderId_ShouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByOrderId(testOrderId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order not found with id: " + testOrderId);

        verify(orderRepository).findById(testOrderId);
        verify(userServiceFeignWrapper, never()).getUserWithCircuitBreaker(any());
    }

    @Test
    void updateByOrderId_ShouldUpdateOrderSuccessfully() {
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrderModel));
        when(orderRepository.save(any(OrderModel.class))).thenReturn(testOrderModel);
        when(userServiceFeignWrapper.getUserWithCircuitBreaker(testUserId)).thenReturn(testUserDto);
        when(orderMapper.modelToDto(testOrderModel)).thenReturn(testOrderDto);

        OrderWithUserResponse response = orderService.updateByOrderId(testOrderId, testOrderDto);

        assertThat(response).isNotNull();
        assertThat(response.getOrderDto()).isEqualTo(testOrderDto);

        verify(orderMapper).updateFromDto(testOrderDto, testOrderModel);
        verify(orderRepository).save(testOrderModel);
    }

    @Test
    void updateByOrderId_ShouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateByOrderId(testOrderId, testOrderDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order not found with id: " + testOrderId);

        verify(orderMapper, never()).updateFromDto(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrdersWithDateRangeAndStatuses_ShouldReturnFilteredOrders() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        List<String> statuses = List.of("CREATED", "PENDING");
        Pageable pageable = Pageable.ofSize(10);
        Page<OrderModel> orderPage = new PageImpl<>(List.of(testOrderModel));

        List<UUID> userIds = List.of(testUserId);
        List<UserDto> userDtos = List.of(testUserDto);

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(userServiceFeignWrapper.getUsersByIds(userIds)).thenReturn(userDtos);
        when(orderMapper.modelToDto(testOrderModel)).thenReturn(testOrderDto);

        Page<OrderWithUserResponse> response = orderService.getOrdersWithDateRangeAndStatuses(
                startDate, endDate, statuses, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getOrderDto()).isEqualTo(testOrderDto);

        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
        verify(userServiceFeignWrapper).getUsersByIds(userIds);
    }

    @Test
    void getOrdersWithDateRangeAndStatuses_ShouldIgnoreInvalidStatus() {
        List<String> invalidStatus = List.of("INVALID_STATUS");
        Pageable pageable = Pageable.ofSize(10);
        Page<OrderModel> orderPage = new PageImpl<>(List.of(testOrderModel));
        List<UUID> userIds = List.of(testUserId);
        List<UserDto> userDtos = List.of(testUserDto);

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(userServiceFeignWrapper.getUsersByIds(userIds)).thenReturn(userDtos);
        when(orderMapper.modelToDto(testOrderModel)).thenReturn(testOrderDto);

        Page<OrderWithUserResponse> response = orderService.getOrdersWithDateRangeAndStatuses(
                null, null, invalidStatus, pageable);

        assertThat(response).isNotNull();
        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
        verify(userServiceFeignWrapper).getUsersByIds(userIds);
    }

    @Test
    void getOrdersWithDateRangeAndStatuses_ShouldHandleNullStatus() {
        Pageable pageable = Pageable.ofSize(10);
        Page<OrderModel> orderPage = new PageImpl<>(List.of(testOrderModel));
        List<UUID> userIds = List.of(testUserId);
        List<UserDto> userDtos = List.of(testUserDto);

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(userServiceFeignWrapper.getUsersByIds(userIds)).thenReturn(userDtos);
        when(orderMapper.modelToDto(testOrderModel)).thenReturn(testOrderDto);

        Page<OrderWithUserResponse> response = orderService.getOrdersWithDateRangeAndStatuses(
                null, null, null, pageable);

        assertThat(response).isNotNull();
        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
        verify(userServiceFeignWrapper).getUsersByIds(userIds);
    }

    @Test
    void getOrdersByUserId_ShouldReturnOrdersForUser() {
        Pageable pageable = Pageable.ofSize(10);
        Page<OrderModel> orderPage = new PageImpl<>(List.of(testOrderModel));

        when(userServiceFeignWrapper.getUserWithCircuitBreaker(testUserId)).thenReturn(testUserDto);
        when(orderRepository.findAllByUserId(testUserId, pageable)).thenReturn(orderPage);
        when(orderMapper.modelToDto(testOrderModel)).thenReturn(testOrderDto);

        Page<OrderWithUserResponse> response = orderService.getOrdersByUserId(testUserId, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getUserDto()).isEqualTo(testUserDto);

        verify(orderRepository). findAllByUserId(testUserId, pageable);
    }

    @Test
    void deleteByOrderId_ShouldDeleteOrderWhenExists() {
        when(orderRepository.existsById(testOrderId)).thenReturn(true);
        doNothing().when(orderRepository).deleteById(testOrderId);

        orderService.deleteByOrderId(testOrderId);

        verify(orderRepository).existsById(testOrderId);
        verify(orderRepository).deleteById(testOrderId);
    }

    @Test
    void deleteByOrderId_ShouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.existsById(testOrderId)).thenReturn(false);

        assertThatThrownBy(() -> orderService.deleteByOrderId(testOrderId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order not found with orderId " + testOrderId);

        verify(orderRepository).existsById(testOrderId);
        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    void sofDeleteByOrderId_ShouldMarkOrderAsDeleted() {
        testOrderModel.setDeleted(false);
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrderModel));

        boolean result = orderService.sofDeleteByOrderId(testOrderId);

        assertThat(result).isTrue();
        assertThat(testOrderModel.isDeleted()).isTrue();
        verify(orderRepository).findById(testOrderId);
    }

    @Test
    void sofDeleteByOrderId_ShouldRestoreOrderWhenAlreadyDeleted() {
        testOrderModel.setDeleted(true);
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrderModel));

        boolean result = orderService.sofDeleteByOrderId(testOrderId);

        assertThat(result).isFalse();
        assertThat(testOrderModel.isDeleted()).isFalse();
        verify(orderRepository).findById(testOrderId);
    }

    @Test
    void sofDeleteByOrderId_ShouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.sofDeleteByOrderId(testOrderId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order not found with id: " + testOrderId);

        verify(orderRepository).findById(testOrderId);
    }

    @Test
    void prepareOrderForSave_ShouldSetOrderItemsRelationAndTotalPrice() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        BigDecimal calculatedPrice = BigDecimal.valueOf(200.00);
        when(orderTotalPriceCalculator.calculateTotalPrice(testOrderModel.getItems()))
                .thenReturn(calculatedPrice);

        java.lang.reflect.Method method = OrderServiceImpl.class.getDeclaredMethod(
                "prepareOrderForSave", OrderModel.class);
        method.setAccessible(true);
        method.invoke(orderService, testOrderModel);

        verify(orderMapper).setOrderItemsRelation(testOrderModel);
        verify(orderTotalPriceCalculator).calculateTotalPrice(testOrderModel.getItems());
        assertThat(testOrderModel.getTotalPrice()).isEqualTo(calculatedPrice);
    }

    @Test
    void findOrderById_ShouldReturnOrderWhenExistsOrder() throws Exception {
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrderModel));

        java.lang.reflect.Method method = OrderServiceImpl.class.getDeclaredMethod(
                "findOrderByOrderId", UUID.class);
        method.setAccessible(true);
        OrderModel result = (OrderModel) method.invoke(orderService, testOrderId);

        assertThat(result).isEqualTo(testOrderModel);
        verify(orderRepository).findById(testOrderId);
    }

    @Test
    void findOrderByOrderId_ShouldThrowExceptionWhenNotFound() throws Exception {
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.empty());

        java.lang.reflect.Method method = OrderServiceImpl.class.getDeclaredMethod(
                "findOrderByOrderId", UUID.class);
        method.setAccessible(true);

        assertThatThrownBy(() -> method.invoke(orderService, testOrderId))
                .hasCauseInstanceOf(EntityNotFoundException.class);

        verify(orderRepository).findById(testOrderId);
    }
}