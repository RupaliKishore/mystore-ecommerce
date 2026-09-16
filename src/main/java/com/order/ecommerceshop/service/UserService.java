package com.order.ecommerceshop.service;

import com.order.ecommerceshop.dto.request.UserRegisterInput;
import com.order.ecommerceshop.dto.request.UserUpdateInput;
import com.order.ecommerceshop.model.User;

import java.util.List;

public interface UserService
{
    List<User> getAllUsers();

    User getuser(Long id);

    User addUser(UserRegisterInput userRegisterInput);

    User updateUser(Long id, UserUpdateInput userUpdateInput);

    boolean deleteUserById(Long id);


    User getUserByEmail(String email);

}
