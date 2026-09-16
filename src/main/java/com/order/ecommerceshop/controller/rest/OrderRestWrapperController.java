package com.order.ecommerceshop.controller.rest;

import com.order.ecommerceshop.dto.request.OrderInput;
import com.order.ecommerceshop.exception.ResourceNotFoundException;
import com.order.ecommerceshop.model.Order;
import com.order.ecommerceshop.model.OrderStatus;
import com.order.ecommerceshop.model.User;
import com.order.ecommerceshop.repository.UserRepository;
import com.order.ecommerceshop.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rest/order")
@Tag(name = "Order Rest API", description = "Order Management by swagger ")
@RequiredArgsConstructor

@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@SecurityRequirement(name = "bearerAuth")
public class OrderRestWrapperController
{
    private final OrderService orderService;
    private final UserRepository userRepository;

    // admin only

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Order list")
    public ResponseEntity<List<Order>> allOrders()
    {
         return ResponseEntity.ok(this.orderService.allOrders());
    }

    @GetMapping("/{id}")
    @Operation(summary = "get order by ID")
    public ResponseEntity<Order> getOrder(@PathVariable long id, Authentication authentication)
    {
        Order order = orderService.getOrder(id);

        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if(!isAdmin)
        {
            User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User", "email", authentication.getName()));

            if(!order.getUser().getId().equals(user.getId())) throw  new IllegalStateException("You can view only your orders");

        }

        return ResponseEntity.ok(order);

    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Order get by userId")
    public ResponseEntity<List<Order>> getOrderByUser(@PathVariable Long userId)
    {
        return ResponseEntity.ok(this.orderService.getOrderByUserId(userId));
    }


    @PostMapping
    @Operation(summary = "Order created")
    @ApiResponse(responseCode = "200", description = "Order added successfully")
    public ResponseEntity<Order> addOrder(@Valid @RequestBody OrderInput orderInput, Authentication authentication)
    {
       User user =  userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User", "email", authentication.getName()));

       orderInput.setUserId(user.getId());

        Order createorder = this.orderService.addOrder(orderInput);
        return new ResponseEntity<>(createorder,HttpStatus.CREATED);
    }


    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "change order status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus orderStatus)
    {
         return ResponseEntity.ok(this.orderService.updateOrderStatus(id, orderStatus));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Order cancel successfully")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id, Authentication authentication)
    {
        Order order = orderService.getOrder(id);
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if(!isAdmin)
        {
            User user =  userRepository.findByEmail(authentication.getName()).orElseThrow(() ->  new ResourceNotFoundException("User", "email", authentication.getName()));

            if(!order.getUser().getId().equals(user.getId())) throw new IllegalStateException("You can cancel your order only");
        }
        return ResponseEntity.ok(this.orderService.cancelOrder(id));
    }
}
