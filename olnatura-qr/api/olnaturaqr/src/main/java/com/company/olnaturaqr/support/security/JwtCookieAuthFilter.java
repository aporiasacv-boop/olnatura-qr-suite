package com.company.olnaturaqr.support.security;

import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.config.AuthCookieProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JwtCookieAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AuthCookieProperties cookieProps;

    public JwtCookieAuthFilter(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            AuthCookieProperties cookieProps
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.cookieProps = cookieProps;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            var existing = SecurityContextHolder.getContext().getAuthentication();
            if (existing != null && existing.isAuthenticated()) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = extractJwtFromCookie(request, cookieProps.name()).orElse(null);
            if (token == null || token.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtTokenProvider.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = jwtTokenProvider.getUserId(token);
            if (userId == null) {
                filterChain.doFilter(request, response);
                return;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isEnabled() || user.getRole() == null) {
                filterChain.doFilter(request, response);
                return;
            }

            String roleName = user.getRole().getName(); // ADMIN / ALMACEN / INSPECCION

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleName));

            var principal = new AuthPrincipal(
                    user.getId(),
                    user.getUsername(),
                    List.of(roleName)
            );

            var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private static Optional<String> extractJwtFromCookie(HttpServletRequest request, String cookieName) {
        if (cookieName == null || cookieName.isBlank()) return Optional.empty();

        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) return Optional.empty();

        return Arrays.stream(cookies)
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }
}