package com.order.ecommerceshop.repository;

import com.order.ecommerceshop.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product>
{

    List<Product> findByCategory(String category);

    List<Product> findByStockQuantityGreaterThan(int stockQuantity);



}
