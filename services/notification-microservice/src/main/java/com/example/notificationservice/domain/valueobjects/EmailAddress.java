package com.example.notificationservice.domain.valueobjects;

import java.util.regex.Pattern;

/**
 * Value object representing an email address with validation.
 * Immutable and validates email format on creation.
 */
public final class EmailAddress {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final String value;

    private EmailAddress(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address cannot be null or blank");
        }
        String trimmedEmail = email.trim().toLowerCase();
        if (!isValid(trimmedEmail)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        this.value = trimmedEmail;
    }

    public static EmailAddress of(String email) {
        return new EmailAddress(email);
    }

    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailAddress that = (EmailAddress) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
