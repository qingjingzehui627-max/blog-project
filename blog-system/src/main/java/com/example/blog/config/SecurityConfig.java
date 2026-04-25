package com.example.blog.config;

import com.example.blog.utils.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Resource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeRequests()
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/posts").permitAll()
                .antMatchers("/api/posts/**").permitAll()
                .antMatchers(org.springframework.http.HttpMethod.GET, "/api/categories/**").permitAll()
                .antMatchers("/api/comments/**").permitAll()
                .antMatchers("/api/upload/**").permitAll()
                .antMatchers("/api/like/count").permitAll()
                .antMatchers("/uploads/**").permitAll()
                .antMatchers(org.springframework.http.HttpMethod.GET, "/api/user/*").permitAll()
                .antMatchers("/api/posts/user/**").permitAll()
                .antMatchers(org.springframework.http.HttpMethod.GET, "/api/ai/config").permitAll()
                .anyRequest().authenticated();

        // 添加JWT过滤器
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
