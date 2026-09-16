package com.order.ecommerceshop.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterInput
{
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 150, message = "Name must be 2-150 characters")
    @Schema(description = "User full name", example = "Kishore Tompe")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "Email Address", example = "kishore@gmail.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8 characters")
    @Schema(description = "Password min 8 characters", example = "Kishore#@1234")
    private String password;

    @Size(max = 500, message = "Address too long")
    @Schema(description = "address", example = "Pune, Maharashtra")
    private String address;

}
