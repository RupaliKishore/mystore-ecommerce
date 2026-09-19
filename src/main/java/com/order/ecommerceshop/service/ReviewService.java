package com.order.ecommerceshop.service;

import com.order.ecommerceshop.dto.request.ReviewInput;
import com.order.ecommerceshop.model.Review;

import java.util.List;
import java.util.Map;

public interface ReviewService
{

    Review addReview(Long userId, ReviewInput reviewInput);

    List<Review> getReviewByProduct(Long productId);

    void deleteReview(Long id, Long userId);

    Double getAverageRating(Long productId);

    Long getReviewCount(Long productId);


    Map<Long, Double> getAverageRatings(List<Long> productIds);

    Map<Long, Long> getReviewCount(List<Long> productIds);
}
