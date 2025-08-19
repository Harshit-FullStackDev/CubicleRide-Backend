package com.orangemantra.authservice.dto;

import com.orangemantra.authservice.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Employee ID cannot be blank")
    private String empId;

    @NotBlank(message= "Name cannot be blank")
    private String name;

    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    private String password;

    private Role role;
}