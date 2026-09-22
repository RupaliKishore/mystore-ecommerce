package com.order.ecommerceshop.service;

import com.order.ecommerceshop.dto.request.ForgotPasswordInput;
import com.order.ecommerceshop.dto.request.ResetPasswordInput;
import com.order.ecommerceshop.dto.request.UserRegisterInput;
import com.order.ecommerceshop.dto.response.AuthPayload;
import com.order.ecommerceshop.dto.response.MessagePayload;
import com.order.ecommerceshop.exception.DuplicateResourceException;
import com.order.ecommerceshop.exception.ResourceNotFoundException;
import com.order.ecommerceshop.model.User;
import com.order.ecommerceshop.model.UserRole;
import com.order.ecommerceshop.repository.UserRepository;
import com.order.ecommerceshop.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService
{
    @Value("${app.reset.token.expiry}")
    private long resetTokenExpiryMs;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    // login
    public AuthPayload login(String email, String password)
    {
       User user =  userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Invalid Email or Password"));

       if (!passwordEncoder.matches(password, user.getPassword())) throw new IllegalArgumentException("Invalid password");


       String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

       return new AuthPayload(token, user.getId(), user.getName(),user.getEmail(), user.getRole());
    }


    // register
    @Transactional
    public AuthPayload register(UserRegisterInput userRegisterInput)
    {
        if(userRepository.existsByEmail(userRegisterInput.getEmail())) throw  new DuplicateResourceException("User", "email", userRegisterInput.getEmail());

        User user = User.builder()
                .name(userRegisterInput.getName())
                .email(userRegisterInput.getEmail())
                .password(passwordEncoder.encode(userRegisterInput.getPassword()))
                .address(userRegisterInput.getAddress())
                .role(UserRole.USER)
                .build();

        userRepository.save(user);

        // send welcome email
        emailService.sendWelcomeEmail(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthPayload(token, user.getId(), user.getName(), user.getEmail(), user.getRole());
    }


    // =========  forgot password =============

    @Transactional
    public MessagePayload forgotPassword(ForgotPasswordInput forgotPasswordInput) {
        log.info("🔍 ========== FORGOT PASSWORD REQUEST ==========");
        log.info(" Email: {}", forgotPasswordInput.getEmail());

        User user = userRepository.findByEmail(forgotPasswordInput.getEmail()).orElse(null);

        if (user == null) {
            log.warn(" User NOT found: {}", forgotPasswordInput.getEmail());
            return new MessagePayload("If the email exist, a reset link has been sent", true);
        }

        // create token
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusSeconds(resetTokenExpiryMs / 1000));
        userRepository.save(user);

        log.info(" Token generated: {}", token);
        log.info(" Expiry: {}", user.getResetTokenExpiry());
        log.info(" Token SAVED to database for: {}", user.getEmail());

        // send email
        emailService.sendPasswordResetEmail(user.getEmail(), token);
        log.info(" Email sent to: {}", user.getEmail());
        log.info(" ================================================");

        return new MessagePayload("Password reset link sent to your email.", true);
    }


    // ==========  reset password ============
    @Transactional
    public MessagePayload resetPassword(ResetPasswordInput resetPasswordInput) {
        log.info(" ========== RESET PASSWORD REQUEST ==========");
        log.info(" Token received: {}", resetPasswordInput.getToken());

        User user = userRepository.findByResetToken(resetPasswordInput.getToken())
                .orElseThrow(() -> {
                    log.error("❌ TOKEN NOT FOUND in database!");
                    log.error("❌ Received token: {}", resetPasswordInput.getToken());
                    return new IllegalArgumentException("Invalid or expired Reset token ");
                });

//        log.info(" User found: {}", user.getEmail());
//        log.info(" Token expiry: {}", user.getResetTokenExpiry());
//        log.info(" Current time: {}", LocalDateTime.now());

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            log.error(" TOKEN EXPIRED!");
            throw new IllegalArgumentException("Reset token has expired. Please request a new one");
        }

        // new Password encode
        user.setPassword(passwordEncoder.encode(resetPasswordInput.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        log.info(" PASSWORD RESET SUCCESSFUL for: {}", user.getEmail());
        log.info(" ==========================================");

        return new MessagePayload("Password reset successfully. you can now login with your new password", true);
    }



    // admin panel method

    @Transactional
    public MessagePayload makeAdmin(Long userId)
    {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("user", "id", userId));
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);

        return new MessagePayload("user"+ user.getName() + " is now an ADMIN", true);
    }


}

