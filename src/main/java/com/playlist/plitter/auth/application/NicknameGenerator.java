import com.playlist.plitter.auth.domain.repository.GuestUserRepository;

import java.util.Random;

public class NicknameGenerator {

    private static final String[] ADJECTIVES = {
            "행복한", "사랑스러운", "아름다운", "사나운", "차가운", "그리운",
            "엄청난", "거대한", "슬픈", "건강한", "멋진"
    };

    private static final String[] NOUNS = {
            "사자", "고양이", "금붕어", "돌고래", "펭귄", "가오리",
            "사슴", "토끼"
    };

    private static final Random RANDOM = new Random();

    public static String generate(GuestUserRepository guestUserRepository) {
        String adj = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
        String base = adj + noun;

        if (!guestUserRepository.existsByRandomNicknameStartingWith(base)) {
            return base;
        }

        int number = 1;
        String nickname;
        do {
            nickname = base + "_" + number;
            number++;
        } while (guestUserRepository.existsByRandomNickname(nickname));

        return nickname;
    }
}