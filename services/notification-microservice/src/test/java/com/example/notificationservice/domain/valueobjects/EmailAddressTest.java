package com.example.notificationservice.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailAddressTest {

    @Test
    void shouldCreateValidEmailAddress() {
        EmailAddress email = EmailAddress.of("test@example.com");
        assertNotNull(email);
        assertEquals("test@example.com", email.getValue());
    }

    @Test
    void shouldNormalizeEmailToLowerCase() {
        EmailAddress email = EmailAddress.of("Test@Example.COM");
        assertEquals("test@example.com", email.getValue());
    }

    @Test
    void shouldTrimWhitespace() {
        EmailAddress email = EmailAddress.of("  test@example.com  ");
        assertEquals("test@example.com", email.getValue());
    }

    @Test
    void shouldThrowExceptionForNullEmail() {
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of(null));
    }

    @Test
    void shouldThrowExceptionForBlankEmail() {
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of(""));
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("   "));
    }

    @Test
    void shouldThrowExceptionForInvalidEmailFormat() {
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("invalid-email"));
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("@example.com"));
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("test@"));
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("test@example"));
    }

    @Test
    void shouldAcceptValidEmailFormats() {
        assertDoesNotThrow(() -> EmailAddress.of("user@example.com"));
        assertDoesNotThrow(() -> EmailAddress.of("user.name@example.com"));
        assertDoesNotThrow(() -> EmailAddress.of("user+tag@example.co.uk"));
        assertDoesNotThrow(() -> EmailAddress.of("user_name@example-domain.com"));
    }

    @Test
    void isValidShouldReturnTrueForValidEmail() {
        assertTrue(EmailAddress.isValid("test@example.com"));
        assertTrue(EmailAddress.isValid("user.name@example.com"));
    }

    @Test
    void isValidShouldReturnFalseForInvalidEmail() {
        assertFalse(EmailAddress.isValid(null));
        assertFalse(EmailAddress.isValid(""));
        assertFalse(EmailAddress.isValid("invalid"));
        assertFalse(EmailAddress.isValid("@example.com"));
    }

    @Test
    void shouldBeEqualForSameEmail() {
        EmailAddress email1 = EmailAddress.of("test@example.com");
        EmailAddress email2 = EmailAddress.of("test@example.com");
        assertEquals(email1, email2);
        assertEquals(email1.hashCode(), email2.hashCode());
    }

    @Test
    void shouldBeEqualForSameEmailDifferentCase() {
        EmailAddress email1 = EmailAddress.of("Test@Example.COM");
        EmailAddress email2 = EmailAddress.of("test@example.com");
        assertEquals(email1, email2);
    }

    @Test
    void shouldNotBeEqualForDifferentEmails() {
        EmailAddress email1 = EmailAddress.of("test1@example.com");
        EmailAddress email2 = EmailAddress.of("test2@example.com");
        assertNotEquals(email1, email2);
    }

    @Test
    void toStringShouldReturnEmailValue() {
        EmailAddress email = EmailAddress.of("test@example.com");
        assertEquals("test@example.com", email.toString());
    }
}
