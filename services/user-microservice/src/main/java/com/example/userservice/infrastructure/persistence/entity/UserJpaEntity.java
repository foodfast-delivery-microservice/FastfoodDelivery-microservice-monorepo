package com.example.userservice.infrastructure.persistence.entity;

import com.example.userservice.domain.entities.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity for User persistence.
 * This is the infrastructure layer representation with JPA annotations.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private User.UserRole role;

    @Column(nullable = false)
    private boolean approved = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column
    private String pendingEmail;

    // Common Profile Fields
    private String fullName;
    private String phone;
    private String address;
    private String avatar;

    // Merchant Profile Fields
    private String restaurantName;
    private String restaurantAddress;
    private String restaurantImage;
    private String openingHours;
}
