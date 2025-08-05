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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmployeeClient employeeClient;
    private final JavaMailSender mailSender;

    public void register(RegisterRequest req) {
        if (userRepository.existsByEmpId(req.getEmpId())) {
            throw new RuntimeException("Employee ID already exists.");
        }
        // Generate OTP
        String otp = String.valueOf((int)((Math.random() * 900000) + 100000)); // 6-digit OTP
        User user = User.builder()
                .empId(req.getEmpId())
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.EMPLOYEE)
                .isVerified(false)
                .otp(otp)
                .build();
        userRepository.save(user);
        EmployeeRegisterRequest emp = new EmployeeRegisterRequest();
        emp.setEmpId(user.getEmpId());
        emp.setName(user.getName());
        emp.setEmail(user.getEmail());
        employeeClient.saveEmployee(emp);
        // Send OTP to email (implement sendOtpEmail)
        sendOtpEmail(user.getEmail(), otp);
    }

    private void sendOtpEmail(String email, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("OTP for Email Verification OrangeMantra Carpool");
            helper.setText("Your OTP is: " + otp + "\nPlease enter this OTP to verify your email.", false);
            mailSender.send(message);
        } catch (MessagingException e) {
            // Log error or handle as needed
            System.out.println("Failed to send OTP email: " + e.getMessage());
        }
    }

    public boolean verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && user.getOtp() != null && user.getOtp().equals(otp)) {
            user.setVerified(true);
            user.setOtp(null); // clear OTP after verification
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public AuthResponse login(AuthRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email"));
        if (!user.isVerified()) {
            throw new RuntimeException("Email not verified. Please verify your email before logging in.");
        }
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
    public void updateUser(String empId, EmployeeRegisterRequest employeeRequest) {
        User user = userRepository.findByEmpId(empId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setName(employeeRequest.getName());
        user.setEmail(employeeRequest.getEmail());
        // update other fields if needed
        userRepository.save(user);
    }
}
