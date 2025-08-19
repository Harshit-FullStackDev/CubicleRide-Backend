package com.orangemantra.rideservice.repository;

import com.orangemantra.rideservice.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE m.rideId = :rideId AND ((m.fromEmpId = :a AND m.toEmpId = :b) OR (m.fromEmpId = :b AND m.toEmpId = :a)) ORDER BY m.ts DESC")
    List<ChatMessage> findConversation(@Param("rideId") Long rideId,
                                       @Param("a") String a,
                                       @Param("b") String b,
                                       Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.rideId = :rideId AND ((m.fromEmpId = :a AND m.toEmpId = :b) OR (m.fromEmpId = :b AND m.toEmpId = :a)) ORDER BY m.ts DESC")
    List<ChatMessage> findConversationAll(@Param("rideId") Long rideId,
                                          @Param("a") String a,
                                          @Param("b") String b);

    @Query(value = "SELECT ride_id AS rideId, CASE WHEN from_emp_id = :me THEN to_emp_id ELSE from_emp_id END AS otherEmpId, MAX(ts) AS lastTs, SUM(CASE WHEN to_emp_id = :me AND read_flag = 0 THEN 1 ELSE 0 END) AS unread " +
            "FROM chat_messages WHERE from_emp_id = :me OR to_emp_id = :me GROUP BY ride_id, otherEmpId ORDER BY lastTs DESC", nativeQuery = true)
    List<ConversationRow> listConversations(@Param("me") String me);


    interface ConversationRow {
        Long getRideId();
        String getOtherEmpId();
        Instant getLastTs();
        Long getUnread();
    }


    @Query("SELECT m FROM ChatMessage m WHERE m.rideId = :rideId AND ((m.fromEmpId = :a AND m.toEmpId = :b) OR (m.fromEmpId = :b AND m.toEmpId = :a)) ORDER BY m.ts DESC")
    Optional<ChatMessage> findTopByPair(@Param("rideId") Long rideId,
                                        @Param("a") String a,
                                        @Param("b") String b);

    long countByRideIdAndFromEmpIdAndToEmpIdAndReadFlagFalse(Long rideId, String fromEmpId, String toEmpId);

    int deleteByRideId(Long rideId);

    @Transactional
    @Modifying
    @Query("UPDATE ChatMessage m SET m.readFlag = true WHERE m.rideId = :rideId AND m.toEmpId = :me AND m.fromEmpId = :other AND m.ts <= :upTo")
    int markReadUpTo(@Param("rideId") Long rideId,
                     @Param("me") String me,
                     @Param("other") String other,
                     @Param("upTo") Instant upTo);

    @Transactional
    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.rideId = :rideId AND ((m.fromEmpId = :a AND m.toEmpId = :b) OR (m.fromEmpId = :b AND m.toEmpId = :a))")
    int deleteConversationForPair(@Param("rideId") Long rideId,
                                  @Param("a") String a,
                                  @Param("b") String b);
}
