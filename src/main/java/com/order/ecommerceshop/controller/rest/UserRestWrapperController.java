package com.order.ecommerceshop.controller.rest;

import com.order.ecommerceshop.dto.request.UserRegisterInput;
import com.order.ecommerceshop.dto.request.UserUpdateInput;
import com.order.ecommerceshop.model.User;
import com.order.ecommerceshop.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rest/users")
@Tag(name = "User Rest API", description = "call userService for swagger")
@RequiredArgsConstructor

@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@SecurityRequirement(name = "bearerAuth")
public class UserRestWrapperController
{
    private final UserService userService;

    // admin only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get All Users", description = "All User List")
    @ApiResponse(responseCode = "200", description = "Found user")
    public ResponseEntity<List<User>> getAllUsers()
    {
        return ResponseEntity.ok(userService.getAllUsers());
    }


    @GetMapping("/{id}")
    @Operation(summary = "Find user ID", description = "Get user ID")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "found user"), @ApiResponse(responseCode = "404", description = "User not found", content = @Content)})
    public ResponseEntity<User> getUser(@Parameter(description = "User ID") @PathVariable Long id)
    {
        return ResponseEntity.ok(userService.getuser(id));
    }


    @GetMapping("/email/{email}")
    @Operation(summary = "find User by email")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email)
    {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }


    @PostMapping("/register")
    @Operation(summary = "Add new User")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "user added successfully"), @ApiResponse(responseCode = "400", description = "Email already exist")})
    public ResponseEntity<User> addUser(@Valid @RequestBody UserRegisterInput userRegisterInput)
    {
       User createUser =  userService.addUser(userRegisterInput);
       return new  ResponseEntity<>(createUser ,HttpStatus.CREATED);
    }


    @PutMapping("/{id}")
    @Operation(summary = "Edit User")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateInput userUpdateInput)
    {
        return  ResponseEntity.ok(userService.updateUser(id, userUpdateInput));
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete User")
    @ApiResponse(responseCode = "200", description = "User delete successfully")
    public ResponseEntity<Boolean> deleteUser(@PathVariable Long id)
    {
        userService.deleteUserById(id);
        return ResponseEntity.ok(true);
    }

}
