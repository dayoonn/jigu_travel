package com.project_merge.jigu_travel.api.user.repository;

import com.project_merge.jigu_travel.api.user.model.User;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByLoginIdAndDeletedFalse(String loginId);
    boolean existsByNickname(String nickname);
    boolean existsByLoginId(String loginId);
    Page<User> findAll(Pageable pageable);
    Optional<User> findByEmailAndDeletedFalse(String email); // 이메일로 사용자 찾기

    // 오늘 가입한 사용자 수 (LocalDateTime으로 변경)
    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // 오늘 탈퇴한 사용자 수 (LocalDateTime으로 변경)
    Long countByDeletedTrueAndUpdatedAtBetween(LocalDateTime start, LocalDateTime end);

    // 탈퇴되지 않은 사용자 조회
    Page<User> findByDeletedFalseAndLoginIdNot(String loginId, Pageable pageable);

    // 탈퇴된 사용자 조회
    Page<User> findByDeletedTrue(Pageable pageable);

    // 탈퇴한지 30일이 지난 사용자 테이블에서 drop
    @Query("SELECT u.userId FROM User u WHERE u.deleted = true AND u.updatedAt < :thresholdDate")
    List<UUID> findOldDeletedUsers(@Param("thresholdDate") LocalDateTime thresholdDate);

}
