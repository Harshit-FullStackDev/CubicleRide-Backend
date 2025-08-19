package com.orangemantra.rideservice.dto;
import lombok.*;
import java.time.Instant;


public class ChatDtos {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor @Builder
    public static class SendRequest {
        private Long rideId;
        private String toEmpId;
        private String content;
    }
    @Data
    @Builder @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageDTO {
        private Long id;
        private Long rideId;
        private String fromEmpId;
        private String toEmpId;
        private String content;
        private Instant ts;
        private boolean read;
    }
    @Data
    @Builder @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationDTO {
        private Long rideId;
        private String otherEmpId;
        private String otherName;
        private Long unread;
        private Instant lastTs;
        private String lastPreview;
    }
    @Data
    @Builder @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadRequest {
        private Long rideId;
        private String otherEmpId;
        private Instant upTo;
    }
}

