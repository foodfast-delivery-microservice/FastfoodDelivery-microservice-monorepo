package com.example.gatewayservice;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TokenStrippingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        if (isPublicEndpoint(uri, method)) {
            System.out.println("DEBUG STRIPPING FILTER: Stripping token for " + uri);
            HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getHeader(String name) {
                    if ("Authorization".equalsIgnoreCase(name)) {
                        return null;
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    List<String> names = Collections.list(super.getHeaderNames());
                    names.removeIf(name -> "Authorization".equalsIgnoreCase(name));
                    return Collections.enumeration(names);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("Authorization".equalsIgnoreCase(name)) {
                        return Collections.emptyEnumeration();
                    }
                    return super.getHeaders(name);
                }
            };
            chain.doFilter(wrapper, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean isPublicEndpoint(String uri, String method) {
        // Allow all OPTIONS requests
        if ("OPTIONS".equals(method)) {
            return true;
        }

        // Public endpoints
        if (uri.startsWith("/api/v1/auth")) {
            return true;
        }

        // GET /api/v1/products/merchants/{merchantId} - public (numeric ID only)
        if ("GET".equals(method) && uri.matches("/api/v1/products/merchants/\\d+")) {
            return true;
        }
        
        // GET /api/v1/products/** - public
        if ("GET".equals(method) && uri.startsWith("/api/v1/products") &&
                !uri.startsWith("/api/v1/products/merchants")) {
            return true;
        }

        // GET /api/v1/restaurants/** - public (nhưng không bao gồm /me/** và /admin/**)
        if ("GET".equals(method) && uri.startsWith("/api/v1/restaurants") &&
                !uri.startsWith("/api/v1/restaurants/me") &&
                !uri.startsWith("/api/v1/restaurants/admin")) {
            return true;
        }

        // GET /api/v1/users/restaurants - public
        if ("GET".equals(method) && uri.equals("/api/v1/users/restaurants")) {
            return true;
        }

        // GET /api/v1/users/{id} - public (numeric ID only)
        if ("GET".equals(method) && uri.matches("/api/v1/users/\\d+")) {
            return true;
        }

        return false;
    }
}
