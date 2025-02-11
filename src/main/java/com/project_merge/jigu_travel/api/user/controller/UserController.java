package com.project_merge.jigu_travel.api.user.controller;

import com.project_merge.jigu_travel.api.user.dto.UserDto;
import com.project_merge.jigu_travel.api.user.model.Role;
import com.project_merge.jigu_travel.api.user.model.User;
import com.project_merge.jigu_travel.api.user.repository.UserRepository;
import com.project_merge.jigu_travel.exception.CustomException;
import com.project_merge.jigu_travel.exception.ErrorCode;
import com.project_merge.jigu_travel.global.common.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder; // 추가


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<BaseResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "인증되지 않은 사용자", null));
        }

        String loginId = userDetails.getUsername();
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserDto userDto = UserDto.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .nickname(user.getNickname())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .location(user.getLocation())
                .role(user.getRole())
                .build();
        System.out.println("현재 로그인 권한" + user.getRole());

        return ResponseEntity.ok(new BaseResponse<>(200, "사용자 정보 조회 성공", userDto));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<BaseResponse<Boolean>> checkNickname(@RequestParam String nickname) {
        boolean exists = userRepository.existsByNickname(nickname);
        if (exists) {
            return ResponseEntity.ok(new BaseResponse<>(409, "이미 사용 중인 닉네임입니다.", false));
        }
        return ResponseEntity.ok(new BaseResponse<>(200, "사용 가능한 닉네임입니다.", true));
    }

    @GetMapping("/check-loginId")
    public ResponseEntity<BaseResponse<Boolean>> checkLoginId(@RequestParam String loginId) {
        boolean exists = userRepository.existsByLoginId(loginId);
        if (exists) {
            return ResponseEntity.ok(new BaseResponse<>(409, "이미 사용 중인 아이디입니다.", false));
        }
        return ResponseEntity.ok(new BaseResponse<>(200, "사용 가능한 아이디입니다.", true));
    }

    /** 관리자 여부 조회 */
    @GetMapping("/is-admin")
    public ResponseEntity<BaseResponse<Boolean>> checkAdmin(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "인증되지 않은 사용자", false));
        }

        String loginId = userDetails.getUsername();
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return ResponseEntity.ok(new BaseResponse<>(200, "관리자 여부 조회 성공", user.getIsAdmin()));
    }

    /** 전체 사용자 목록 조회 */
    @GetMapping("/all")
    public ResponseEntity<BaseResponse<Page<UserDto>>> getAllActiveUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails // 로그인한 사용자 정보 가져오기
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 현재 로그인한 사용자의 loginId 가져오기
        String currentLoginId = userDetails.getUsername();

        // 현재 로그인한 사용자를 제외한 삭제되지 않은 사용자만 조회
        Page<UserDto> users = userRepository.findByDeletedFalseAndLoginIdNot(currentLoginId, pageable)
                .map(user -> UserDto.builder()
                        .userId(user.getUserId())
                        .loginId(user.getLoginId())
                        .nickname(user.getNickname())
                        .birthDate(user.getBirthDate())
                        .gender(user.getGender())
                        .location(user.getLocation())
                        .role(user.getRole())
                        .createdAt(user.getCreatedAt())
                        .build());

        return ResponseEntity.ok(new BaseResponse<>(200, "일반 사용자 조회 성공", users));
    }

    /** 탈퇴된 사용자 목록 조회 (Soft Delete) */
    @GetMapping("/deleted")
    public ResponseEntity<BaseResponse<Page<UserDto>>> getDeletedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<UserDto> deletedUsers = userRepository.findByDeletedTrue(pageable)
                .map(user -> UserDto.builder()
                        .userId(user.getUserId())
                        .loginId(user.getLoginId())
                        .nickname(user.getNickname())
                        .birthDate(user.getBirthDate())
                        .gender(user.getGender())
                        .location(user.getLocation())
                        .role(user.getRole())
                        .updatedAt(user.getUpdatedAt())
                        .build());

        return ResponseEntity.ok(new BaseResponse<>(200, "탈퇴된 사용자 조회 성공", deletedUsers));
    }


    @PostMapping("/set-admin")
    public ResponseEntity<BaseResponse<String>> setAdmin(@RequestParam UUID userId, @RequestParam String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        try {
            Role newRole = Role.valueOf(role);  // 올바른 ENUM 값인지 체크
            user.setRole(newRole);
            userRepository.save(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new BaseResponse<>(400, "잘못된 role 값입니다.", null));
        }

        String message = role.equals("ROLE_ADMIN") ? "관리자 권한 부여 완료" : "일반 사용자로 변경 완료";
        return ResponseEntity.ok(new BaseResponse<>(200, message, null));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<BaseResponse<String>> deleteMyAccount(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "인증되지 않은 사용자", null));
        }

        String inputPassword = request.get("password");  // 클라이언트에서 보낸 비밀번호
        if (inputPassword == null || inputPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new BaseResponse<>(400, "비밀번호를 입력해주세요.", null));
        }

        User currentUser = userRepository.findByLoginIdAndDeletedFalse(userDetails.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증 (Spring Security 사용 가정)
        if (!passwordEncoder.matches(inputPassword, currentUser.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "비밀번호 입력을 실패하셨습니다.", null));
        }

        // 탈퇴 처리
        currentUser.setDeleted(true);
        userRepository.save(currentUser);

        return ResponseEntity.ok(new BaseResponse<>(200, "회원 탈퇴 완료", null));
    }

    @DeleteMapping("/admin/delete/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<String>> deleteUserByAdmin(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails adminUser) {

        if (adminUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "관리자 인증 실패", null));
        }

        User admin = userRepository.findByLoginIdAndDeletedFalse(adminUser.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!admin.getRole().equals(Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new BaseResponse<>(403, "관리자 권한이 필요합니다.", null));
        }

        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (userToDelete.getUserId().equals(admin.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new BaseResponse<>(403, "관리자는 본인을 삭제할 수 없습니다.", null));
        }

        userToDelete.setDeleted(true);
        userRepository.save(userToDelete);

        return ResponseEntity.ok(new BaseResponse<>(200, "사용자 삭제 완료", null));
    }

    @PutMapping("/admin/restore/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<String>> restoreUserByAdmin(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails adminUser) {

        if (adminUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse<>(401, "관리자 인증 실패", null));
        }

        User admin = userRepository.findByLoginIdAndDeletedFalse(adminUser.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!admin.getRole().equals(Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new BaseResponse<>(403, "관리자 권한이 필요합니다.", null));
        }

        User userToRestore = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!userToRestore.isDeleted()) {
            return ResponseEntity.badRequest()
                    .body(new BaseResponse<>(400, "이미 활성화된 사용자입니다.", null));
        }

        // 탈퇴된 사용자 복구
        userToRestore.setDeleted(false);
        userRepository.save(userToRestore);

        return ResponseEntity.ok(new BaseResponse<>(200, "사용자 복구 완료", null));
    }


    @GetMapping("/admin/stats/today")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Map<String, Long>>> getTodayUserStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay(); // 00:00:00
        LocalDateTime endOfDay = today.atTime(23, 59, 59); // 23:59:59

        long newUsers = userRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        long deletedUsers = userRepository.countByDeletedTrueAndUpdatedAtBetween(startOfDay, endOfDay);

        Map<String, Long> stats = Map.of(
                "newUsers", newUsers,
                "deletedUsers", deletedUsers
        );

        return ResponseEntity.ok(new BaseResponse<>(200, "오늘 가입 & 탈퇴자 수 조회 성공", stats));
    }

}