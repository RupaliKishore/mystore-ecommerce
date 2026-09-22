package com.order.ecommerceshop.service.implementation;

import com.order.ecommerceshop.dto.request.ProductInput;
import com.order.ecommerceshop.dto.response.ProductPage;
import com.order.ecommerceshop.exception.ResourceNotFoundException;
import com.order.ecommerceshop.model.Product;
import com.order.ecommerceshop.repository.ProductRepository;
import com.order.ecommerceshop.service.ProductService;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImplementation implements ProductService
{
    private final ProductRepository productRepository;

    private static final int MAX_PAGE_SIZE = 100;

//    @Override
//    public List<Product> allProduct() {
//        return this.productRepository.findAll();
//    }

    // product page
    @Override
    public ProductPage getAllProducts(Integer page, Integer size, String category, BigDecimal minPrice, BigDecimal maxPrice, String search)
    {
        int pageNumber = (page == null || page < 0) ? 0 : page;
        int pageSize = (size == null || size <= 0) ? 10 : Math.min(size, MAX_PAGE_SIZE);

        // create pageable descending by id
        Pageable pageable =  PageRequest.of(pageNumber, pageSize, Sort.by("id").descending());

        // create specification for filtering
        Specification<Product> specification = (root, query, cb) ->
        {
            List<Predicate> predicates = new ArrayList<>();

            // category filter
            if(StringUtils.hasText(category))  predicates.add(cb.equal(root.get("category"), category));

            // min price
            if(minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));

            // max price filter
            if(maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));

            // search String
            if(StringUtils.hasText(search))
            {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description"))      , pattern);
                predicates.add(cb.or(nameLike, descLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // get page by repository
        Page<Product> productPage = productRepository.findAll(specification, pageable);
        return ProductPage.builder()
                .content(productPage.getContent())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .currentPage(productPage.getNumber())
                .pageSize(productPage.getSize())
                .build();
    }

    @Override
    public Product getProductById(Long id) {
        return this.productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product","id", id));
    }

    @Override
    @Transactional
    public Product addProduct(ProductInput productInput)
    {
        if(productInput.getName() == null || productInput.getName().isBlank()) throw  new IllegalArgumentException("Product name is required");
        if(productInput.getPrice() == null) throw new IllegalArgumentException("Product price is required");
        if(productInput.getPrice().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Price is must be greater than 0");
        if(productInput.getStockQuantity() == null) throw new IllegalArgumentException("Stock quantity is required");
        if(productInput.getStockQuantity() < 0) throw new IllegalArgumentException("Stock quantity cannot be negative");


        Product product = new Product();
        product.setName(productInput.getName());
        product.setDescription(productInput.getDescription());
        product.setPrice(productInput.getPrice());
        product.setStockQuantity(productInput.getStockQuantity());
        product.setCategory(productInput.getCategory());
        product.setImageUrl(productInput.getImageUrl());

        return this.productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateStock(Long id, Integer stockQuantity)
    {
        Product existProduct =  this.productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product ID not found"));
        if(stockQuantity == null || stockQuantity < 0) throw new IllegalArgumentException("stockQuantity or stockQuantity < 0");


        existProduct.setStockQuantity(stockQuantity);
        return this.productRepository.save(existProduct);
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, ProductInput productInput)
    {
        Product existProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (productInput.getName() != null && !productInput.getName().isBlank()) {
            existProduct.setName(productInput.getName());
        }
        if (productInput.getDescription() != null) {
            existProduct.setDescription(productInput.getDescription());
        }
        if (productInput.getPrice() != null) {
            existProduct.setPrice(productInput.getPrice());
        }
        if (productInput.getStockQuantity() != null) {
            existProduct.setStockQuantity(productInput.getStockQuantity());
        }
        if (productInput.getCategory() != null && !productInput.getCategory().isBlank()) {
            existProduct.setCategory(productInput.getCategory());
        }
        if (productInput.getImageUrl() != null) {
            existProduct.setImageUrl(productInput.getImageUrl());
        }

      return productRepository.save(existProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id)
    {
        Product existProduct = this.productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product ID not found "+ id));
        this.productRepository.delete(existProduct);

    }
}
