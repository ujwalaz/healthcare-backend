package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "dob", nullable = false)
    private Date dob;

    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "address")
    private String address;

    @Column(name = "emergency_contact_number")
    private String emergencyContactNumber;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(name = "permanent_illness")
    private String permanentIllness;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}