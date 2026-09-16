package com.order.ecommerceshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Test API", description = "for swagger testing Rest API")
public class TestController
{


    @GetMapping("/hello")
    @Operation(summary = "Hello, world API", description = "test swagger")
    public Map<String, String> sayHello()
    {
        Map<String, String> response = new HashMap<>();
        response.put("Messsage", "hello Swagger test");
        return  response;
    }

}
