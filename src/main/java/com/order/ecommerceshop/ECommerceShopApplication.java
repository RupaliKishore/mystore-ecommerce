package com.order.ecommerceshop;

import com.order.ecommerceshop.model.User;
import com.order.ecommerceshop.model.UserRole;
import com.order.ecommerceshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
@EnableAsync
public class ECommerceShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ECommerceShopApplication.class, args);
    }


    @Bean
    CommandLineRunner initBootstrapAdmin(UserRepository userRepository,
                                         PasswordEncoder passwordEncoder,
                                        @Value("${app.bootstrap.admin.enabled:true}") boolean enabled,
                                        @Value("${app.bootstrap.admin.email:}") String adminEmail,
                                         @Value("${app.bootstrap.admin.password:}") String adminPassword,
                                         @Value("${app.bootstrap.admin.name:Admin}") String adminName,
                                         @Value("${app.bootstrap.admin.address:}") String adminAddress
                                        )
    {
        return args ->
        {
            if(!enabled) // if bootstrap is desabled
            {
                log.info("Bootstrap admin disabled. Skipping");
                return;
            }

            if(adminEmail == null || adminEmail.isBlank()) // if email is empty then skip
            {
                log.warn("Bootstrap admin email not configured. Skipping");
                return;
            }

            try
            {
              User existUser =  userRepository.findByEmail(adminEmail).orElse(null);

              if(existUser == null) // If not user -  create new user
                {
                  User admin = User.builder()
                          .name(adminName)
                          .email(adminEmail)
                          .password(passwordEncoder.encode(adminPassword))
                          .address(adminAddress)
                          .role(UserRole.ADMIN)
                          .build();
                  userRepository.save(admin);
                  log.info("Bootstrap admin created: {}", adminEmail);
                } else if(existUser.getRole() != UserRole.ADMIN) // change user to admin
              {
                  existUser.setRole(UserRole.ADMIN);
                  userRepository.save(existUser);
                  log.info("User upgrade to admin: {}", adminEmail);
              }
              else
              {
                  log.info("Bootstrap admin already exists: {}", adminEmail);
              }
            }
            catch (Exception e)
            {
                log.error("Bootstrap admin created failed: {}", e.getMessage(), e);
            }


        };
    }


}
