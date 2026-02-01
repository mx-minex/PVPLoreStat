package kr.minex.pvplorestat.infrastructure.lore;

import kr.minex.pvplorestat.domain.model.ItemStats;
import kr.minex.pvplorestat.domain.model.StatType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoreManager 테스트
 * <p>
 * 로어 파싱, 생성, 수정 기능을 테스트합니다.
 * config.yml의 lore.format과 정확히 일치하는 형식만 파싱합니다.
 * </p>
 */
@DisplayName("LoreManager 테스트")
class LoreManagerTest {

    private LoreManager loreManager;
    private LoreTemplate template;

    @BeforeEach
    void setUp() {
        template = LoreTemplate.defaultTemplate();
        loreManager = new LoreManager(template);
    }

    @Nested
    @DisplayName("로어 파싱 테스트")
    class ParseTest {

        @Test
        @DisplayName("스탯이 없는 로어는 빈 스탯을 반환해야 한다")
        void 빈_로어_파싱_테스트() {
            List<String> lore = List.of(
                    "일반 설명 텍스트",
                    "아이템 정보"
            );

            ItemStats stats = loreManager.parseLore(lore);

            assertTrue(stats.isEmpty());
        }

        @Test
        @DisplayName("null 로어는 빈 스탯을 반환해야 한다")
        void null_로어_파싱_테스트() {
            ItemStats stats = loreManager.parseLore(null);
            assertTrue(stats.isEmpty());
        }

        @ParameterizedTest
        @DisplayName("config 형식과 일치하는 스탯을 파싱해야 한다")
        @CsvSource({
                "'⚔ 공격력 +50', DAMAGE, 50",
                "'🛡 방어력 +30', DEFENSE, 30",
                "'❤ 체력 +100', HEALTH, 100",
                "'🩸 피흡수 10%', LIFESTEAL, 10",
                "'⚡ 치명타 확률 25%', CRIT_CHANCE, 25",
                "'💥 치명타 데미지 +150', CRIT_DAMAGE, 150",
                "'💨 회피율 15%', DODGE, 15"
        })
        void 기본_형식_파싱_테스트(String loreLine, String expectedType, double expectedValue) {
            List<String> lore = List.of(loreLine);

            ItemStats stats = loreManager.parseLore(lore);

            assertEquals(expectedValue, stats.getStat(StatType.valueOf(expectedType)));
        }

        @ParameterizedTest
        @DisplayName("색상 코드가 포함된 config 형식을 파싱해야 한다")
        @ValueSource(strings = {
                "§c⚔ 공격력 §f+50",
                "&c⚔ 공격력 &f+50",
                "§c§l⚔ 공격력 §f+50"
        })
        void 색상_코드_파싱_테스트(String loreLine) {
            List<String> lore = List.of(loreLine);

            ItemStats stats = loreManager.parseLore(lore);

            assertEquals(50, stats.getStat(StatType.DAMAGE));
        }

        @Test
        @DisplayName("소수점 스탯을 파싱해야 한다")
        void 소수점_파싱_테스트() {
            List<String> lore = List.of("⚔ 공격력 +10.5");

            ItemStats stats = loreManager.parseLore(lore);

            assertEquals(10.5, stats.getStat(StatType.DAMAGE));
        }

        @Test
        @DisplayName("여러 스탯을 동시에 파싱해야 한다")
        void 다중_스탯_파싱_테스트() {
            List<String> lore = List.of(
                    "§c⚔ 공격력 §f+100",
                    "§9🛡 방어력 §f+50",
                    "§6❤ 체력 §f+200",
                    "§4🩸 피흡수 §f10%"
            );

            ItemStats stats = loreManager.parseLore(lore);

            assertEquals(100, stats.getDamage());
            assertEquals(50, stats.getDefense());
            assertEquals(200, stats.getHealth());
            assertEquals(10, stats.getLifesteal());
        }

        @Test
        @DisplayName("같은 스탯이 여러 번 있으면 마지막 값을 사용해야 한다")
        void 중복_스탯_마지막_값_테스트() {
            List<String> lore = List.of(
                    "⚔ 공격력 +50",
                    "⚔ 공격력 +100"  // 마지막 값
            );

            ItemStats stats = loreManager.parseLore(lore);

            assertEquals(100, stats.getDamage());
        }

