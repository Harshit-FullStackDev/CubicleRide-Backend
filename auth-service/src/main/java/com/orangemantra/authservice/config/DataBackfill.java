package com.orangemantra.authservice.config;

import com.orangemantra.authservice.model.User;
import com.orangemantra.authservice.repository.UserRepository;
import com.orangemantra.authservice.util.HashUtil;
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
    private final UserRepository repo;

    @PostConstruct
    public void fillEmailHashes() {
        try {
            List<User> all = repo.findAll();
            int updated = 0;
            for (User u : all) {
                if (u.getEmailHash() == null || u.getEmailHash().isBlank()) {
                    String email = u.getEmail(); // JPA converter decrypts
                    String hash = HashUtil.sha256Hex(email == null ? null : email.trim().toLowerCase());
                    u.setEmailHash(hash);
                    updated++;
                }
            }
            if (updated > 0) repo.saveAll(all);
            if (updated > 0) log.info("Backfilled emailHash for {} users", updated);
        } catch (Exception e) {
            log.warn("Email hash backfill failed: {}", e.getMessage());
        }
    }
}

