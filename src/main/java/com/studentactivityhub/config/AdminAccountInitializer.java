package com.studentactivityhub.config;

import com.studentactivityhub.entity.Role;
import com.studentactivityhub.entity.User;
import com.studentactivityhub.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminAccountInitializer {

    @Bean
    public CommandLineRunner createAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            String email = "admin@studentactivityhub.com";
            String password = System.getenv("STUDENT_HUB_ADMIN_PASSWORD");

            if (password == null || password.isBlank()) {
                System.out.println(
                    "Admin account not initialized: " +
                    "set STUDENT_HUB_ADMIN_PASSWORD first.");
                return;
            }

            if (userRepository.existsByEmail(email)) {
                System.out.println("Admin account already exists.");
                return;
            }

            User admin = new User(
                    "System Admin",
                    email,
                    passwordEncoder.encode(password),
                    Role.ADMIN
            );

            userRepository.save(admin);

            System.out.println("Admin account created successfully.");
        };
    }
}