package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.FakePasswordHasher;
import com.ssangtteongnip.domain.profile.PersonalInfo;
import com.ssangtteongnip.domain.profile.Profile;
import com.ssangtteongnip.domain.profile.ProfileRepository;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.domain.user.UserRepository;
import com.ssangtteongnip.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class ProfileRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired ProfileRepository profileRepository;
    @Autowired EntityManager em;

    private User saveUser() {
        return userRepository.save(User.register("hong@example.com", "홍길동", "password1", new FakePasswordHasher()));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void 프로필의_인적_사항을_저장하고_조회한다() {
        User user = saveUser();
        Profile profile = profileRepository.save(Profile.create(user));
        profile.reviseNameAndInfo("홍길순", PersonalInfo.of("010-1234-5678", LocalDate.of(1998, 4, 27), "서울시",
                "https://github.com/hong", null, null, "OO대", "인턴", "백엔드", "안녕하세요", null));
        flushAndClear();

        Profile found = profileRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(found.getUser().getName()).isEqualTo("홍길순");
        assertThat(found.getInfo().getBirthDate()).isEqualTo(LocalDate.of(1998, 4, 27));
        assertThat(found.getInfo().getDesiredJob()).isEqualTo("백엔드");
    }

    @Test
    void 인적_사항이_모두_비어_있는_프로필도_다시_조회하면_빈_값으로_읽힌다() {
        User user = saveUser();
        profileRepository.save(Profile.create(user));
        flushAndClear();

        Profile found = profileRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(found.getInfo()).isNotNull();
        assertThat(found.getInfo().getPhone()).isNull();
        assertThat(found.getInfo().getBirthDate()).isNull();
    }
}
