package com.guang.miniecommercebackend.security;

import com.guang.miniecommercebackend.entity.User;
import com.guang.miniecommercebackend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isValid(token)) {
                Claims claims = jwtService.parseClaims(token);
                String username = claims.getSubject();
                List<String> roles = claims.get("roles", List.class);
                if (username != null && roles != null && !roles.isEmpty()) {
                    // Reject immediately if the user has been banned since this token was issued
                    User user = userRepository.findByUsername(username).orElse(null);
                    if (user == null || user.getStatus() == User.UserStatus.BANNED) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"message\":\"your account has been banned\"}");
                        return;
                    }
                    // Build authorities from JWT roles
                    List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>(
                            roles.stream().map(SimpleGrantedAuthority::new).toList());
                    // ROLE_ADMIN only for users with the original ROLE_ADMIN database role (full superadmin)
                    boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getRoleName()));
                    // ROLE_ADMIN_PANEL + permission codes for custom admin roles (isAdminRole=true but not ROLE_ADMIN)
                    boolean isCustomAdmin = !isSuperAdmin && user.getRoles().stream().anyMatch(r -> r.isAdminRole());
                    if (isSuperAdmin && authorities.stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }
                    if (isCustomAdmin) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN_PANEL"));
                        user.getRoles().forEach(r ->
                            r.getPermissions().forEach(p ->
                                authorities.add(new SimpleGrantedAuthority(p.getPermissionCode()))
                            )
                        );
                    }
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        chain.doFilter(request, response);
    }
}
