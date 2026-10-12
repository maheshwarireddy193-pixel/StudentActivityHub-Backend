package com.studentactivityhub.config;

import com.studentactivityhub.entity.Role;
import com.studentactivityhub.entity.User;
import com.studentactivityhub.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class FacultyAccountInitializer {

    @Bean
    public CommandLineRunner createFaculty(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            String email = "faculty@studentactivityhub.com";
            String password =
                    System.getenv("STUDENT_HUB_FACULTY_PASSWORD");

            if (password == null || password.isBlank()) {
                System.out.println(
                    "Faculty account not initialized: " +
                    "set STUDENT_HUB_FACULTY_PASSWORD first.");
                return;
            }

            if (userRepository.existsByEmail(email)) {
                System.out.println("Faculty account already exists.");
                return;
            }

            User faculty = new User(
                    "System Faculty",
                    email,
                    passwordEncoder.encode(password),
                    Role.FACULTY
            );

            userRepository.save(faculty);

            System.out.println("Faculty account created successfully.");
        };
    }
}