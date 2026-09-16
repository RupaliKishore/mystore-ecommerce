package com.order.ecommerceshop.service;

import com.order.ecommerceshop.dto.request.ProductInput;
import com.order.ecommerceshop.dto.response.ProductPage;
import com.order.ecommerceshop.model.Product;

import java.math.BigDecimal;

public interface ProductService
{
//    List<Product> allProduct();

    ProductPage getAllProducts(Integer page, Integer size, String category, BigDecimal minPrice, BigDecimal maxPrice, String search);

    Product getProductById(Long id);

    Product addProduct(ProductInput productInput);

    Product updateStock(Long id, Integer stockQuantity);

    Product updateProduct(Long id, ProductInput productInput);

    void deleteProduct(Long id);
}
