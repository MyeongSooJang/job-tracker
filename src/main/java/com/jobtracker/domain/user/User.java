package com.jobtracker.domain.user;

import com.jobtracker.global.exception.BusinessException;
import com.jobtracker.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    public static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private String passwordHash;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public static User register(String email, String name, String rawPassword, PasswordHasher hasher) {
        String normalizedEmail = normalizeEmail(email);
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL);
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        User user = new User();
        user.email = normalizedEmail;
        user.name = name;
        user.passwordHash = hasher.hash(rawPassword);
        return user;
    }

    public void authenticate(String rawPassword, PasswordHasher hasher) {
        if (rawPassword == null || !hasher.matches(rawPassword, passwordHash)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    public void changeName(String name) {
        this.name = name;
    }
}
