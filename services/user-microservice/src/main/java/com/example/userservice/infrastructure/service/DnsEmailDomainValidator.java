package com.example.userservice.infrastructure.service;

import com.example.userservice.application.service.EmailDomainValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

@Slf4j
@Component
public class DnsEmailDomainValidator implements EmailDomainValidator {

    private final boolean enabled;

    public DnsEmailDomainValidator(@Value("${app.email.dns-check.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isValidDomain(String email) {
        if (!enabled || email == null || email.isBlank()) {
            return true;
        }

        int atIndex = email.lastIndexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            DirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(domain, new String[] { "MX" });
            Attribute attr = attrs.get("MX");
            
            if (attr != null && attr.size() > 0) {
                return true;
            }
            
            // Fallback to A record if MX is missing (some domains use A record for mail)
            attrs = ictx.getAttributes(domain, new String[] { "A" });
            attr = attrs.get("A");
            return attr != null && attr.size() > 0;
            
        } catch (NamingException e) {
            log.warn("DNS check failed for domain: {}. Error: {}", domain, e.getMessage());
            return false;
        }
    }
}
