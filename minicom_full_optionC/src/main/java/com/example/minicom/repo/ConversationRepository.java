package com.example.minicom.repo;

import com.example.minicom.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    //List<Conversation> findByDeletedFalseOrderByCreatedAtDesc();
    Page<Conversation> findByDeletedFalse(Pageable pageable);
}
