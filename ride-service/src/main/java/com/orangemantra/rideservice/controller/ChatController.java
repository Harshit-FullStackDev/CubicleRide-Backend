package com.orangemantra.rideservice.controller;

import com.orangemantra.rideservice.dto.ChatDtos;
import com.orangemantra.rideservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/ride/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public List<ChatDtos.ConversationDTO> conversations() {
        String me = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        return chatService.listConversations(me);
    }

    @GetMapping("/messages")
    public List<ChatDtos.MessageDTO> messages(@RequestParam Long rideId,
                                              @RequestParam String otherEmpId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        String me = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        return chatService.listMessages(me, rideId, otherEmpId, page, size);
    }

    @MessageMapping("/chat/send")
    @SendToUser("/topic/chat")
    public ChatDtos.MessageDTO send(@Payload ChatDtos.SendRequest req, Principal principal) {
        String me = principal != null ? principal.getName() : SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        return chatService.sendMessage(me, req);
    }

    @MessageMapping("/chat/read")
    public void markRead(@Payload ChatDtos.ReadRequest req, Principal principal) {
        String me = principal != null ? principal.getName() : SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        chatService.markRead(me, req);
    }

    @MessageMapping("/chat/delete")
    public void deleteViaStomp(@Payload ChatDtos.ReadRequest req, Principal principal) {
        // Reuse ReadRequest shape: rideId + otherEmpId fields
        String me = principal != null ? principal.getName() : SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        chatService.deleteConversation(me, req.getRideId(), req.getOtherEmpId());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteViaRest(@RequestParam Long rideId, @RequestParam String otherEmpId) {
        String me = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        chatService.deleteConversation(me, rideId, otherEmpId);
        return ResponseEntity.noContent().build();
    }
}
