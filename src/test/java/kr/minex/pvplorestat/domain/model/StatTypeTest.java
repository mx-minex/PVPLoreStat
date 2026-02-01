package kr.minex.pvplorestat.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StatType 열거형 테스트
 */
@DisplayName("StatType 열거형 테스트")
class StatTypeTest {

    @Nested
    @DisplayName("기본 속성 테스트")
    class BasicPropertiesTest {

        @Test
        @DisplayName("모든 스탯 타입이 7개여야 한다")
        void 스탯_타입_개수_테스트() {
            assertEquals(7, StatType.values().length);
        }

        @Test
        @DisplayName("각 스탯 타입이 고유한 configKey를 가져야 한다")
        void 고유_configKey_테스트() {
            StatType[] types = StatType.values();
            for (int i = 0; i < types.length; i++) {
                for (int j = i + 1; j < types.length; j++) {
                    assertNotEquals(types[i].getConfigKey(), types[j].getConfigKey(),
                            types[i] + "와 " + types[j] + "의 configKey가 중복됨");
                }
            }
        }

        @Test
        @DisplayName("각 스탯 타입이 한글 표시명을 가져야 한다")
        void 한글_표시명_테스트() {
            for (StatType type : StatType.values()) {
                assertNotNull(type.getDisplayName());
                assertFalse(type.getDisplayName().isEmpty());
            }
        }

        @Test
        @DisplayName("각 스탯 타입이 영문 표시명을 가져야 한다")
        void 영문_표시명_테스트() {
            for (StatType type : StatType.values()) {
                assertNotNull(type.getDisplayNameEn());
                assertFalse(type.getDisplayNameEn().isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("스탯 타입 검색 테스트")
    class FindStatTypeTest {

        @ParameterizedTest
        @DisplayName("한글 키워드로 스탯 타입을 찾을 수 있어야 한다")
        @CsvSource({
                "공격력, DAMAGE",
                "방어력, DEFENSE",
                "체력, HEALTH",
                "피흡수, LIFESTEAL",
                "치명타 확률, CRIT_CHANCE",
                "치명타 데미지, CRIT_DAMAGE",
                "회피율, DODGE"
        })
        void 한글_키워드_검색_테스트(String keyword, String expectedType) {
            Optional<StatType> result = StatType.findByKeyword(keyword);
            assertTrue(result.isPresent(), keyword + " 키워드로 스탯을 찾을 수 없음");
            assertEquals(StatType.valueOf(expectedType), result.get());
        }

        @ParameterizedTest
        @DisplayName("영문 키워드로 스탯 타입을 찾을 수 있어야 한다")
        @CsvSource({
                "damage, DAMAGE",
                "defense, DEFENSE",
                "health, HEALTH",
                "lifesteal, LIFESTEAL",
                "critchance, CRIT_CHANCE",
                "critdamage, CRIT_DAMAGE",
                "dodge, DODGE"
        })
        void 영문_키워드_검색_테스트(String keyword, String expectedType) {
            Optional<StatType> result = StatType.findByKeyword(keyword);
            assertTrue(result.isPresent(), keyword + " 키워드로 스탯을 찾을 수 없음");
            assertEquals(StatType.valueOf(expectedType), result.get());
        }

        @ParameterizedTest
        @DisplayName("대소문자 구분 없이 검색할 수 있어야 한다")
        @ValueSource(strings = {"DAMAGE", "Damage", "damage", "DaMaGe"})
        void 대소문자_무시_검색_테스트(String keyword) {
            Optional<StatType> result = StatType.findByKeyword(keyword);
            assertTrue(result.isPresent());
            assertEquals(StatType.DAMAGE, result.get());
        }

        @ParameterizedTest
        @DisplayName("존재하지 않는 키워드는 빈 Optional을 반환해야 한다")
        @ValueSource(strings = {"없는스탯", "invalid", "마법력", "speed"})
        void 없는_키워드_검색_테스트(String keyword) {
            Optional<StatType> result = StatType.findByKeyword(keyword);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("null 키워드는 빈 Optional을 반환해야 한다")
        void null_키워드_검색_테스트() {
            Optional<StatType> result = StatType.findByKeyword(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("빈 문자열 키워드는 빈 Optional을 반환해야 한다")
        void 빈_문자열_키워드_검색_테스트() {
            Optional<StatType> result = StatType.findByKeyword("");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("퍼센트 스탯 테스트")
    class PercentStatTest {

        @ParameterizedTest
        @DisplayName("퍼센트로 표시되는 스탯은 isPercent가 true여야 한다")
        @ValueSource(strings = {"LIFESTEAL", "CRIT_CHANCE", "DODGE"})
        void 퍼센트_스탯_테스트(String statName) {
            StatType type = StatType.valueOf(statName);
            assertTrue(type.isPercent(), statName + "은(는) 퍼센트 스탯이어야 함");
        }

        @ParameterizedTest
        @DisplayName("일반 수치 스탯은 isPercent가 false여야 한다")
        @ValueSource(strings = {"DAMAGE", "DEFENSE", "HEALTH", "CRIT_DAMAGE"})
        void 일반_스탯_테스트(String statName) {
            StatType type = StatType.valueOf(statName);
            assertFalse(type.isPercent(), statName + "은(는) 일반 스탯이어야 함");
        }
    }

    @Nested
    @DisplayName("별칭(Alias) 테스트")
    class AliasTest {

        @ParameterizedTest
        @DisplayName("한글 별칭으로도 검색할 수 있어야 한다")
        @CsvSource({
                "치확, CRIT_CHANCE",
                "치뎀, CRIT_DAMAGE",
                "회피, DODGE",
                "흡혈, LIFESTEAL"
        })
        void 한글_별칭_검색_테스트(String alias, String expectedType) {
            Optional<StatType> result = StatType.findByKeyword(alias);
            assertTrue(result.isPresent(), alias + " 별칭으로 스탯을 찾을 수 없음");
            assertEquals(StatType.valueOf(expectedType), result.get());
        }

        @ParameterizedTest
        @DisplayName("영문 축약어로도 검색할 수 있어야 한다")
        @CsvSource({
                "atk, DAMAGE",
                "def, DEFENSE",
                "hp, HEALTH",
                "crit, CRIT_CHANCE"
        })
        void 영문_축약어_검색_테스트(String alias, String expectedType) {
            Optional<StatType> result = StatType.findByKeyword(alias);
            assertTrue(result.isPresent(), alias + " 축약어로 스탯을 찾을 수 없음");
            assertEquals(StatType.valueOf(expectedType), result.get());
        }
    }
}
