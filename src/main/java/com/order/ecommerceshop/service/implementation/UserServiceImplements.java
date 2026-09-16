package com.order.ecommerceshop.service.implementation;

import com.order.ecommerceshop.dto.request.UserRegisterInput;
import com.order.ecommerceshop.dto.request.UserUpdateInput;
import com.order.ecommerceshop.exception.DuplicateResourceException;
import com.order.ecommerceshop.exception.ResourceNotFoundException;
import com.order.ecommerceshop.model.User;
import com.order.ecommerceshop.model.UserRole;
import com.order.ecommerceshop.repository.UserRepository;
import com.order.ecommerceshop.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImplements implements UserService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;



    @Override
    public List<User> getAllUsers()
    {
        return this.userRepository.findAll();
    }

    @Override
    public User getuser(Long id) {
        return this.userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Override
    @Transactional
    public User addUser(UserRegisterInput userRegisterInput)
    {
        if(userRepository.existsByEmail(userRegisterInput.getEmail())) throw  new DuplicateResourceException("User", "Email", userRegisterInput.getEmail());

        User user = User.builder()
                .name(userRegisterInput.getName())
                .email(userRegisterInput.getEmail())
                .password(passwordEncoder.encode(userRegisterInput.getPassword()))
                .address(userRegisterInput.getAddress())
                .role(UserRole.USER)
                .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUser(Long id, UserUpdateInput userUpdateInput)
    {
        User existUser = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if(userUpdateInput.getEmail() != null
                &&  !userUpdateInput.getEmail().equals(existUser.getEmail()) &&
                userRepository.existsByEmail(userUpdateInput.getEmail())) throw new DuplicateResourceException("User", "email", userUpdateInput.getEmail());

        if(userUpdateInput.getName() != null) existUser.setName(userUpdateInput.getName());
        if(userUpdateInput.getEmail() != null) existUser.setEmail(userUpdateInput.getEmail());
        if(userUpdateInput.getPassword() != null) existUser.setPassword(passwordEncoder.encode(userUpdateInput.getPassword()));
        if(userUpdateInput.getAddress() != null) existUser.setAddress(userUpdateInput.getAddress());

        return this.userRepository.save(existUser);
    }

    @Override
    @Transactional
    public boolean deleteUserById(Long id)
    {
        if(!userRepository.existsById(id)) throw new ResourceNotFoundException("User", "id", id);
        this.userRepository.deleteById(id);

        return true;
    }

    @Override
    public User getUserByEmail(String email)
    {
        return this.userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User","email", email)    );
    }




}
