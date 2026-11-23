package com.wbw.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbw.auth.GoogleOidcUserService;
import com.wbw.auth.SpaCsrfTokenRequestHandler;
import com.wbw.common.ErrorCode;
import com.wbw.config.swagger.SwaggerLoginController;
import com.wbw.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final RequestMatcher API = request -> pathOf(request).startsWith("/api/");

    private final AppProperties props;
    private final ObjectMapper objectMapper;

    public SecurityConfig(AppProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, GoogleOidcUserService oidcUserService) throws Exception {
        // 로그인 성공 시 프론트엔드로 이동. Swagger에서 로그인을 시작했다면 Swagger로 돌아간다.
        AuthenticationSuccessHandler success = (request, response, authentication) -> {
            HttpSession session = request.getSession(false);
            String target = props.frontendBaseUrl() + "/";
            if (session != null && Boolean.TRUE.equals(session.getAttribute(SwaggerLoginController.RETURN_TO_SWAGGER))) {
                session.removeAttribute(SwaggerLoginController.RETURN_TO_SWAGGER);
                target = request.getContextPath() + "/swagger-ui.html";
            }
            response.sendRedirect(target);
        };
        SimpleUrlAuthenticationFailureHandler failure =
                new SimpleUrlAuthenticationFailureHandler(props.frontendBaseUrl() + "/login?error=oauth");

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**", "/api/csrf", "/api/fonts").permitAll()
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-login").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .anyRequest().denyAll())
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(u -> u.oidcUserService(oidcUserService))
                        .successHandler(success)
                        .failureHandler(failure))
                .exceptionHandling(ex -> ex
                        // API 요청은 로그인 페이지로 리다이렉트하지 않고 401/403 JSON을 반환한다.
                        .defaultAuthenticationEntryPointFor(
                                (req, res, e) -> writeError(res, ErrorCode.UNAUTHORIZED), API)
                        .defaultAccessDeniedHandlerFor(
                                (req, res, e) -> writeError(res, ErrorCode.FORBIDDEN), API))
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                        .deleteCookies("WBW_SESSION"));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = props.cors() == null || props.cors().allowedOrigins() == null
                ? List.of() : props.cors().allowedOrigins().stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-Requested-With"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private void writeError(HttpServletResponse res, ErrorCode code) throws IOException {
        res.setStatus(code.status().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), ErrorResponse.of(code));
    }

    private static String pathOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        return ctx != null && !ctx.isEmpty() && uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;
    }
}
