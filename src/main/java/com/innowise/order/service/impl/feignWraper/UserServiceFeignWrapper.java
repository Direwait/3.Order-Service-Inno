package com.innowise.order.service.impl.feignWraper;

import com.innowise.order.controller.feignClient.UserServiceFeignClient;
import com.innowise.order.service.dto.feignClient.UserDto;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceFeignWrapper {
    private static final String USER_SERVICE_CIRCUIT_BREAKER = "user-service";

    private final UserServiceFeignClient userServiceClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public UserDto getUserWithCircuitBreaker(UUID userId) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(USER_SERVICE_CIRCUIT_BREAKER);

        return circuitBreaker.executeSupplier(() -> {
            try {
                return userServiceClient.getUserById(userId);
            } catch (Exception e) {
                log.error("User Service unavailable for userId: {}", userId, e);
                return UserDto.builder()
                        .id(userId)
                        .name("Unknow name")
                        .surname("Unknow surname")
                        .active(false)
                        .build();
            }
        });
    }
}