        @ParameterizedTest
        @DisplayName("config 형식과 일치하지 않는 로어는 무시해야 한다")
        @ValueSource(strings = {
                "공격력 50",           // 아이콘 없음
                "⚔ 공격력 50",        // + 기호 없음
                "공격력 +50",          // 아이콘 없음
                "공격력: 50",          // 다른 형식
                "알 수 없는 스탯 100"   // 없는 스탯
        })
        void config_형식_불일치_무시_테스트(String loreLine) {
            List<String> lore = List.of(loreLine);

            ItemStats stats = loreManager.parseLore(lore);

            assertTrue(stats.isEmpty());
        }

        @ParameterizedTest
        @DisplayName("음수나 유효하지 않은 값은 무시해야 한다")
        @ValueSource(strings = {
                "⚔ 공격력 +-50",       // 음수
                "⚔ 공격력 +abc"        // 숫자가 아님
        })
        void 잘못된_값_무시_테스트(String loreLine) {
            List<String> lore = List.of(loreLine);

            ItemStats stats = loreManager.parseLore(lore);

            assertTrue(stats.isEmpty());
        }

        @Test
        @DisplayName("다른 플러그인의 로어 사이에 있는 config 형식 스탯도 파싱해야 한다")
        void 혼합_로어_파싱_테스트() {
            List<String> lore = List.of(
                    "§8─────────────",
                    "§7등급: §e전설",
                    "§c⚔ 공격력 §f+100",
                    "§9🛡 방어력 §f+50",
                    "§8─────────────",
                    "§6세트 효과: 드래곤"
            );

            ItemStats stats = loreManager.parseLore(lore);

            assertEquals(100, stats.getDamage());
            assertEquals(50, stats.getDefense());
        }
    }

    @Nested
    @DisplayName("로어 생성 테스트")
    class GenerateTest {

        @Test
        @DisplayName("단일 스탯으로 로어를 생성해야 한다")
        void 단일_스탯_로어_생성_테스트() {
            ItemStats stats = ItemStats.builder().damage(100).build();

            List<String> lore = loreManager.generateLore(stats);

            assertFalse(lore.isEmpty());
            // 템플릿에 따라 공격력 포함 확인
            assertTrue(lore.stream().anyMatch(line ->
                    LoreManager.stripColor(line).contains("공격력") &&
                            LoreManager.stripColor(line).contains("100")));
        }

        @Test
        @DisplayName("여러 스탯으로 로어를 생성해야 한다")
        void 다중_스탯_로어_생성_테스트() {
            ItemStats stats = ItemStats.builder()
                    .damage(100)
                    .defense(50)
                    .health(200)
                    .build();

            List<String> lore = loreManager.generateLore(stats);

            // 각 스탯이 포함되어 있는지 확인
            String joinedLore = String.join("\n", lore);
            String stripped = LoreManager.stripColor(joinedLore);

            assertTrue(stripped.contains("공격력"));
            assertTrue(stripped.contains("방어력"));
            assertTrue(stripped.contains("체력"));
        }

        @Test
        @DisplayName("빈 스탯은 빈 로어를 반환해야 한다")
        void 빈_스탯_로어_생성_테스트() {
            ItemStats stats = ItemStats.empty();

            List<String> lore = loreManager.generateLore(stats);

            assertTrue(lore.isEmpty());
        }

        @Test
        @DisplayName("템플릿 순서대로 스탯을 생성해야 한다")
        void 스탯_순서_테스트() {
            ItemStats stats = ItemStats.builder()
                    .dodge(15)      // 마지막
                    .damage(100)    // 첫 번째
                    .defense(50)    // 두 번째
                    .build();

            List<String> lore = loreManager.generateLore(stats);

            // 공격력이 방어력보다 먼저 나와야 함
            int damageIndex = -1;
            int defenseIndex = -1;
            int dodgeIndex = -1;

            for (int i = 0; i < lore.size(); i++) {
                String stripped = LoreManager.stripColor(lore.get(i));
                if (stripped.contains("공격력")) damageIndex = i;
                if (stripped.contains("방어력")) defenseIndex = i;
                if (stripped.contains("회피율")) dodgeIndex = i;
            }

            assertTrue(damageIndex < defenseIndex, "공격력이 방어력보다 먼저 와야 함");
            assertTrue(defenseIndex < dodgeIndex, "방어력이 회피율보다 먼저 와야 함");
        }
    }

