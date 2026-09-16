package com.order.ecommerceshop.controller;

import com.order.ecommerceshop.dto.request.ProductInput;
import com.order.ecommerceshop.dto.response.ProductPage;
import com.order.ecommerceshop.model.Product;
import com.order.ecommerceshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class ProductController
{
    private final ProductService productService;

    @QueryMapping
    public ProductPage getAllProducts(@Argument Integer page, @Argument Integer size,
                                      @Argument String category, @Argument BigDecimal minPrice,
                                      @Argument BigDecimal maxPrice, @Argument String search)
    {
         // null saftey
        int pageNumber = (page != null) ? page : 0;
        int pageSize = (size != null) ? size : 10;
        BigDecimal min = minPrice;
        BigDecimal max = maxPrice;

            return  productService.getAllProducts(pageNumber, pageSize, category, min, max, search);
    }


    @QueryMapping
    public Product getProduct(@Argument Long id)
    {
        return this.productService.getProductById(id);
    }


    // admin only mutations

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Product addProduct(@Argument ProductInput productInput)
    {
        return this.productService.addProduct(productInput);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Product updateProduct(@Argument ProductInput productInput, @Argument Long id)
    {
        Product product = this.productService.getProductById(id);
        product.setName(productInput.getName());
        return productService.updateProduct(id, productInput);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public  Product updateStock(@Argument Long id, @Argument Integer stockQuantity)
    {
        return this.productService.updateStock(id, stockQuantity);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteProduct(@Argument Long id)
    {
        this.productService.deleteProduct(id);
        return  "Product deleted";
    }
}
