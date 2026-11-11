package com.ssangtteongnip.domain.profile;

import com.ssangtteongnip.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Getter(AccessLevel.NONE)
    @Embedded
    private PersonalInfo info;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static Profile create(User user) {
        Profile profile = new Profile();
        profile.user = user;
        profile.info = PersonalInfo.empty();
        return profile;
    }

    // 모든 컬럼이 null 이면 Hibernate 가 임베디드 객체를 null 로 복원하므로 빈 값 객체로 대신한다.
    public PersonalInfo getInfo() {
        return info != null ? info : PersonalInfo.empty();
    }

    public void reviseNameAndInfo(String name, PersonalInfo info) {
        this.user.changeName(name);
        this.info = info;
    }
}
