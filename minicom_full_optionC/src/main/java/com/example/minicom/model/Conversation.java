package com.example.minicom.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;
    private Instant createdAt = Instant.now();
    private boolean deleted = false;

    @ManyToOne
    private User user;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Message> messages = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }

    @Transient
    public long getAgentUnreadCount() {
        return messages.stream().filter(m -> !m.isReadByAgent()).count();
    }

    @Transient
    public long getUserUnreadCount() {
        return messages.stream().filter(m -> !m.isReadByUser()).count();
    }
}
