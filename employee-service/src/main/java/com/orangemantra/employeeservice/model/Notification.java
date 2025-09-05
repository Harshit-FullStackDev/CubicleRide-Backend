package com.orangemantra.employeeservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Convert;
import lombok.Data;
import com.orangemantra.employeeservice.util.StringCryptoConverter;

@Entity
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    @Convert(converter = StringCryptoConverter.class)
    private String message;
    private boolean read = false;
}