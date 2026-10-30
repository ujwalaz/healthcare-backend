package com.healthcare.repository;

import com.healthcare.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByMobileNumber(String mobileNumber);

    Optional<AdminUser> findByEmail(String email);

    boolean existsByEmail(String email);

    List<AdminUser> findByHospitalId(Long hospitalId);
}