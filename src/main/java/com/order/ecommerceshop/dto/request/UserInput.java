package com.order.ecommerceshop.dto.request;

import com.order.ecommerceshop.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class UserInput
{
    private Long id;

    @NotBlank(message = "Name is required")
    @Schema(description = "User full name", example = "Kishore Tompe")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "Email address", example = "kishore@gmail.com")
    private String email;


    @NotBlank(message = "Password is must required")
    @Schema(description = "password min(6 character)", example = "MySecrete1234")
    private String password;

    @Schema(description = "address", example = "Pune, Maharashtra")
    private String address;

    private UserRole role;

    private String resetToken;

    private String resetTokenExpiry;

    private LocalDateTime createdAt;
}
