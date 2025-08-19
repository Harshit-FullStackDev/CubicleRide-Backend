package com.orangemantra.rideservice.service;

import com.orangemantra.rideservice.dto.ChatDtos;
import com.orangemantra.rideservice.model.ChatMessage;
import com.orangemantra.rideservice.model.Ride;
import com.orangemantra.rideservice.repository.ChatMessageRepository;
import com.orangemantra.rideservice.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatRepo;
    private final RideRepository rideRepo;
    private final RestTemplate restTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatDtos.MessageDTO sendMessage(String fromEmpId, ChatDtos.SendRequest req) {
        Ride ride = rideRepo.findWithJoinedEmpIdsById(req.getRideId()).orElseThrow(() -> new RuntimeException("Ride not found"));
        if (!"Active".equalsIgnoreCase(ride.getStatus())) throw new RuntimeException("Ride not active; chat closed");
        if (!isOwnerPassengerPair(ride, fromEmpId, req.getToEmpId())) {
            log.warn("Chat denied: rideId={}, owner={}, joinedCount={}, from={}, to={}",
                    ride.getId(), ride.getOwnerEmpId(), ride.getJoinedEmpIds() == null ? 0 : ride.getJoinedEmpIds().size(), fromEmpId, req.getToEmpId());
            throw new RuntimeException("Chat allowed only between owner and joined passenger");
        }

        ChatMessage saved = chatRepo.save(ChatMessage.builder()
                .rideId(req.getRideId())
                .fromEmpId(fromEmpId)
                .toEmpId(req.getToEmpId())
                .content(req.getContent())
                .ts(Instant.now())
                .readFlag(false)
                .build());

        ChatDtos.MessageDTO dto = toDto(saved);
        // send to recipient and echo back to sender
        messagingTemplate.convertAndSendToUser(req.getToEmpId(), "/topic/chat", dto);
        messagingTemplate.convertAndSendToUser(fromEmpId, "/topic/chat", dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ChatDtos.ConversationDTO> listConversations(String me) {
        List<ChatMessageRepository.ConversationRow> rows = chatRepo.listConversations(me);
        if (rows.isEmpty()) return List.of();
        Map<String, String> nameCache = new HashMap<>();
        List<ChatDtos.ConversationDTO> result = new ArrayList<>(rows.size());
        HttpHeaders headers = authHeaders();
        for (ChatMessageRepository.ConversationRow r : rows) {
            String other = r.getOtherEmpId();
            String otherName = nameCache.computeIfAbsent(other, id -> fetchName(id, headers));
            // fetch latest message preview efficiently
            List<ChatMessage> top = chatRepo.findConversation(r.getRideId(), me, other, PageRequest.of(0, 1));
            String preview = top.isEmpty() ? "" : top.get(0).getContent();
            result.add(ChatDtos.ConversationDTO.builder()
                    .rideId(r.getRideId())
                    .otherEmpId(other)
                    .otherName(otherName)
                    .unread(Optional.ofNullable(r.getUnread()).orElse(0L))
                    .lastTs(r.getLastTs())
                    .lastPreview(preview)
                    .build());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ChatDtos.MessageDTO> listMessages(String me, Long rideId, String otherEmpId, int page, int size) {
        Ride ride = rideRepo.findWithJoinedEmpIdsById(rideId).orElseThrow(() -> new RuntimeException("Ride not found"));
        if (!isOwnerPassengerPair(ride, me, otherEmpId)) throw new RuntimeException("Not a valid chat pair for this ride");
        List<ChatMessage> msgs = chatRepo.findConversation(rideId, me, otherEmpId, PageRequest.of(page, size));
        // return chronological (oldest first)
        Collections.reverse(msgs);
        return msgs.stream().map(this::toDto).toList();
    }

    public void markRead(String me, ChatDtos.ReadRequest req) {
        Instant upTo = Optional.ofNullable(req.getUpTo()).orElse(Instant.now());
        int n = chatRepo.markReadUpTo(req.getRideId(), me, req.getOtherEmpId(), upTo);
        if (n > 0) {
            Map<String, Object> evt = new HashMap<>();
            evt.put("type", "read");
            evt.put("rideId", req.getRideId());
            evt.put("reader", me);
            evt.put("upTo", upTo.toEpochMilli());
            messagingTemplate.convertAndSendToUser(req.getOtherEmpId(), "/topic/chat", evt);
        }
    }

    public void notifyRideClosed(Ride ride) {
        Map<String, Object> evt = Map.of("type", "closed", "rideId", ride.getId());
        // owner
        messagingTemplate.convertAndSendToUser(ride.getOwnerEmpId(), "/topic/chat", evt);
        // joined passengers
        if (ride.getJoinedEmpIds() != null) {
            for (String p : ride.getJoinedEmpIds()) {
                messagingTemplate.convertAndSendToUser(p, "/topic/chat", evt);
            }
        }
    }

    @Transactional
    public void deleteConversation(String requesterEmpId, Long rideId, String otherEmpId) {
        Ride ride = rideRepo.findWithJoinedEmpIdsById(rideId).orElseThrow(() -> new RuntimeException("Ride not found"));
        if (!isOwnerPassengerPair(ride, requesterEmpId, otherEmpId)) {
            log.warn("Delete denied: rideId={}, owner={}, joinedCount={}, requester={}, other={}",
                    ride.getId(), ride.getOwnerEmpId(), ride.getJoinedEmpIds() == null ? 0 : ride.getJoinedEmpIds().size(), requesterEmpId, otherEmpId);
            throw new RuntimeException("Delete allowed only between owner and joined passenger");
        }
        int n = chatRepo.deleteConversationForPair(rideId, requesterEmpId, otherEmpId);
        log.info("Deleted {} chat messages for rideId={}, pair=({}, {})", n, rideId, requesterEmpId, otherEmpId);
        // notify both parties to refresh UI
        Map<String, Object> evt = new HashMap<>();
        evt.put("type", "deleted");
        evt.put("rideId", rideId);
        evt.put("by", requesterEmpId);
        evt.put("other", otherEmpId);
        messagingTemplate.convertAndSendToUser(requesterEmpId, "/topic/chat", evt);
        messagingTemplate.convertAndSendToUser(otherEmpId, "/topic/chat", evt);
    }

    private boolean isOwnerPassengerPair(Ride ride, String a, String b) {
        String owner = norm(ride.getOwnerEmpId());
        Set<String> joined = ride.getJoinedEmpIds() == null ? Collections.emptySet() :
                ride.getJoinedEmpIds().stream().filter(Objects::nonNull).map(this::norm).collect(Collectors.toSet());
        String A = norm(a), B = norm(b);
        return (A.equals(owner) && joined.contains(B)) || (B.equals(owner) && joined.contains(A));
    }

    private String norm(String s) { return s == null ? null : s.trim().toLowerCase(); }

    private ChatDtos.MessageDTO toDto(ChatMessage m) {
        return ChatDtos.MessageDTO.builder()
                .id(m.getId())
                .rideId(m.getRideId())
                .fromEmpId(m.getFromEmpId())
                .toEmpId(m.getToEmpId())
                .content(m.getContent())
                .ts(m.getTs())
                .read(m.isReadFlag())
                .build();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String auth = attrs.getRequest().getHeader("Authorization");
            if (auth != null) headers.set("Authorization", auth);
        }
        return headers;
    }

    private String fetchName(String empId, HttpHeaders headers) {
        try {
            ResponseEntity<String> resp = restTemplate.exchange("http://localhost:8082/employee/" + empId + "/name", HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return Optional.ofNullable(resp.getBody()).orElse("Unknown");
        } catch (Exception e) {
            log.warn("Failed fetching name for {}: {}", empId, e.getMessage());
            return "Unknown";
        }
    }
}
