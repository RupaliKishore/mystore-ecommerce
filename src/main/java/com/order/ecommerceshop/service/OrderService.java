package com.order.ecommerceshop.service;

import com.order.ecommerceshop.dto.request.OrderInput;
import com.order.ecommerceshop.model.Order;
import com.order.ecommerceshop.model.OrderStatus;

import java.util.List;

public interface OrderService
{

    List<Order> allOrders();

    Order getOrder(Long id);

    List<Order> getOrderByUserId(Long userId);

    Order addOrder(OrderInput orderInput);

    Order updateOrderStatus(Long id, OrderStatus status);

    Order cancelOrder(Long id);
    void deleteOrder(Long id);
}
