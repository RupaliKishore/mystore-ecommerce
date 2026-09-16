package com.order.ecommerceshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessagePayload
{
    private String message;

    private Boolean success;
}
