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

    @GetMapping("/health")
    public java.util.Map<String,Object> health(){
        return java.util.Map.of("status","UP","service","auth-service","timestamp",System.currentTimeMillis());
    }

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
    public void deleteUser(@PathVariable("empId") String empId) {
        authService.deleteUserByEmpId(empId);
    }
    @PutMapping("/user/{empId}")
    public ResponseEntity<?> updateUser(@PathVariable("empId") String empId, @RequestBody EmployeeRegisterRequest employeeRequest) {
        authService.updateUser(empId, employeeRequest);
        return ResponseEntity.ok().build();
    }
        @PutMapping("/user/name/{empId}")
        public ResponseEntity<?> updateUserName(@PathVariable("empId") String empId, @RequestBody NameUpdateRequest nameUpdateRequest) {
            authService.updateUserName(empId, nameUpdateRequest);
            return ResponseEntity.ok().build();
        }
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody OtpVerifyRequest request) {
        boolean success = authService.verifyOtp(request.getEmail(), request.getOtp());
        if (success) {
            return ResponseEntity.ok("Email verified successfully. You can now login.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid OTP or email.");
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody OtpVerifyRequest request) {
        authService.resendOtp(request.getEmail());
        return ResponseEntity.ok().build();
    }

    // --- Admin OTP operations ---
    @PostMapping("/admin/otp/verify/{empId}")
    public ResponseEntity<?> adminForceVerify(@PathVariable("empId") String empId) {
        authService.adminVerifyOtp(empId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/otp/resend/{empId}")
    public ResponseEntity<?> adminResendOtp(@PathVariable("empId") String empId) {
        authService.adminResendOtp(empId);
        return ResponseEntity.ok().build();
    }
}
