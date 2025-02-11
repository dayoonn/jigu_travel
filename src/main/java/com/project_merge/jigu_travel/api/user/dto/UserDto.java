package com.project_merge.jigu_travel.api.user.dto;

import com.project_merge.jigu_travel.api.user.model.Gender;
import com.project_merge.jigu_travel.api.user.model.Location;
import com.project_merge.jigu_travel.api.user.model.Role;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private UUID userId;
    private String loginId;
    private String nickname;
    private String birthDate;
    private Gender gender;
    private Location location;
    private Role role;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    public boolean getIsAdmin() {
        return this.role == Role.ROLE_ADMIN;
    }
}
