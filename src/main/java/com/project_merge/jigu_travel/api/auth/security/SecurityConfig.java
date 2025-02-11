package com.project_merge.jigu_travel.api.auth.security;

import com.project_merge.jigu_travel.api.auth.security.jwt.JwtAuthenticationFilter;
import com.project_merge.jigu_travel.api.auth.security.jwt.JwtUtil;
import com.project_merge.jigu_travel.api.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserService userService; // UserService 주입

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/home").permitAll()
                        .requestMatchers("/api/user/check-nickname").permitAll()
                        .requestMatchers("/api/user/check-loginId").permitAll()
                        .requestMatchers("/api/user/admin/delete/**").hasRole("ADMIN")
                        .requestMatchers("/api/user/admin/restore/**").hasRole("ADMIN")
                        .requestMatchers("/api/user/admin/stats/today").hasRole("ADMIN")
                        .requestMatchers("/api/user/set-admin").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/user/delete/**").authenticated()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/auth/login", "/auth/register").permitAll()
                        .requestMatchers("/login", "/register").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/static/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/user-info").permitAll()
                        .requestMatchers("/board/list").permitAll()
                        .requestMatchers("/api/board/**").permitAll()
                        .requestMatchers("/board/**").authenticated() //
                        .requestMatchers("/ws/**", "/stomp-ws/**").permitAll()
                        .requestMatchers("/pub/**", "/sub/**").permitAll()
                        .requestMatchers("/place/delete/**").hasRole("ADMIN")
                        .requestMatchers("/place/permanent-delete/**").hasRole("ADMIN")
                        .requestMatchers("/place/deleted/**").hasRole("ADMIN")
                        .requestMatchers("/place/update/**").hasRole("ADMIN")
                        .requestMatchers("/api/user/set-admin").hasRole("ADMIN")
                        .requestMatchers("/location/**", "/place/**").permitAll()
                        .requestMatchers("/ai-guide/**").permitAll()
                        .requestMatchers("/api/ai-guide/**").permitAll()
                        .requestMatchers("/places/upload").permitAll()
                        .requestMatchers("/api/image/**").permitAll()
                        .requestMatchers("/visitor/**").permitAll()
                        .requestMatchers("/visitor/records").permitAll()
                        .requestMatchers("/visitor/count").permitAll()
                        .requestMatchers("/api/ai/ai_classification/exists").authenticated()



//                         .requestMatchers("/admin/**").hasRole("ADMIN")

//                        .requestMatchers("**").permitAll()

                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            System.out.println("인증 실패 - 401 Unauthorized 반환");
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userService), UsernamePasswordAuthenticationFilter.class) // UserService 추가
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/home")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://127.0.0.1:8080", "http://localhost:8080", "https://jiangxy.github.io","http://127.0.0.1:8000", "http://localhost:8000",
                "http://127.0.0.1:5173", "http://localhost:5173", "http://13.209.3.228:8080", "https://13.209.3.228:8080","http://127.0.0.1:4173", "http://localhost:4173", "http://localhost:5174",
                "http://jigu-travel.kro.kr", "https://jigu-travel.kro.kr", "http://15.164.4.29:5173", "https://15.164.4.29:5173", "http://15.164.4.29:4173", "https://15.164.4.29:4173", "http://192.168.56.1:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Sec-WebSocket-Accept",
                "Sec-WebSocket-Key",
                "Sec-WebSocket-Version",
                "Sec-WebSocket-Protocol"
        ));
        
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
