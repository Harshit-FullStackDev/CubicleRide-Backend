package com.orangemantra.authservice.service;

import com.orangemantra.authservice.dto.*;
import com.orangemantra.authservice.feign.EmployeeClient;
import com.orangemantra.authservice.model.User;
import com.orangemantra.authservice.model.Role;
import com.orangemantra.authservice.repository.UserRepository;
import com.orangemantra.authservice.util.JwtUtil;
import com.orangemantra.authservice.dto.EmployeeRegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmployeeClient employeeClient;

    public void register(RegisterRequest req) {
        if (userRepository.existsByEmpId(req.getEmpId())) {
            throw new RuntimeException("Employee ID already exists.");
        }
        User user = User.builder()
                .empId(req.getEmpId())
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.EMPLOYEE)
                .build();
        userRepository.save(user);
        EmployeeRegisterRequest emp = new EmployeeRegisterRequest();
        emp.setEmpId(user.getEmpId());
        emp.setName(user.getName());
        emp.setEmail(user.getEmail());
        employeeClient.saveEmployee(emp);
    }

    public AuthResponse login(AuthRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        return new AuthResponse(jwtUtil.generateToken(user));
    }
    public void deleteUserByEmpId(String empId) {
        User user = userRepository.findByEmpId(empId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        userRepository.delete(user);
    }
}
