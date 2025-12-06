package com.example.gatewayservice;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Custom AuthenticationEntryPoint để cho phép anonymous access cho public
 * endpoints
 */
public class PublicEndpointAwareAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final AuthenticationEntryPoint delegate;

    public PublicEndpointAwareAuthenticationEntryPoint(AuthenticationEntryPoint delegate) {
        this.delegate = delegate;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        System.out.println("DEBUG ENTRYPOINT: uri=" + uri + ", method=" + method);

        // Check nếu đây là public endpoint
        if (isPublicEndpoint(uri, method)) {
            System.out.println("DEBUG ENTRYPOINT: MATCHED PUBLIC!");
            // Public endpoint - không cần authentication
            // Set anonymous authentication để request có thể tiếp tục
            // Không throw exception
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        System.out.println("DEBUG ENTRYPOINT: NOT MATCHED!");
        // Protected endpoint - delegate đến entry point gốc để trả về 401
        delegate.commence(request, response, authException);
    }

    private boolean isPublicEndpoint(String uri, String method) {
        // Allow all OPTIONS requests (CORS preflight)
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
        if ("GET".equals(method) && uri.startsWith("/api/v1/products")) {
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
