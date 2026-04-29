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

/**
 * Spring Security 安全配置。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private JwtFilter jwtFilter;

    /**
     * 注册密码加密器。
     *
     * @return BCrypt 密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 暴露认证管理器。
     *
     * @param authenticationConfiguration 认证配置
     * @return 认证管理器
     * @throws Exception 获取失败时抛出异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 配置鉴权规则和过滤器链。
     *
     * @param http HttpSecurity 配置对象
     * @return 安全过滤器链
     * @throws Exception 配置失败时抛出异常
     */
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
                .antMatchers(org.springframework.http.HttpMethod.GET, "/api/news/**").permitAll()
                .antMatchers("/uploads/**").permitAll()
                .antMatchers(org.springframework.http.HttpMethod.GET, "/api/user/*").permitAll()
                .antMatchers("/api/posts/user/**").permitAll()
                .antMatchers(org.springframework.http.HttpMethod.GET, "/api/ai/config").permitAll()
                .anyRequest().authenticated();

        // 将 JWT 过滤器放到用户名密码过滤器之前，先完成令牌解析。
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
