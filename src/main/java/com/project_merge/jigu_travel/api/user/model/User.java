package com.project_merge.jigu_travel.api.user.model;

import com.project_merge.jigu_travel.api.auth.model.Auth;
import com.project_merge.jigu_travel.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID userId;

    @Column(nullable = false, unique = true, length = 30)
    private String loginId;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(nullable = false)
    private String birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Location location;

    // role을 변경하면 isAdmin도 자동으로 반영됨
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    @Column(nullable = false)
    private boolean deleted = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Auth> authList = new ArrayList<>();

    // DB에 저장되지 않도록 @Transient 추가
    @Transient
    public boolean getIsAdmin() {
        return this.role == Role.ROLE_ADMIN;
    }

    //이메일 추가
    @Column(nullable = true)
    private String email;

}
