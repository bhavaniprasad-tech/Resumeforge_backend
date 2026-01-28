package com.bhavani.resumeforge.security;

import com.bhavani.resumeforge.document.User;
import com.bhavani.resumeforge.repository.UserRepository;
import com.bhavani.resumeforge.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                String userId = jwtUtil.getUserIdFromToken(token);

                if (userId != null
                        && SecurityContextHolder.getContext().getAuthentication() == null
                        && jwtUtil.validateToken(token)) {

                    User user = userRepository.findById(userId).orElse(null);

                    if (user != null) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user.getId(),                      // principal
                                        null,
                                        Collections.singleton(() -> "ROLE_USER") // authorities
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("JWT authenticated user {}", user.getId());
                    }
                }
            } catch (Exception e) {
                log.error("❌ JWT authentication failed", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
