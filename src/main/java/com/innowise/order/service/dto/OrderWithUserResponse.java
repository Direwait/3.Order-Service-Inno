package com.innowise.order.service.dto;

import com.innowise.order.service.dto.feignClient.UserDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OrderWithUserResponse {
    private OrderDto orderDto;
    private UserDto userDto;
}