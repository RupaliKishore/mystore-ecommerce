package com.order.ecommerceshop.dto.response;

import com.order.ecommerceshop.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthPayload
{
    private String token;
    private Long userId;
    private String name;
    private String email;
    private UserRole role;
}
