package com.order.ecommerceshop.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderInput
{
    private Long userId; // internal service use only

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemInput> orderItems;
}
