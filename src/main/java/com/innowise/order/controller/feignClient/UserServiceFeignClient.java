package com.innowise.order.controller.feignClient;

import com.innowise.order.service.dto.feignClient.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "user-service",
        url = "${user-service-url:http://localhost:8080}"
)
public interface UserServiceFeignClient {

    @GetMapping("/users/by-email")
    UserDto getUserByEmail(@RequestParam String email);

    @GetMapping("/users/batch")
    List<UserDto> getUsersByIds(@RequestParam List<UUID> ids);

    @GetMapping("/users/{userId}")
    UserDto getUserByUserId(@PathVariable UUID userId);
}