package com.example.userservice.infrastructure.persistence.mapper;

import com.example.userservice.domain.entities.User;
import com.example.userservice.infrastructure.persistence.entity.UserJpaEntity;

/**
 * Mapper to convert between User domain entity and UserJpaEntity.
 */
public class UserMapper {

    /**
     * Convert domain entity to JPA entity
     */
    public static UserJpaEntity toJpaEntity(User domainEntity) {
        if (domainEntity == null) {
            return null;
        }

        UserJpaEntity jpaEntity = new UserJpaEntity();
        jpaEntity.setId(domainEntity.getId());
        jpaEntity.setUsername(domainEntity.getUsername());
        jpaEntity.setEmail(domainEntity.getEmail());
        jpaEntity.setPassword(domainEntity.getPassword());
        jpaEntity.setRole(domainEntity.getRole());
        jpaEntity.setApproved(domainEntity.isApproved());
        jpaEntity.setActive(domainEntity.isActive());
        jpaEntity.setFullName(domainEntity.getFullName());
        jpaEntity.setPhone(domainEntity.getPhone());
        jpaEntity.setAddress(domainEntity.getAddress());
        jpaEntity.setAvatar(domainEntity.getAvatar());
        jpaEntity.setRestaurantName(domainEntity.getRestaurantName());
        jpaEntity.setRestaurantAddress(domainEntity.getRestaurantAddress());
        jpaEntity.setRestaurantImage(domainEntity.getRestaurantImage());
        jpaEntity.setOpeningHours(domainEntity.getOpeningHours());

        return jpaEntity;
    }

    /**
     * Convert JPA entity to domain entity
     */
    public static User toDomainEntity(UserJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        User domainEntity = new User();
        domainEntity.setId(jpaEntity.getId());
        domainEntity.setUsername(jpaEntity.getUsername());
        domainEntity.setEmail(jpaEntity.getEmail());
        domainEntity.setPassword(jpaEntity.getPassword());
        domainEntity.setRole(jpaEntity.getRole());
        domainEntity.setApproved(jpaEntity.isApproved());
        domainEntity.setActive(jpaEntity.isActive());
        domainEntity.setFullName(jpaEntity.getFullName());
        domainEntity.setPhone(jpaEntity.getPhone());
        domainEntity.setAddress(jpaEntity.getAddress());
        domainEntity.setAvatar(jpaEntity.getAvatar());
        domainEntity.setRestaurantName(jpaEntity.getRestaurantName());
        domainEntity.setRestaurantAddress(jpaEntity.getRestaurantAddress());
        domainEntity.setRestaurantImage(jpaEntity.getRestaurantImage());
        domainEntity.setOpeningHours(jpaEntity.getOpeningHours());

        return domainEntity;
    }
}
