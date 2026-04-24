package com.innowise.order.service.impl.feignWraper.factory;

import org.springframework.stereotype.Component;
import com.innowise.order.service.dto.feignClient.UserDto;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class FeignClientFallbackFactory {

    public UserDto createFallbackUserById(UUID userId) {
        log.error("User Service unavailable for userId: {}", userId);
        return UserDto.builder()
                .id(userId)
                .name("Unknown name")
                .surname("Unknown surname")
                .email("Unknown email")
                .active(false)
                .build();
    }

    public UserDto createFallbackUserByEmail(String email) {
        log.error("User Service unavailable for email: {}", email);
        return UserDto.builder()
                .id(null)
                .name("Unknown name")
                .surname("Unknown surname")
                .email(email)
                .active(false)
                .build();
    }

    public List<UserDto> createFallbackUsersByIds(List<UUID> userIds) {
        log.error("User Service unavailable for batch request: {}", userIds);
        return userIds.stream()
                .map(id -> UserDto.builder()
                        .id(id)
                        .name("Unknown name")
                        .surname("Unknown surname")
                        .email("Unknown email")
                        .active(false)
                        .build())
                .toList();
    }
}
