package com.order.ecommerceshop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter
{

    private final JwtService  jwtService;
    private final CustomUserDetailsService customUserDetailsService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException
    {
        final String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")) // need space with Bearer
        {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try
            {
                String email = jwtService.extractEmail(token);

                if(email != null && SecurityContextHolder.getContext().getAuthentication() == null)
                {
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                    if(jwtService.isTokenValid(token, userDetails.getUsername()))
                    {
                        String role = jwtService.extractRole(token);
                        List<SimpleGrantedAuthority> authorities =  List.of(new SimpleGrantedAuthority("ROLE_"+ role));

                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                }
            }
        catch (Exception ex)
        {
            logger.error("JWT Authentication failed: " + ex.getMessage()); // Token Invalid
        }

        filterChain.doFilter(request, response);

    }
}
