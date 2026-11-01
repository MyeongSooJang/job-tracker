package com.jobtracker.domain;

import com.jobtracker.domain.profile.PersonalInfo;
import com.jobtracker.domain.profile.Profile;
import com.jobtracker.domain.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileTest {

    private final User user = User.register("hong@example.com", "홍길동", "password1", new FakePasswordHasher());

    @Test
    void create_빈_인적_사항으로_프로필을_생성한다() {
        Profile profile = Profile.create(user);

        assertThat(profile.getUser()).isSameAs(user);
        assertThat(profile.getInfo().getPhone()).isNull();
        assertThat(profile.getInfo().getIntroduction()).isNull();
    }

    @Test
    void update_이름과_인적_사항을_함께_수정한다() {
        Profile profile = Profile.create(user);
        PersonalInfo info = PersonalInfo.of("010-1234-5678", LocalDate.of(1998, 4, 27), "서울시",
                "https://github.com/hong", null, null, "OO대 컴퓨터공학", "인턴 6개월", "백엔드 개발자", "안녕하세요");

        profile.update("홍길순", info);

        assertThat(user.getName()).isEqualTo("홍길순");
        assertThat(profile.getInfo().getPhone()).isEqualTo("010-1234-5678");
        assertThat(profile.getInfo().getBirthDate()).isEqualTo(LocalDate.of(1998, 4, 27));
        assertThat(profile.getInfo().getDesiredJob()).isEqualTo("백엔드 개발자");
    }
}
