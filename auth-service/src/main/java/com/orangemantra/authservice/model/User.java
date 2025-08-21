package com.orangemantra.authservice.model;

import com.orangemantra.authservice.util.StringCryptoConverter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(indexes = {
    @Index(name = "idx_user_emp_id", columnList = "emp_id"),
    @Index(name = "idx_user_email_hash", columnList = "email_hash", unique = true)
})
@Getter
@Setter
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "emp_id", unique = true, nullable = false)
    private String empId;
    private String name;

    // Encrypted email
    @Convert(converter = StringCryptoConverter.class)
    @Column(name = "email", nullable = false, length = 512)
    private String email;

    // Deterministic SHA-256(emailLower) for lookups and uniqueness
    @Column(name = "email_hash", unique = true, nullable = false, length = 64)
    private String emailHash;

    private String password;
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.EMPLOYEE;

    @Builder.Default
    private boolean isVerified = false;
    private String otp;

    public User() {}

    public User(Long id, String empId, String name, String email, String emailHash, String password, Role role, boolean isVerified, String otp) {
        this.id = id;
        this.empId = empId;
        this.name = name;
        this.email = email;
        this.emailHash = emailHash;
        this.password = password;
        this.role = role;
        this.isVerified = isVerified;
        this.otp = otp;
    }
}
