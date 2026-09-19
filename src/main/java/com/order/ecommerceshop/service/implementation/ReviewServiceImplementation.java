package com.order.ecommerceshop.service.implementation;

import com.order.ecommerceshop.dto.request.ReviewInput;
import com.order.ecommerceshop.exception.DuplicateResourceException;
import com.order.ecommerceshop.exception.ResourceNotFoundException;
import com.order.ecommerceshop.model.Product;
import com.order.ecommerceshop.model.Review;
import com.order.ecommerceshop.model.User;
import com.order.ecommerceshop.repository.ProductRepository;
import com.order.ecommerceshop.repository.ReviewRepository;
import com.order.ecommerceshop.repository.UserRepository;
import com.order.ecommerceshop.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewServiceImplementation implements ReviewService
{
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public Review addReview(Long userId, ReviewInput reviewInput)
    {
        // check user exist
        User user = this.userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // check product exist
        Product product = this.productRepository.findById(reviewInput.getProductId()).orElseThrow(() -> new ResourceNotFoundException("Product", "productId", reviewInput.getProductId()));

        // check review already exist
        if(reviewRepository.findByUserIdAndProductId(userId, reviewInput.getProductId()).isPresent()) throw new DuplicateResourceException("Review", "user-product", userId + reviewInput.getProductId());

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(reviewInput.getRating())
                .comment(reviewInput.getComment())
                .build();

        return reviewRepository.save(review);
    }


    @Override
    public List<Review> getReviewByProduct(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    @Override
    public void deleteReview(Long id, Long userId)
    {
        Review review = reviewRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));

        // only owner can delete review
        if(!review.getUser().getId().equals(userId)) throw new IllegalStateException("You can delete your own review");

        reviewRepository.delete(review);

    }

    @Override
    public Double getAverageRating(Long productId)
    {
        Double average  = reviewRepository.getAverageRatingByProductId(productId);
        return average != null ? Math.round(average * 10.0) / 10.0 : 0.0;  // round to 1 decimal
    }

    @Override
    public Long getReviewCount(Long productId)
    {
        return reviewRepository.getReviewCountByProductId(productId);
    }

    @Override
    public Map<Long, Double> getAverageRatings(List<Long> productIds) {
        List<Object[]> results = reviewRepository.getAverageRatingForProducts(productIds);
        Map<Long, Double> map = new HashMap<>();

        for (Object[] row : results) {
            Long productId = (Long) row[0];
            Double avg = (Double) row[1];
            // Round to 1 decimal
            map.put(productId, Math.round(avg * 10.0) / 10.0);
        }

        return map;
    }

    @Override
    public Map<Long, Long> getReviewCount(List<Long> productIds)
    {
        List<Object[]> results = reviewRepository.getReviewCountForProducts(productIds);
        Map<Long, Long> map = new HashMap<>();

        for (Object[] row : results) {
            map.put((Long) row[0], (Long) row[1]);
        }

        return map;
    }
}
