package com.order.ecommerceshop.repository;

import com.order.ecommerceshop.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>
{
    // review for all products
    List<Review> findByProductId(Long productId);

    // check if users specific review on product
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);


    // calculate average rating
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRatingByProductId(@Param("productId") Long productId);


    // total review count
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Long getReviewCountByProductId(@Param("productId") Long productId);


    // bathv query for all products
    @Query("SELECT r.product.id, AVG(r.rating) FROM Review r "+ "WHERE r.product.id IN :productIds GROUP BY r.product.id")
    List<Object[]> getAverageRatingForProducts(@Param("productIds") List<Long> productIds);

    @Query("SELECT r.product.id, COUNT(r)  FROM Review r "+ "WHERE r.product.id IN :productIds GROUP BY r.product.id")
    List<Object[]> getReviewCountForProducts(@Param("productIds") List<Long> productIds);

}
