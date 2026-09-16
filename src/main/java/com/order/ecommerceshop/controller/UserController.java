package com.order.ecommerceshop.controller;

import com.order.ecommerceshop.dto.request.UserRegisterInput;
import com.order.ecommerceshop.dto.request.UserUpdateInput;
import com.order.ecommerceshop.model.User;
import com.order.ecommerceshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserController
{
    private final UserService userService;

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers()
    {
        return this.userService.getAllUsers();
    }

    @QueryMapping
    public User getUser(@Argument Long id)
    {
        return this.userService.getuser(id);
    }

    @QueryMapping
    public User getUserByEmail(@Argument String email)
    {
        return userService.getUserByEmail(email);
    }
//

    @MutationMapping
    public User addUser(@Argument  UserRegisterInput userRegisterInput)
    {
        if(userRegisterInput == null) throw new IllegalStateException(("User input cannot be null "));

        return userService.addUser(userRegisterInput);
    }

    @MutationMapping
    public User updateUser(@Argument Long id, @Argument UserUpdateInput userUpdateInput)
    {
        userService.updateUser(id, userUpdateInput);
        return this.userService.getuser(id);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean deleteUserById(@Argument Long id)
    {
        return userService.deleteUserById(id);

    }


}
