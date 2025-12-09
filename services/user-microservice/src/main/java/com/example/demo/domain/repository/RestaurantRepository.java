package com.example.demo.domain.repository;

import com.example.demo.domain.model.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByMerchantId(Long merchantId);

    List<Restaurant> findByActiveAndApproved(Boolean active, Boolean approved);

    List<Restaurant> findByActiveTrueAndApprovedTrue();
    @Query("SELECT r FROM Restaurant r WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:city IS NULL OR :city = '' OR r.city = :city) " +
            "AND (:category IS NULL OR :category = '' OR r.category = :category) " +
            "AND (:active IS NULL OR r.active = :active)")
    Page<Restaurant> findWithFilters(
            @Param("keyword") String keyword,
            @Param("city") String city,
            @Param("category") String category,
            @Param("active") Boolean active,
            Pageable pageable
    );
}

















