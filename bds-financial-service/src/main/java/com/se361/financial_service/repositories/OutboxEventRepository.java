package com.se361.financial_service.repositories;

import com.se361.financial_service.entities.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Lấy tối đa 50 event chưa xử lý, sắp xếp theo thứ tự tạo (FIFO).
     * Bỏ qua event đã retry quá 5 lần (poison pill).
     */
    @Query("""
            SELECT o FROM OutboxEvent o
            WHERE o.processed = false
              AND o.retryCount < 5
            ORDER BY o.createdAt ASC
            LIMIT 50
            """)
    List<OutboxEvent> findPendingEvents();

    /**
     * Bulk mark processed để giảm số lần UPDATE riêng lẻ.
     */
    @Modifying
    @Query("""
            UPDATE OutboxEvent o
            SET o.processed = true,
                o.processedAt = CURRENT_TIMESTAMP
            WHERE o.id IN :ids
            """)
    void markAsProcessed(@Param("ids") List<UUID> ids);
}
