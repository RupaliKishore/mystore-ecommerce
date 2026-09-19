package com.order.ecommerceshop.controller;

import com.order.ecommerceshop.dto.request.ReviewInput;
import com.order.ecommerceshop.exception.ResourceNotFoundException;
import com.order.ecommerceshop.model.Review;
import com.order.ecommerceshop.model.User;
import com.order.ecommerceshop.repository.UserRepository;
import com.order.ecommerceshop.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReviewController
{
    private final ReviewService reviewService;
    private final UserRepository userRepository;

    // get userId from token
    private Long getCurrentUserId()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName()))
        {
            throw new IllegalStateException("User not authenticated. Please login first.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return user.getId();
    }


    @QueryMapping
    public List<Review> getProductReviews(@Argument Long productId)
    {
        return reviewService.getReviewByProduct(productId);
    }

    @QueryMapping
    public Double getProductAverageRating(@Argument Long productId)
    {
        return reviewService.getAverageRating(productId);
    }

    @QueryMapping
    public Long getProductReviewCount(@Argument Long productId)
    {
        return reviewService.getReviewCount(productId);
    }

    @MutationMapping
    public Review addReview(@Argument @Valid ReviewInput reviewInput)
    {
        Long userId = getCurrentUserId();
       return reviewService.addReview(userId, reviewInput);
    }

    @MutationMapping
    public String deleteReview(@Argument Long id)
    {
        Long userId = getCurrentUserId();
        reviewService.deleteReview(id, userId);
        return "Review Deleted.";
    }
}