    @Nested
    @DisplayName("로어 수정 테스트")
    class ModifyTest {

        @Test
        @DisplayName("기존 로어에 스탯을 추가해야 한다")
        void 스탯_추가_테스트() {
            List<String> existingLore = new ArrayList<>(List.of(
                    "§7일반 아이템 설명"
            ));
            ItemStats newStats = ItemStats.builder().damage(100).build();

            List<String> result = loreManager.addOrUpdateStats(existingLore, newStats, 1);

            // 기존 로어가 유지되어야 함
            assertTrue(result.stream().anyMatch(line ->
                    LoreManager.stripColor(line).contains("일반 아이템 설명")));
            // 새 스탯이 추가되어야 함
            assertTrue(result.stream().anyMatch(line ->
                    LoreManager.stripColor(line).contains("공격력")));
        }

        @Test
        @DisplayName("기존 스탯을 수정해야 한다")
        void 스탯_수정_테스트() {
            // 이미 공격력이 있는 로어 (config 형식)
            List<String> existingLore = new ArrayList<>(List.of(
                    "§c⚔ 공격력 §f+50"
            ));
            ItemStats newStats = ItemStats.builder().damage(100).build();

            List<String> result = loreManager.addOrUpdateStats(existingLore, newStats, 0);

            // 공격력이 100으로 변경되어야 함
            String stripped = LoreManager.stripColor(String.join("\n", result));
            assertTrue(stripped.contains("100"));
            assertFalse(stripped.contains("+50"));
        }

        @Test
        @DisplayName("특정 스탯을 제거해야 한다")
        void 스탯_제거_테스트() {
            List<String> existingLore = new ArrayList<>(List.of(
                    "§c⚔ 공격력 §f+100",
                    "§9🛡 방어력 §f+50"
            ));

            List<String> result = loreManager.removeStat(existingLore, StatType.DAMAGE);

            // 공격력이 제거되어야 함
            String stripped = LoreManager.stripColor(String.join("\n", result));
            assertFalse(stripped.contains("공격력"));
            assertTrue(stripped.contains("방어력"));
        }

        @Test
        @DisplayName("모든 스탯을 제거해야 한다")
        void 모든_스탯_제거_테스트() {
            List<String> existingLore = new ArrayList<>(List.of(
                    "§7일반 설명",
                    "§c⚔ 공격력 §f+100",
                    "§9🛡 방어력 §f+50",
                    "§6다른 설명"
            ));

            List<String> result = loreManager.removeAllStats(existingLore);

            String stripped = LoreManager.stripColor(String.join("\n", result));
            assertFalse(stripped.contains("공격력"));
            assertFalse(stripped.contains("방어력"));
            // 일반 설명은 유지되어야 함
            assertTrue(stripped.contains("일반 설명") || stripped.contains("다른 설명"));
        }

        @Test
        @DisplayName("원하는 위치에 스탯 로어를 삽입해야 한다")
        void 위치_지정_삽입_테스트() {
            List<String> existingLore = new ArrayList<>(List.of(
                    "§7라인 1",
                    "§7라인 2",
                    "§7라인 3"
            ));
            ItemStats stats = ItemStats.builder().damage(100).build();

            // 인덱스 1에 삽입 (라인 1 다음)
            List<String> result = loreManager.addOrUpdateStats(existingLore, stats, 1);

            // 라인 1이 첫 번째여야 함
            assertTrue(LoreManager.stripColor(result.get(0)).contains("라인 1"));
        }

