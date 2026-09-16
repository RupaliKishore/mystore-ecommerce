package com.order.ecommerceshop.controller;

import com.order.ecommerceshop.dto.request.OrderInput;
import com.order.ecommerceshop.exception.ResourceNotFoundException;
import com.order.ecommerceshop.model.Order;
import com.order.ecommerceshop.model.OrderStatus;
import com.order.ecommerceshop.model.User;
import com.order.ecommerceshop.repository.UserRepository;
import com.order.ecommerceshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller

@RequiredArgsConstructor
public class OrderController
{

    private final OrderService orderService;
    private final UserRepository userRepository;

    // get userId by token
    private Long getCurrentUserId()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) throw new IllegalStateException(("User not authenticated. please login first"));

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("user", "email", email));
        return user.getId();

    }


    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Order> allOrders()
    {
        return orderService.allOrders();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Order getOrder(@Argument Long id)
    {
        return orderService.getOrder(id);
    }

    @QueryMapping
    public List<Order> myOrders()
    {
        Long userId = getCurrentUserId();
        return orderService.getOrderByUserId(userId);
    }

    @MutationMapping
    public Order addOrder(@Argument OrderInput orderInput)
    {
        // get userId by token
        Long userId = getCurrentUserId();
        orderInput.setUserId(userId);
        return orderService.addOrder(orderInput);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public  Order updateOrderStatus(@Argument Long id ,@Argument OrderStatus status)
    {
        return orderService.updateOrderStatus(id, status);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteOrder(@Argument Long id)
    {
        orderService.deleteOrder(id);
        return "Order deleted";
    }

    // cancel
    @MutationMapping
    public Order cancelOrder(@Argument Long id)
    {
        // verify ownership
        Order order = orderService.getOrder(id);
        Long currentUserId = getCurrentUserId();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if(!isAdmin && !order.getUser().getId().equals(currentUserId)) throw  new IllegalStateException("You can only cancel your own orders");

        return orderService.cancelOrder(id);
    }
}
