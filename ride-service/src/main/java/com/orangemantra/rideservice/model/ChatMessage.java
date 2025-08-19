package com.orangemantra.rideservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_ride_participants", columnList = "rideId,fromEmpId,toEmpId,ts"),
        @Index(name = "idx_chat_to_user_unread", columnList = "toEmpId,readFlag")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rideId;

    private String fromEmpId;

    private String toEmpId;

    @Column(length = 2000)
    private String content;

    private Instant ts;

    @Builder.Default
    private boolean readFlag = false;
}

