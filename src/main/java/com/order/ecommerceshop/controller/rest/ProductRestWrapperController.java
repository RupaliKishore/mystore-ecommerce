package com.order.ecommerceshop.controller.rest;

import com.order.ecommerceshop.dto.request.ProductInput;
import com.order.ecommerceshop.dto.request.ProductUpdateInput;
import com.order.ecommerceshop.dto.response.ProductPage;
import com.order.ecommerceshop.model.Product;
import com.order.ecommerceshop.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/rest/product")
@Tag(name = "Product Rest API", description = "Product management through swagger")

@RequiredArgsConstructor
public class ProductRestWrapperController
{
    private final ProductService productService;


    @GetMapping
    @Operation(summary = "product list")
    public ProductPage allProducts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String search)
    {

        return this.productService.getAllProducts(page, size,category, minPrice, maxPrice, search);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find Product by ID")
    public ResponseEntity<Product> getProduct(@PathVariable Long id)
    {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // admin only
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "add new Product")
    public ResponseEntity<Product> addProduct(@Valid @RequestBody ProductInput productInput)
    {
       Product addproduct = productService.addProduct(productInput);
       return new  ResponseEntity<>(addproduct,HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "update Product")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductUpdateInput productUpdateInput)
    {
        ProductInput productInput = new ProductInput();
        productInput.setName(productUpdateInput.getName());
        productInput.setDescription(productUpdateInput.getDescription());
        productInput.setPrice(productUpdateInput.getPrice());
        productInput.setStockQuantity(productUpdateInput.getStockQuantity());
        productInput.setCategory(productUpdateInput.getCategory());
        productInput.setImageUrl(productUpdateInput.getImageUrl());
        return ResponseEntity.ok(productService.updateProduct(id, productInput));
    }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "only stock quantity update")
    public ResponseEntity<Product> updateStock(@PathVariable Long id,  @RequestParam Integer quantity)
    {
         return ResponseEntity.ok(productService.updateStock(id, quantity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Product by id")
    public ResponseEntity<Boolean> deleteProduct(@PathVariable Long id)
    {
        this.productService.deleteProduct(id);
        return ResponseEntity.ok(true);
    }



}
