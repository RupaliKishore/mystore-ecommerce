package com.order.ecommerceshop.controller;

import com.order.ecommerceshop.dto.request.ProductInput;
import com.order.ecommerceshop.dto.response.ProductPage;
import com.order.ecommerceshop.model.Product;
import com.order.ecommerceshop.service.ProductService;
import com.order.ecommerceshop.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProductController
{
    private final ProductService productService;
    private final ReviewService reviewService;

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
    public Product addProduct(@Argument @Valid ProductInput productInput)
    {
        return this.productService.addProduct(productInput);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Product updateProduct(@Argument @Valid ProductInput productInput, @Argument Long id)
    {
//        Product product = this.productService.getProductById(id);
        return productService.updateProduct(id, productInput);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public  Product updateStock(@Argument Long id, @Argument @Valid Integer stockQuantity)
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


    @BatchMapping(typeName = "Product", field = "averageRating")
    public Map<Product, Double> averageRating(List<Product> products)
    {
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());

        // single query for all products
        Map<Long, Double> ratingMap = reviewService.getAverageRatings(productIds);

        return products.stream()
                .collect(Collectors.toMap(
                        product -> product,
                        product -> ratingMap.getOrDefault(product.getId(), 0.0)));
    }



    @BatchMapping(typeName = "Product", field = "totalReviews")
    public Map<Product, Long> totalReviews(List<Product> products)
    {
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        Map<Long, Long> countMap = reviewService.getReviewCount(productIds);

        return products.stream()
                .collect(Collectors.toMap(
                        product -> product,
                        product -> countMap.getOrDefault(product.getId(), 0L)
                ));
    }
}
