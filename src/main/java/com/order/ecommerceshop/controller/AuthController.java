package com.order.ecommerceshop.controller;

import com.order.ecommerceshop.dto.request.ForgotPasswordInput;
import com.order.ecommerceshop.dto.request.ResetPasswordInput;
import com.order.ecommerceshop.dto.request.UserRegisterInput;
import com.order.ecommerceshop.dto.response.AuthPayload;
import com.order.ecommerceshop.dto.response.MessagePayload;
import com.order.ecommerceshop.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class AuthController
{
    private final AuthService authService;

    @MutationMapping
    public AuthPayload login(@Argument String email, @Argument String password)
    {
        return authService.login(email, password);
    }

    @MutationMapping
    public AuthPayload register(@Argument @Valid  UserRegisterInput userRegisterInput )
    {
        return authService.register(userRegisterInput);
    }

    @MutationMapping
    public MessagePayload forgotPassword(@Argument @Valid ForgotPasswordInput forgotPasswordInput)
    {
        return authService.forgotPassword(forgotPasswordInput);
    }

    @MutationMapping
    public MessagePayload resetPassword(@Argument ResetPasswordInput resetPasswordInput)
    {
        return authService.resetPassword(resetPasswordInput);
    }

    // admin
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')") // ONLY admin can run
    public MessagePayload makeAdmin(@Argument Long userId)
    {
        return authService.makeAdmin(userId);
    }
}
