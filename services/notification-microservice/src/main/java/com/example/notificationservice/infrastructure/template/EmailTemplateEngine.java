package com.example.notificationservice.infrastructure.template;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Component
public class EmailTemplateEngine {

    private final TemplateEngine templateEngine;

    public EmailTemplateEngine(ApplicationContext applicationContext) {
        this.templateEngine = new org.thymeleaf.spring6.SpringTemplateEngine();
        org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver templateResolver =
                new org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(applicationContext);
        templateResolver.setPrefix("classpath:/templates/email/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        ((org.thymeleaf.spring6.SpringTemplateEngine) this.templateEngine).setTemplateResolver(templateResolver);
    }

    public String render(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }
}
