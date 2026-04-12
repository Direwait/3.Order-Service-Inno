package com.innowise.order.service.impl.feignWraper;

import com.innowise.order.controller.feignClient.UserServiceFeignClient;
import com.innowise.order.service.dto.feignClient.UserDto;
import com.innowise.order.service.impl.feignWraper.factory.FeignClientFallbackFactory;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public UserDto getUserWithCircuitBreaker(UUID userId) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(USER_SERVICE_CIRCUIT_BREAKER);

        return circuitBreaker.executeSupplier(() -> {
            try {
                return userServiceClient.getUserByUserId(userId);
            } catch (Exception e) {
                log.error("User Service unavailable for userId: {}", userId, e);
                return factory.createFallbackUserById(userId);
            }
        });
    }

    public UserDto getUserWithCircuitBreakerByEmail(String email) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(USER_SERVICE_CIRCUIT_BREAKER);

        return circuitBreaker.executeSupplier(() -> {
            try {
                return userServiceClient.getUserByEmail(email);
            } catch (Exception e) {
                log.error("User Service unavailable for email: {}", email, e);
                return factory.createFallbackUserByEmail(email);
            }
        });
    }

    public List<UserDto> getUsersByIds(List<UUID> userIds) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(USER_SERVICE_CIRCUIT_BREAKER);

        return circuitBreaker.executeSupplier(() -> {
            try {
                return userServiceClient.getUsersByIds(userIds);
            } catch (Exception e) {
                log.error("User Service unavailable for batch request: {}", userIds, e);
                return factory.createFallbackUsersByIds(userIds);
            }
        });
    }
}
