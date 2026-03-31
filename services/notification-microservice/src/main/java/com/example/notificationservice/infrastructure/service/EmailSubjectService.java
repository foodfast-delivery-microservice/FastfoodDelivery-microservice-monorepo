package com.example.notificationservice.infrastructure.service;

import com.example.notificationservice.infrastructure.config.EmailTemplateConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for resolving email subjects from templates.
 * Supports variable substitution in subject templates.
 */
@Service
@RequiredArgsConstructor
public class EmailSubjectService {

    private final EmailTemplateConfig templateConfig;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("#\\{([^}]+)\\}");

    /**
     * Gets subject for a template with variable substitution.
     *
     * @param templateName template name (e.g., "payment-success")
     * @param variables    variables for substitution (e.g., {"orderId": "123"})
     * @return resolved subject
     */
    public String getSubject(String templateName, Map<String, Object> variables) {
        String template = templateConfig.getSubject(templateName);
        return substituteVariables(template, variables);
    }

    /**
     * Gets subject for payment success email.
     */
    public String getPaymentSuccessSubject(Long orderId) {
        return getSubject("payment-success", Map.of("orderId", orderId));
    }

    /**
     * Gets subject for payment failed email.
     */
    public String getPaymentFailedSubject(Long orderId) {
        return getSubject("payment-failure", Map.of("orderId", orderId));
    }

    /**
     * Gets subject for payment refunded email.
     */
    public String getPaymentRefundedSubject(Long orderId) {
        return getSubject("payment-refunded", Map.of("orderId", orderId));
    }

    /**
     * Gets subject for order confirmed email.
     */
    public String getOrderConfirmedSubject(String orderCode) {
        return getSubject("order-confirmed", Map.of("orderCode", orderCode));
    }

    /**
     * Substitutes variables in template string.
     * Example: "Order #{orderId}" with {"orderId": "123"} -> "Order 123"
     */
    private String substituteVariables(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
