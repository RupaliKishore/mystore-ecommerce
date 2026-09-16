package com.order.ecommerceshop.dto.response;

import com.order.ecommerceshop.model.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductPage
{
    private List<Product> content; // product list

    private Long totalElements;  // total product

    private Integer totalPages;

    private Integer currentPage;

    private Integer pageSize;
}
