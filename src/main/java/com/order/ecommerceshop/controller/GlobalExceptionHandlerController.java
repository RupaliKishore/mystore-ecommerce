//package com.order.ecommerceshop.controller;
//
//import com.order.ecommerceshop.exception.InsufficientStockException;
//import com.order.ecommerceshop.exception.ResourceNotFoundException;
//import graphql.GraphQLError;
//import graphql.GraphqlErrorBuilder;
//import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//
//@ControllerAdvice
//public class GlobalExceptionHandlerController
//{
//
//  @ControllerAdvice
//    public class GlobalExceptionHandler {
//
//        @GraphQlExceptionHandler
//        public GraphQLError handleResourceNotFound(ResourceNotFoundException ex) {
//            return GraphqlErrorBuilder.newError().message(ex.getMessage()).build();
//        }
//
//        @GraphQlExceptionHandler
//        public GraphQLError handleInsufficientStock(InsufficientStockException ex) {
//            return GraphqlErrorBuilder.newError().message(ex.getMessage()).build();
//        }
//
//        @GraphQlExceptionHandler
//        public GraphQLError handleIllegalState(IllegalStateException ex) {
//            return GraphqlErrorBuilder.newError().message(ex.getMessage()).build();
//        }
//    }
//}