        @Test
        @DisplayName("기존 스탯 위치에서 스탯을 업데이트해야 한다")
        void 기존_위치_업데이트_테스트() {
            List<String> existingLore = new ArrayList<>(List.of(
                    "§7라인 1",
                    "§c⚔ 공격력 §f+50",
                    "§7라인 3"
            ));
            ItemStats stats = ItemStats.builder().damage(100).build();

            List<String> result = loreManager.addOrUpdateStats(existingLore, stats, 0);

            // 라인1, (구분선+스탯+구분선), 라인3 순서 유지 확인
            // 첫 번째와 마지막이 일반 라인이어야 함
            String first = LoreManager.stripColor(result.get(0));
            String last = LoreManager.stripColor(result.get(result.size() - 1));
            assertTrue(first.contains("라인 1"));
            assertTrue(last.contains("라인 3"));
        }
    }

    @Nested
    @DisplayName("유틸리티 테스트")
    class UtilityTest {

        @ParameterizedTest
        @DisplayName("색상 코드를 제거해야 한다")
        @CsvSource({
                "'§c빨간색', '빨간색'",
                "'§a§l초록색 굵게', '초록색 굵게'",
                "'&6노란색', '노란색'",
                "'§x§1§2§3§4§5§6RGB색', 'RGB색'",
                "'일반 텍스트', '일반 텍스트'"
        })
        void 색상_코드_제거_테스트(String input, String expected) {
            assertEquals(expected, LoreManager.stripColor(input));
        }

        @Test
        @DisplayName("config 형식의 스탯 라인을 감지해야 한다")
        void 스탯_타입_감지_테스트() {
            // config 형식과 일치하는 것만 true
            assertTrue(loreManager.isStatLine("§c⚔ 공격력 §f+100"));
            assertTrue(loreManager.isStatLine("⚔ 공격력 +100"));
            assertTrue(loreManager.isStatLine("🛡 방어력 +50"));

            // config 형식과 일치하지 않으면 false
            assertFalse(loreManager.isStatLine("공격력 +100"));
            assertFalse(loreManager.isStatLine("방어력: 50"));
            assertFalse(loreManager.isStatLine("일반 설명"));
            assertFalse(loreManager.isStatLine("─────────"));
        }
    }

    @Nested
    @DisplayName("커스텀 템플릿 테스트")
    class CustomTemplateTest {

        @Test
        @DisplayName("커스텀 형식으로 파싱해야 한다")
        void 커스텀_형식_파싱_테스트() {
            // 커스텀 템플릿 생성 (다른 형식)
            LoreTemplate customTemplate = LoreTemplate.builder()
                    .format(StatType.DAMAGE, "공격력: {value}")
                    .format(StatType.DEFENSE, "방어력: {value}")
                    .order(Arrays.asList(StatType.DAMAGE, StatType.DEFENSE))
                    .separatorEnabled(false)
                    .build();

            LoreManager customManager = new LoreManager(customTemplate);

            // 커스텀 형식으로 파싱
            List<String> lore = List.of(
                    "공격력: 100",
                    "방어력: 50"
            );

            ItemStats stats = customManager.parseLore(lore);

            assertEquals(100, stats.getDamage());
            assertEquals(50, stats.getDefense());
        }

        @Test
        @DisplayName("커스텀 형식은 기본 형식을 파싱하지 않아야 한다")
        void 커스텀_형식_기본_형식_불일치_테스트() {
            // 커스텀 템플릿 생성
            LoreTemplate customTemplate = LoreTemplate.builder()
                    .format(StatType.DAMAGE, "공격력: {value}")
                    .order(List.of(StatType.DAMAGE))
                    .separatorEnabled(false)
                    .build();

            LoreManager customManager = new LoreManager(customTemplate);

            // 기본 형식 로어 (파싱되지 않아야 함)
            List<String> lore = List.of(
                    "⚔ 공격력 +100"
            );

            ItemStats stats = customManager.parseLore(lore);

            assertTrue(stats.isEmpty());
        }

        @Test
        @DisplayName("퍼센트 스탯은 % 기호를 선택적으로 매칭해야 한다")
        void 퍼센트_스탯_파싱_테스트() {
            List<String> lore = List.of(
                    "🩸 피흡수 10%",
                    "⚡ 치명타 확률 25%",
                    "💨 회피율 15%"
            );

            ItemStats stats = loreManager.parseLore(lore);

            assertEquals(10, stats.getLifesteal());
            assertEquals(25, stats.getCritChance());
            assertEquals(15, stats.getDodge());
        }
    }
}
