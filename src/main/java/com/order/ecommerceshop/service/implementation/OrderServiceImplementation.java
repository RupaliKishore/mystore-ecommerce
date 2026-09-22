package com.order.ecommerceshop.service.implementation;

import com.order.ecommerceshop.dto.request.OrderInput;
import com.order.ecommerceshop.dto.request.OrderItemInput;
import com.order.ecommerceshop.exception.InsufficientStockException;
import com.order.ecommerceshop.exception.ResourceNotFoundException;
import com.order.ecommerceshop.model.*;
import com.order.ecommerceshop.repository.OrderRepository;
import com.order.ecommerceshop.repository.ProductRepository;
import com.order.ecommerceshop.repository.UserRepository;
import com.order.ecommerceshop.service.EmailService;
import com.order.ecommerceshop.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class OrderServiceImplementation implements OrderService
{
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;

//    private String generateOrderNumber;

    @Override
    public List<Order> allOrders() {
        return this.orderRepository.findAll();
    }

    @Override
    public Order getOrder(Long id) {
        return this.orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    // get user all order
    @Override
    public List<Order> getOrderByUserId(Long userId) {
        User user = this.userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return orderRepository.findByUser(user);
    }


    // add order
    @Override
    @Transactional
    public Order addOrder(OrderInput orderInput)
    {
        User user = userRepository.findById(orderInput.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not not found by userId "+ orderInput.getUserId()));

        // add order


        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .user(user)
                .orderItems(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // for loop for orderItem
        for(OrderItemInput orderItemInput: orderInput.getOrderItems())
        {

            // find product
            Product product = productRepository.findById(orderItemInput.getProductId()).orElseThrow(() -> new ResourceNotFoundException("Product", "id ", orderItemInput.getProductId()));

            // check stock
            if(product.getStockQuantity() < orderItemInput.getQuantity())
            {
                throw new InsufficientStockException("Insufficient stock for product "+ product.getName() +
                                                     "(Available: "+  product.getStockQuantity() +
                                                     ", Requested: "+ orderItemInput.getQuantity() + ")");
            }

            // stock -
            product.setStockQuantity(product.getStockQuantity() - orderItemInput.getQuantity());
            productRepository.save(product);

            // create orderItem
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(orderItemInput.getQuantity())
                    .price(product.getPrice())
                    .order(order)
                    .build();

            order.getOrderItems().add(orderItem);

            // total = price * quantity
            BigDecimal totalItems = product.getPrice().multiply(BigDecimal.valueOf(orderItemInput.getQuantity()));
            totalAmount = totalAmount.add(totalItems);

        }
        // set totalAmount
        order.setTotalAmount(totalAmount);
        Order saveOrder = orderRepository.save(order);

        // send order confirmation
        emailService.sendOrderConfirmationEmail(saveOrder);

        // save
       return  saveOrder;
    }

    private String generateOrderNumber()
    {

        return "ORD-" + UUID.randomUUID().toString().substring(0,8).toUpperCase();
    }


    // change order status
    @Override
    @Transactional
    public Order updateOrderStatus(Long id, OrderStatus orderStatus)
    {
        Order order = getOrder(id);

        // not change status when already order DELEVERED OR CANCELLED
        if(order.getStatus() == orderStatus.DELIVERED || order.getStatus() == orderStatus.CANCELLED) throw  new IllegalStateException("Cannot update status of a "+ order.getStatus() + " order");

        order.setStatus(orderStatus);
        Order updateOrder = orderRepository.save(order);

        // send status update email
        emailService.sendOrderStatusUpdateEmail(updateOrder);
        return updateOrder;
    }


    // cancel order
    @Override
    @Transactional
    public Order cancelOrder(Long id)
    {
        Order order = getOrder(id);
        if(order.getStatus() == OrderStatus.DELIVERED) throw  new IllegalStateException("Cannot cancel delivered order");

        if (order.getStatus() == OrderStatus.CANCELLED) throw new IllegalStateException("Order is already cancelled");

        // restore stock
        for(OrderItem orderItem:order.getOrderItems())
        {
            Product product = orderItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelOrder = orderRepository.save(order);

        // send cancel email
        emailService.sendOrderCancellationEmail(cancelOrder);
        return cancelOrder;
    }


    // delete order
    @Override
    @Transactional
    public void deleteOrder(Long id)
    {
        Order order = getOrder(id);
        orderRepository.delete(order);

    }



}
