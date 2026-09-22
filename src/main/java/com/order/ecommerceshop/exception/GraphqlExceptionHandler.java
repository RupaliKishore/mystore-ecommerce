package com.order.ecommerceshop.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;

@Component
public class GraphqlExceptionHandler extends DataFetcherExceptionResolverAdapter
{

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment environment)
    {
        // error type / message for every exception
        ErrorType errorType = ErrorType.INTERNAL_ERROR;

        if(ex instanceof ResourceNotFoundException)
        {
            errorType = ErrorType.NOT_FOUND;
        }
        else if(ex instanceof InsufficientStockException
                || ex instanceof IllegalArgumentException
                || ex instanceof IllegalStateException
                || ex instanceof DuplicateResourceException
                || ex instanceof CustomException )
        {
            errorType = ErrorType.BAD_REQUEST;
        }
        else if(ex instanceof AccessDeniedException)
        {
            errorType = ErrorType.FORBIDDEN;
        }

        return   GraphqlErrorBuilder.newError()
                .errorType(errorType)
                .message(ex.getMessage() != null  ? ex.getMessage() : "Something went wrong!")
                .path(environment.getExecutionStepInfo().getPath())
                .location(environment.getField().getSourceLocation())
                .build();
    }

}
