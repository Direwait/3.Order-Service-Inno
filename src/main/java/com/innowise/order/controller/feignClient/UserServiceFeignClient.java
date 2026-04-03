package com.innowise.order.controller.feignClient;

import com.innowise.order.service.dto.feignClient.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "user-service",
        url = "${user-service-url:http://localhost:8080}"
)
public interface UserServiceFeignClient {

    @GetMapping("/users/{userId}")
    UserDto getUserById(@PathVariable("userId") UUID userId);
}