package com.innowise.order.service.impl.feignWraper;

import com.innowise.order.controller.feignClient.UserServiceFeignClient;
import com.innowise.order.service.dto.feignClient.UserDto;
import com.innowise.order.service.impl.feignWraper.factory.FeignClientFallbackFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceFeignWrapper {
    private static final String USER_SERVICE_CIRCUIT_BREAKER = "user-service";

    private final FeignClientFallbackFactory factory;
    private final UserServiceFeignClient userServiceClient;

    @CircuitBreaker(name = USER_SERVICE_CIRCUIT_BREAKER, fallbackMethod = "getUserByUserIdFallback")
    public UserDto getUserWithCircuitBreaker(UUID userId) {
        return userServiceClient.getUserByUserId(userId);
    }

    @CircuitBreaker(name = USER_SERVICE_CIRCUIT_BREAKER, fallbackMethod = "getUserByEmailFallback")
    public UserDto getUserWithCircuitBreakerByEmail(String email) {
        return userServiceClient.getUserByEmail(email);
    }

    @CircuitBreaker(name = USER_SERVICE_CIRCUIT_BREAKER, fallbackMethod = "getUsersByIdsFallback")
    public List<UserDto> getUsersByIds(List<UUID> userIds) {
        return userServiceClient.getUsersByIds(userIds);
    }


    private UserDto getUserByUserIdFallback(UUID userId, Exception ex) {
        log.error("User Service unavailable for userId: {}", userId, ex);
        return factory.createFallbackUserById(userId);
    }

    private UserDto getUserByEmailFallback(String email, Exception ex) {
        log.error("User Service unavailable for email: {}", email, ex);
        return factory.createFallbackUserByEmail(email);
    }

    private List<UserDto> getUsersByIdsFallback(List<UUID> userIds, Exception ex) {
        log.error("User Service unavailable for batch request: {}", userIds, ex);
        return factory.createFallbackUsersByIds(userIds);
    }
}
