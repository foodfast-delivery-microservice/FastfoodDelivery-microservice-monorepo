package com.example.gatewayservice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
public class PublicEndpointBearerTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        System.out.println("DEBUG RESOLVER: uri=" + uri + ", method=" + method);

        if (isPublicEndpoint(uri, method)) {
            System.out.println("DEBUG RESOLVER: IGNORING TOKEN for public endpoint");
            return null; // Ignore token for public endpoints
        }

        return defaultResolver.resolve(request);
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
