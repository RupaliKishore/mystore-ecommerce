package com.order.ecommerceshop.repository;

import com.order.ecommerceshop.model.Order;
import com.order.ecommerceshop.model.OrderStatus;
import com.order.ecommerceshop.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>
{

    List<Order> findByUser(User user);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByStatus(OrderStatus status);


}
