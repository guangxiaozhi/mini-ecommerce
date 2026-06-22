package com.guang.miniecommercebackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter){
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * SockJS 握手（/ws/info 等）不走 Spring MVC，普通 permitAll 匹配不到。
     * 这里让 /ws/** 完全跳过 HTTP Security 过滤器链。
     * STOMP CONNECT 时仍由 StompAuthChannelInterceptor 校验 JWT。
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/ws/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        return http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(sm ->sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/health", "/api/products", "/api/products/**").permitAll()
                .anyRequest().authenticated())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
