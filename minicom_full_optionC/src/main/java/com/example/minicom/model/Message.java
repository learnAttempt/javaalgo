package com.example.minicom.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="message")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    private Conversation conversation;

    private String sender;
    @Lob
    private String body;
    private Instant sentAt = Instant.now();

    @Column(nullable = false)
    private boolean delivered = false;
    @Column(nullable = false)
    private boolean readByUser = false;
    @Column(nullable = false)
    private boolean readByAgent = false;

    public Message() {
        this.delivered = false;
        this.readByAgent = false;
        this.readByUser = false;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public boolean isDelivered() { return delivered; }
    public void setDelivered(boolean delivered) { this.delivered = delivered; }
    public boolean isReadByUser() { return readByUser; }
    public void setReadByUser(boolean readByUser) { this.readByUser = readByUser; }
    public boolean isReadByAgent() { return readByAgent; }
    public void setReadByAgent(boolean readByAgent) { this.readByAgent = readByAgent; }
}
