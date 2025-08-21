    package com.orangemantra.employeeservice.config;

import com.orangemantra.employeeservice.model.Employee;
import com.orangemantra.employeeservice.repository.EmployeeRepository;
import com.orangemantra.employeeservice.util.HashUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataBackfill {
    private static final Logger log = LoggerFactory.getLogger(DataBackfill.class);
    private final EmployeeRepository repo;

    @PostConstruct
    public void fillEmailHashes() {
        try {
            List<Employee> all = repo.findAll();
            int updated = 0;
            for (Employee e : all) {
                if (e.getEmailHash() == null || e.getEmailHash().isBlank()) {
                    String email = e.getEmail(); // converter decrypts
                    String hash = HashUtil.sha256Hex(email == null ? null : email.trim().toLowerCase());
                    e.setEmailHash(hash);
                    updated++;
                }
            }
            if (updated > 0) repo.saveAll(all);
            if (updated > 0) log.info("Backfilled emailHash for {} employees", updated);
        } catch (Exception ex) {
            log.warn("Employee emailHash backfill failed: {}", ex.getMessage());
        }
    }
}

