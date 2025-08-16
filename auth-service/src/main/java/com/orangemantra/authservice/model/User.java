package com.orangemantra.authservice.model;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(indexes = {
    @Index(name = "idx_user_emp_id", columnList = "emp_id"),
    @Index(name = "idx_user_email", columnList = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "emp_id", unique = true, nullable = false)
    private String empId;
    private String name;

    @Column(unique = true, nullable = false)
    private String email;
    private String password;
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.EMPLOYEE;

    @Builder.Default
    private boolean isVerified = false;
    private String otp;
}
