package com.order.ecommerceshop.controller.rest;

import com.order.ecommerceshop.dto.request.LoginInput;
import com.order.ecommerceshop.dto.response.AuthPayload;
import com.order.ecommerceshop.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rest/auth")
@Tag(name = "Authentication Rest API", description = "Login / Register via Rest")
@RequiredArgsConstructor
public class AuthRestWrapperController
{
    private final AuthService authService;

    // login for get JWT token
    @PostMapping("/login")
    @Operation(summary = "Login - get JWT token", description = "Add email & password get JWT token "+ "Add token in Authorise button 'Bearer <token>")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Login successful"),
                  @ApiResponse(responseCode = "400", description = "Invalid email or password"),
                  @ApiResponse(responseCode = "401", description = "Authentication failed")})
    public ResponseEntity<AuthPayload> login(@Valid @RequestBody LoginInput loginInput)
    {

        AuthPayload authPayload = authService.login(loginInput.getEmail(), loginInput.getPassword());
        return ResponseEntity.ok(authPayload);
    }

}
