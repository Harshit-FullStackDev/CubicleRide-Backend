package com.orangemantra.authservice.service;

import com.orangemantra.authservice.dto.*;
import com.orangemantra.authservice.feign.EmployeeClient;
import com.orangemantra.authservice.dto.NameUpdateRequest;
import com.orangemantra.authservice.model.User;
import com.orangemantra.authservice.model.Role;
import com.orangemantra.authservice.repository.UserRepository;
import com.orangemantra.authservice.util.JwtUtil;
import com.orangemantra.authservice.dto.EmployeeRegisterRequest;
import com.orangemantra.authservice.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mail.MailException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmployeeClient employeeClient;
    private final JavaMailSender mailSender;

    public void register(RegisterRequest req) {
        String emailLower = req.getEmail() == null ? null : req.getEmail().trim().toLowerCase();
        String emailHash = HashUtil.sha256Hex(emailLower);
        if (userRepository.existsByEmpId(req.getEmpId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee already registered (emp_id).");
        }
        if (emailHash != null && userRepository.existsByEmailHash(emailHash)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee already registered (email).");
        }

        String otp = String.valueOf((int)((Math.random() * 900000) + 100000)); // 6-digit OTP
        User user = User.builder()
                .empId(req.getEmpId())
                .name(req.getName())
                .email(req.getEmail()) // encrypted via JPA converter
                .emailHash(emailHash)
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.EMPLOYEE)
                .isVerified(false)
                .otp(otp)
                .build();
        userRepository.save(user);
        EmployeeRegisterRequest emp = new EmployeeRegisterRequest();
        emp.setEmpId(user.getEmpId());
        emp.setName(user.getName());
        emp.setEmail(req.getEmail()); // send plaintext to employee-service; it will encrypt/hash internally
        employeeClient.saveEmployee(emp);
   
        sendOtpEmail(req.getEmail(), otp);
    }

    private void sendOtpEmail(String email, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("OTP for Email Verification OrangeMantra Carpool");
            helper.setText("Your OTP is: " + otp + "\nPlease enter this OTP to verify your email.", false);
            mailSender.send(message);
        } catch (Exception e) { // catch MailAuthenticationException and others
            System.out.println("Failed to send OTP email: " + e.getMessage());
        }
    }

    public boolean verifyOtp(String email, String otp) {
        String hash = HashUtil.sha256Hex(email == null ? null : email.trim().toLowerCase());
        User user = (hash == null) ? null : userRepository.findByEmailHash(hash).orElse(null);
        if (user != null && user.getOtp() != null && user.getOtp().equals(otp)) {
            user.setVerified(true);
            user.setOtp(null); // clear OTP after verification
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Resend OTP to the given (unverified) email. Silently no-op if user not found or already verified.
     */
    public void resendOtp(String email) {
        String hash = HashUtil.sha256Hex(email == null ? null : email.trim().toLowerCase());
        if (hash == null) return;
        userRepository.findByEmailHash(hash).ifPresent(user -> {
            if (user.isVerified()) return; // already verified – don't resend
            String otp = String.valueOf((int)((Math.random() * 900000) + 100000));
            user.setOtp(otp);
            userRepository.save(user);
            sendOtpEmail(email, otp);
        });
    }

    public AuthResponse login(AuthRequest req) {
        String hash = HashUtil.sha256Hex(req.getEmail() == null ? null : req.getEmail().trim().toLowerCase());
        User user = (hash == null) ? null : userRepository.findByEmailHash(hash)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!user.isVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email not verified. Please verify your email before logging in.");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
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
        if (employeeRequest.getEmail() != null && !employeeRequest.getEmail().isBlank()) {
            String emailLower = employeeRequest.getEmail().trim().toLowerCase();
            user.setEmail(employeeRequest.getEmail());
            user.setEmailHash(HashUtil.sha256Hex(emailLower));
        }
        userRepository.save(user);
    }

    // Update only the name (used when employee profile name changes). Email remains OTP-verified and immutable.
    public void updateUserName(String empId, NameUpdateRequest req) {
        if (req == null) return;
        String name = req.getName();
        if (name == null || name.isBlank()) return;
        User user = userRepository.findByEmpId(empId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (!name.equals(user.getName())) {
            user.setName(name);
            userRepository.save(user);
        }
    }

    // --- Admin OTP utilities ---
    public void adminVerifyOtp(String empId) {
        User user = userRepository.findByEmpId(empId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setVerified(true);
        user.setOtp(null);
        userRepository.save(user);
    }

    public void adminResendOtp(String empId) {
        User user = userRepository.findByEmpId(empId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (user.isVerified()) return; // already verified
        String otp = String.valueOf((int)((Math.random() * 900000) + 100000));
        user.setOtp(otp);
        userRepository.save(user);
        sendOtpEmail(user.getEmail(), otp);
    }
}
