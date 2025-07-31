package com.orangemantra.authservice.controller;

import com.orangemantra.authservice.dto.*;
import com.orangemantra.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/user/{empId}")
    public void deleteUser(@PathVariable String empId) {
        authService.deleteUserByEmpId(empId);
    }
    @PutMapping("/user/{empId}")
    public ResponseEntity<?> updateUser(@PathVariable String empId, @RequestBody EmployeeRegisterRequest employeeRequest) {
        authService.updateUser(empId, employeeRequest);
        return ResponseEntity.ok().build();
    }
}
