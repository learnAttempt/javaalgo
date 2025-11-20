package com.example.minicom.controller;

import com.example.minicom.model.Conversation;
import com.example.minicom.model.Message;
import com.example.minicom.repo.ConversationRepository;
import com.example.minicom.repo.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;
    private final SimpMessagingTemplate messaging;

    public MessageController(
            ConversationRepository convRepo,
            MessageRepository msgRepo,
            SimpMessagingTemplate messaging
    ) {
        this.convRepo = convRepo;
        this.msgRepo = msgRepo;
        this.messaging = messaging;
    }

    // -------------------------------------------------------
    // ADD MESSAGE
    // -------------------------------------------------------
    @PostMapping("/conversations/{id}")
    public ResponseEntity<Message> addMessage(@PathVariable Long id, @RequestBody Message m) {

        return convRepo.findById(id).map(conv -> {

            // Prepare message
            m.setConversation(conv);
            m.setDelivered(true);
            m.setReadByUser(false);
            m.setReadByAgent(false);

            Message saved = msgRepo.save(m);

            // Always fetch fresh conversation
            Conversation refreshed = convRepo.findById(id).orElseThrow();

            // ─── FIX: Only send these TWO messages ──────────────────────
            messaging.convertAndSend("/topic/conversations", refreshed);
            messaging.convertAndSend("/topic/conversations/" + id, refreshed);
            // ─────────────────────────────────────────────────────────────

            return ResponseEntity.created(URI.create("/api/messages/" + saved.getId())).body(saved);

        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // -------------------------------------------------------
    // LIST MESSAGES (PAGINATED)
    // -------------------------------------------------------
    @GetMapping("/conversations/{id}")
    public Page<Message> listForConversation(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").ascending());
        return msgRepo.findByConversationId(id, pageable);
    }

    // -------------------------------------------------------
    // MARK READ
    // -------------------------------------------------------
    @PostMapping("/{id}/read")
    public ResponseEntity<Message> markRead(@PathVariable Long id, @RequestParam("by") String by) {

        return msgRepo.findById(id).map(m -> {

            if ("agent".equalsIgnoreCase(by)) m.setReadByAgent(true);
            if ("user".equalsIgnoreCase(by)) m.setReadByUser(true);

            Message saved = msgRepo.save(m);

            // Fetch updated conversation
            Conversation refreshed = convRepo.findById(saved.getConversation().getId())
                    .orElseThrow();

            // ─── FIX: Only send these TWO messages ──────────────────────
            messaging.convertAndSend("/topic/conversations", refreshed);
            messaging.convertAndSend("/topic/conversations/" + refreshed.getId(), refreshed);
            // ─────────────────────────────────────────────────────────────

            return ResponseEntity.ok(saved);

        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
