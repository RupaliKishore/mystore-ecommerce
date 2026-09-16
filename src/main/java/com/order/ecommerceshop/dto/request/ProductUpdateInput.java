package com.order.ecommerceshop.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
@Data
public class ProductUpdateInput
{
    @Size(min = 2, max = 150, message = "Name must be 2-150 characters")
    @Schema(description = "New Product name", example = "Dress", nullable = true)
    private String name;

    @Size(max = 1000, message = "Description too long")
    @Schema(description = "New Description", example = "red dress", nullable = true)
    private String description;

    @Positive(message = "Proce must be greater than 0")
    @Schema(description = "New Price", example = "1500", nullable = true)
    private BigDecimal price;

    @PositiveOrZero(message = "Quantity can not be negative")
    @Schema(description = "new Quantity", example = "50", nullable = true)
    private Integer stockQuantity;

    @Size(max = 100)
    @Schema(description = "New category", example = "One piece",nullable = true)
    private String category;

    @Size(max = 500)
    @Schema(description = "New image URL", example = "https://images.unsplash.com/photo-xxx?w=400", nullable = true)
    private String imageUrl;
}
