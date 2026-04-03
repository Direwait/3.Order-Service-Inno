package com.innowise.order.service.dto.feignClient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto implements Serializable {
    private UUID id;

    @NotBlank(message = "Name is not empty")
    private String name;

    @NotBlank(message = "Surname is not empty")
    private String surname;

    @Past(message = "Birthdate must be in past")
    private LocalDate birthDate;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private boolean active;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
