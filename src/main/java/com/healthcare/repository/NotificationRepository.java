package com.healthcare.repository;

import com.healthcare.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientUserTypeAndRecipientId(
            String recipientUserType, Long recipientId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.recipientUserType = :userType AND n.recipientId = :recipientId " +
           "AND (:isRead IS NULL OR n.isRead = :isRead)")
    Page<Notification> findByRecipientWithReadFilter(
            @Param("userType") String userType,
            @Param("recipientId") Long recipientId,
            @Param("isRead") Boolean isRead,
            Pageable pageable);
}
