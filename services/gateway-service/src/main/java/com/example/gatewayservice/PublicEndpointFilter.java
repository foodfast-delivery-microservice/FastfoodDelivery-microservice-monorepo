//package com.example.gatewayservice;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.lang.NonNull;
//import org.springframework.security.authentication.AnonymousAuthenticationToken;
//import org.springframework.security.core.authority.AuthorityUtils;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
///**
// * Filter để set anonymous authentication cho public endpoints
// * Điều này cho phép OAuth2 Resource Server skip JWT validation cho public
// * endpoints
// */
//@Component
//public class PublicEndpointFilter extends OncePerRequestFilter {
//
//    @Override
//    protected void doFilterInternal(@NonNull HttpServletRequest request,
//            @NonNull HttpServletResponse response,
//            @NonNull FilterChain filterChain) throws ServletException, IOException {
//        String uri = request.getRequestURI();
//        String method = request.getMethod();
//
//        // Check nếu đây là public endpoint và chưa có authentication
//        if (isPublicEndpoint(uri, method) &&
//                SecurityContextHolder.getContext().getAuthentication() == null) {
//            // Set anonymous authentication để OAuth2 Resource Server không yêu cầu JWT
//            AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
//                    "public-key",
//                    "anonymousUser",
//                    AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
//            SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
//        }
//
//        filterChain.doFilter(request, response);
//    }
//
//    private boolean isPublicEndpoint(String uri, String method) {
//        // Allow all OPTIONS requests
//        if ("OPTIONS".equals(method)) {
//            return true;
//        }
//
//        // Public endpoints
//        if (uri.startsWith("/api/v1/auth")) {
//            return true;
//        }
//
//        // GET /api/v1/products/merchants/{merchantId} - public (numeric ID only)
//        if ("GET".equals(method) && uri.matches("/api/v1/products/merchants/\\d+")) {
//            return true;
//        }
//
//        // GET /api/v1/products/** - public (nhưng không bao gồm /merchants/**)
//        if ("GET".equals(method) && uri.startsWith("/api/v1/products") &&
//                !uri.startsWith("/api/v1/products/merchants")) {
//            return true;
//        }
//
//        // GET /api/v1/restaurants/** - public (nhưng không bao gồm /me/**)
//        if ("GET".equals(method) && uri.startsWith("/api/v1/restaurants") &&
//                !uri.startsWith("/api/v1/restaurants/me") &&
//                !uri.startsWith("/api/v1/restaurants/admin/all")) {
//            return true;
//        }
//
//        // GET /api/v1/users/restaurants - public
//        if ("GET".equals(method) && uri.equals("/api/v1/users/restaurants")) {
//            return true;
//        }
//
//        // GET /api/v1/users/{id} - public (numeric ID only)
//        if ("GET".equals(method) && uri.matches("/api/v1/users/\\d+")) {
//            return true;
//        }
//
//        return false;
//    }
//}
