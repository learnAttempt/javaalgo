package com.example.minicom.controller;

import com.example.minicom.model.Conversation;
import com.example.minicom.model.User;
import com.example.minicom.repo.ConversationRepository;
import com.example.minicom.repo.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationRepository convRepo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messaging;

    public ConversationController(ConversationRepository convRepo, UserRepository userRepo, SimpMessagingTemplate messaging) {
        this.convRepo = convRepo;
        this.userRepo = userRepo;
        this.messaging = messaging;
    }

    // -------------------------------------------------------
    // CREATE
    // -------------------------------------------------------
    @PostMapping
    public ResponseEntity<Conversation> createConversation(@RequestBody Map<String,Object> req) {

        Conversation c = new Conversation();
        c.setSubject((String) req.get("subject"));

        Object uid = req.get("userId");
        if (uid != null) {
            Long userId = Long.valueOf(uid.toString());
            User u = userRepo.findById(userId).orElseThrow();
            c.setUser(u);
        }

        Conversation saved = convRepo.save(c);

        // Always fetch a complete, fresh conversation
        Conversation refreshed = convRepo.findById(saved.getId())
                .orElseThrow();

        // Broadcast to inbox
        messaging.convertAndSend("/topic/conversations", refreshed);

        return ResponseEntity.created(URI.create("/api/conversations/" + saved.getId()))
                .body(refreshed);
    }

    // -------------------------------------------------------
    // LIST (PAGINATED)
    // -------------------------------------------------------
    @GetMapping
    public Page<Conversation> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return convRepo.findByDeletedFalse(pageable);
    }

    // -------------------------------------------------------
    // GET ONE
    // -------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<Conversation> get(@PathVariable Long id) {
        return convRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // -------------------------------------------------------
    // DELETE (SOFT)
    // -------------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        return convRepo.findById(id).map(c -> {

            c.setDeleted(true);
            convRepo.save(c);

            Conversation refreshed = convRepo.findById(c.getId()).orElseThrow();

            // Broadcast updated list
            messaging.convertAndSend("/topic/conversations", refreshed);

            return ResponseEntity.noContent().build();

        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
