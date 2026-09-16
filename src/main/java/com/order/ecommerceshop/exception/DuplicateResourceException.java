package com.order.ecommerceshop.exception;

public class DuplicateResourceException extends RuntimeException
{
    public DuplicateResourceException(String message)
    {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue)
    {
        super(String.format("%s already exist with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
