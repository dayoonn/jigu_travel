package com.project_merge.jigu_travel.api.user.service;
import com.project_merge.jigu_travel.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCleanupScheduler {
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시 실행
    public void deleteOldUsers() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(30);
        List<UUID> userIds = userRepository.findOldDeletedUsers(thresholdDate);

        if (!userIds.isEmpty()) {
            log.info("30일 이상 지난 탈퇴 사용자 {}명 삭제", userIds.size());
            userRepository.deleteAllById(userIds);
        } else {
            log.info("삭제할 탈퇴 사용자 없음");
        }
    }
}
