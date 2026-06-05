package com.guang.miniecommercebackend.config;

import com.guang.miniecommercebackend.entity.Role;
import com.guang.miniecommercebackend.entity.User;
import com.guang.miniecommercebackend.repository.RoleRepository;
import com.guang.miniecommercebackend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Component
public class UserSampleDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    public UserSampleDataLoader(UserRepository userRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @Override
    public void run(String... args){
        seedRoleIfAbsent("ROLE_USER","Standard customer account");
        seedRoleIfAbsent("ROLE_ADMIN","Full admin access");
        Role userRole = roleRepository.findByRoleName("ROLE_USER").orElseThrow(() -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ROLE_USER missing after seedRoleIfAbsent"
        ));

        if (!userRepository.existsByUsername("demo_user")){
            User user = new User();
            user.setUsername("demo_user");
            String rawUserPassword  = System.getenv("DEFAULT_USER_PASSWORD");
            if (rawUserPassword == null || rawUserPassword.isBlank()) {
                throw new IllegalStateException("DEFAULT_USER_PASSWORD is required");
            }
            user.setPasswordHash(passwordEncoder.encode(rawUserPassword));
            user.setRoles(Set.of(userRole));
            userRepository.save(user);
        }

        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN").orElseThrow(() -> new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ROLE_ADMIN missing after seedRoleIfAbsent"
        ));
        if (!userRepository.existsByUsername("demo_admin")){
            User user = new User();
            user.setUsername("demo_admin");
            String rawAdminPassword  = System.getenv("DEFAULT_ADMIN_PASSWORD");
            if (rawAdminPassword == null || rawAdminPassword.isBlank()) {
                throw new IllegalStateException("DEFAULT_ADMIN_PASSWORD is required");
            }
            user.setPasswordHash(passwordEncoder.encode(rawAdminPassword));
            user.setRoles(Set.of(adminRole));
            userRepository.save(user);
        }
        if (!userRepository.existsByUsername("chat_bot")){
            User user = new User();
            user.setUsername("chat_bot");
            user.setPasswordHash(passwordEncoder.encode("bot-not-used"));
            user.setRoles(Set.of(userRole));
            userRepository.save(user);
        }

    }

    private void seedRoleIfAbsent(String roleName, String description) {
        if (!roleRepository.existsByRoleName(roleName)) {
            Role role = new Role();
            role.setRoleName(roleName);
            role.setDescription(description);
            if ("ROLE_ADMIN".equals(roleName)) {
                role.setAdminRole(true);
            }
            roleRepository.save(role);
        }
    }
}
