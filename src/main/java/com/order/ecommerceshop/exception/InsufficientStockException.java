package com.order.ecommerceshop.exception;

public class InsufficientStockException extends RuntimeException
{
    public InsufficientStockException(String message)
    {
         super(message);
    }

    public InsufficientStockException(String productName, Integer available, Integer requested)
    {
        super(String.format("Insufficient stock for %s. Available: %d, Requested: %d", productName, available, requested ));
    }

}
