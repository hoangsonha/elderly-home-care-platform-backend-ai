package com.capstone_project.elderly_platform.configurations;

import com.capstone_project.elderly_platform.enums.EnumTokenType;
import com.capstone_project.elderly_platform.exceptions.AuthenticationException;
import io.jsonwebtoken.lang.Strings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenConfiguration jwtTokenConfiguration;
    @Autowired
    private CustomAccountDetailService customAccountDetailService;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    private final List<String> NON_USER = List.of(
            "/swagger-ui/**",
            "/v3/**",
            "/api-docs/**",
            "/swagger-resources/**",
            "/public/**",
            "/actuator/**",
            "/api/v1/public/**",
            "/api/v1/hashtags/non-paging",
            "/api/v1/reviews/top-trending",
            "/api/v1/reviews/search",
            "/api/v1/payments/**",
            "/api/v1/subscriptions/**");

    public String getToken(HttpServletRequest request) {
        try {
            String s = request.getHeader("Authorization");
            if (s.startsWith("Bearer ") && StringUtils.hasText(s)) {
                return s.substring(7);
            }
        } catch (Exception e) {
            throw new AuthenticationException("JWT is expired or invalid: " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            if (request.getServletPath().equals("/")) {
                response.sendRedirect(request.getContextPath() + "/swagger-ui/index.html");
                return;
            }
            if (isAuthentication(request.getRequestURI())) {
                filterChain.doFilter(request, response);
                return;
            }
            String bearerToken = getToken(request);
            if (Strings.hasText(bearerToken) && jwtTokenConfiguration.validate(bearerToken, EnumTokenType.TOKEN)) {
                String email = jwtTokenConfiguration.getEmailFromJwt(bearerToken, EnumTokenType.TOKEN);
                CustomAccountDetail customAccountDetail = (CustomAccountDetail) customAccountDetailService
                        .loadUserByUsername(email);
                if (customAccountDetail != null) {
                    if (customAccountDetail.getAccessToken() != null
                            && customAccountDetail.getAccessToken().equals(bearerToken)) {
                        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                                customAccountDetail, null, customAccountDetail.getAuthorities());
                        usernamePasswordAuthenticationToken
                                .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    } else {
                        throw new AuthenticationException("You don't have permission");
                    }
                } else {
                    throw new AuthenticationException("You don't have permission");
                }
            } else {
                throw new AuthenticationException("You don't have permission");
            }
        } catch (Exception e) {
            log.error("Fail on set user authentication:{}", e.toString());
            resolver.resolveException(request, response, null, e);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAuthentication(String uri) {
        AntPathMatcher pathcMatcher = new AntPathMatcher();
        return NON_USER.stream().anyMatch(pattern -> pathcMatcher.match(pattern, uri));
    }

}
