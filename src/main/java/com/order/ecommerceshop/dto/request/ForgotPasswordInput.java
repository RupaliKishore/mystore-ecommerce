package com.order.ecommerceshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordInput
{
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid Email format")
    private String email;
}
