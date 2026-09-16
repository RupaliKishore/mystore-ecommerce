package com.order.ecommerceshop.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateInput
{
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    @Schema(description = "new Name", example = "Kishore Tompe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Email(message = "Email should be valid")
    @Schema(description = "New Email", example = "kishore@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Schema(description = "New Password", example = "kishore@142356", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Size(max = 100, message = "Address too long")
    @Schema(description = "New Address", example = "Pune, Maharashtra", requiredMode = Schema.RequiredMode.REQUIRED)
    private String address;
}
