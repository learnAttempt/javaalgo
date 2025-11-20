package com.example.minicom.repo;

import com.example.minicom.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
   // List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);
   Page<Message> findByConversationId(Long conversationId, Pageable pageable);

}
