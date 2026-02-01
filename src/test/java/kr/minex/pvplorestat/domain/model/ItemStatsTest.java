package kr.minex.pvplorestat.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ItemStats Value Object 테스트
 */
@DisplayName("ItemStats Value Object 테스트")
class ItemStatsTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreationTest {

        @Test
        @DisplayName("빈 스탯으로 생성할 수 있어야 한다")
        void 빈_스탯_생성_테스트() {
            ItemStats stats = ItemStats.empty();

            assertNotNull(stats);
            assertEquals(0, stats.getDamage());
            assertEquals(0, stats.getDefense());
            assertEquals(0, stats.getHealth());
            assertEquals(0, stats.getLifesteal());
            assertEquals(0, stats.getCritChance());
            assertEquals(0, stats.getCritDamage());
            assertEquals(0, stats.getDodge());
        }

        @Test
        @DisplayName("빌더로 스탯을 설정할 수 있어야 한다")
        void 빌더_스탯_설정_테스트() {
            ItemStats stats = ItemStats.builder()
                    .damage(100)
                    .defense(50)
                    .health(200)
                    .lifesteal(10)
                    .critChance(25)
                    .critDamage(150)
                    .dodge(15)
                    .build();

            assertEquals(100, stats.getDamage());
            assertEquals(50, stats.getDefense());
            assertEquals(200, stats.getHealth());
            assertEquals(10, stats.getLifesteal());
            assertEquals(25, stats.getCritChance());
            assertEquals(150, stats.getCritDamage());
            assertEquals(15, stats.getDodge());
        }

        @Test
        @DisplayName("단일 스탯만 설정해도 나머지는 0이어야 한다")
        void 단일_스탯_설정_테스트() {
            ItemStats stats = ItemStats.builder()
                    .damage(50)
                    .build();

            assertEquals(50, stats.getDamage());
            assertEquals(0, stats.getDefense());
            assertEquals(0, stats.getHealth());
        }

        @Test
        @DisplayName("of 메서드로 단일 스탯을 생성할 수 있어야 한다")
        void of_메서드_테스트() {
            ItemStats stats = ItemStats.of(StatType.DAMAGE, 100);

            assertEquals(100, stats.getDamage());
            assertEquals(0, stats.getDefense());
        }

        @Test
        @DisplayName("Map으로 스탯을 생성할 수 있어야 한다")
        void Map_생성_테스트() {
            Map<StatType, Double> statMap = Map.of(
                    StatType.DAMAGE, 100.0,
                    StatType.DEFENSE, 50.0
            );

            ItemStats stats = ItemStats.fromMap(statMap);

            assertEquals(100, stats.getDamage());
            assertEquals(50, stats.getDefense());
            assertEquals(0, stats.getHealth());
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("ItemStats는 불변 객체여야 한다")
        void 불변성_테스트() {
            ItemStats original = ItemStats.builder()
                    .damage(100)
                    .build();

            // with 메서드로 새 객체 생성
            ItemStats modified = original.withDamage(200);

            // 원본은 변경되지 않아야 함
            assertEquals(100, original.getDamage());
            assertEquals(200, modified.getDamage());
            assertNotSame(original, modified);
        }

        @Test
        @DisplayName("각 스탯별 with 메서드가 작동해야 한다")
        void with_메서드_테스트() {
            ItemStats base = ItemStats.empty();

            assertEquals(100, base.withDamage(100).getDamage());
            assertEquals(50, base.withDefense(50).getDefense());
            assertEquals(200, base.withHealth(200).getHealth());
            assertEquals(10, base.withLifesteal(10).getLifesteal());
            assertEquals(25, base.withCritChance(25).getCritChance());
            assertEquals(150, base.withCritDamage(150).getCritDamage());
            assertEquals(15, base.withDodge(15).getDodge());
        }
    }

    @Nested
    @DisplayName("스탯 조회 테스트")
    class GetterTest {

        @Test
        @DisplayName("StatType으로 스탯 값을 조회할 수 있어야 한다")
        void StatType_조회_테스트() {
            ItemStats stats = ItemStats.builder()
                    .damage(100)
                    .critChance(25)
                    .build();

            assertEquals(100, stats.getStat(StatType.DAMAGE));
            assertEquals(25, stats.getStat(StatType.CRIT_CHANCE));
            assertEquals(0, stats.getStat(StatType.DEFENSE));
        }

        @Test
        @DisplayName("0이 아닌 스탯만 조회할 수 있어야 한다")
        void 비어있지_않은_스탯_조회_테스트() {
            ItemStats stats = ItemStats.builder()
                    .damage(100)
                    .critChance(25)
                    .build();

            Map<StatType, Double> nonZeroStats = stats.getNonZeroStats();

            assertEquals(2, nonZeroStats.size());
            assertTrue(nonZeroStats.containsKey(StatType.DAMAGE));
            assertTrue(nonZeroStats.containsKey(StatType.CRIT_CHANCE));
            assertFalse(nonZeroStats.containsKey(StatType.DEFENSE));
        }

        @Test
        @DisplayName("스탯이 있는지 확인할 수 있어야 한다")
        void 스탯_존재_확인_테스트() {
            ItemStats stats = ItemStats.builder()
                    .damage(100)
                    .build();

            assertTrue(stats.hasStat(StatType.DAMAGE));
            assertFalse(stats.hasStat(StatType.DEFENSE));
        }

        @Test
        @DisplayName("빈 스탯인지 확인할 수 있어야 한다")
        void 빈_스탯_확인_테스트() {
            ItemStats empty = ItemStats.empty();
            ItemStats nonEmpty = ItemStats.builder().damage(100).build();

            assertTrue(empty.isEmpty());
            assertFalse(nonEmpty.isEmpty());
        }
    }

    @Nested
    @DisplayName("스탯 연산 테스트")
    class OperationTest {

        @Test
        @DisplayName("두 ItemStats를 더할 수 있어야 한다")
        void 스탯_덧셈_테스트() {
            ItemStats stats1 = ItemStats.builder()
                    .damage(100)
                    .defense(50)
                    .build();

            ItemStats stats2 = ItemStats.builder()
                    .damage(50)
                    .health(200)
                    .build();

            ItemStats merged = stats1.merge(stats2);

            assertEquals(150, merged.getDamage());  // 100 + 50
            assertEquals(50, merged.getDefense());   // 50 + 0
            assertEquals(200, merged.getHealth());   // 0 + 200
        }

        @Test
        @DisplayName("빈 스탯과 더해도 값이 유지되어야 한다")
        void 빈_스탯_덧셈_테스트() {
            ItemStats stats = ItemStats.builder()
                    .damage(100)
                    .build();

            ItemStats merged = stats.merge(ItemStats.empty());

            assertEquals(100, merged.getDamage());
        }

        @Test
        @DisplayName("특정 스탯을 제거할 수 있어야 한다")
        void 스탯_제거_테스트() {
            ItemStats stats = ItemStats.builder()
                    .damage(100)
                    .defense(50)
                    .build();

            ItemStats removed = stats.removeStat(StatType.DAMAGE);

            assertEquals(0, removed.getDamage());
            assertEquals(50, removed.getDefense());
        }
    }

    @Nested
    @DisplayName("유효성 검증 테스트")
    class ValidationTest {

        @Test
        @DisplayName("음수 스탯은 0으로 처리되어야 한다")
        void 음수_스탯_방지_테스트() {
            ItemStats stats = ItemStats.builder()
                    .damage(-100)
                    .defense(-50)
                    .build();

            assertEquals(0, stats.getDamage());
            assertEquals(0, stats.getDefense());
        }

        @Test
        @DisplayName("소수점 스탯을 지원해야 한다")
        void 소수점_스탯_테스트() {
            ItemStats stats = ItemStats.builder()
                    .damage(10.5)
                    .lifesteal(5.5)
                    .build();

            assertEquals(10.5, stats.getDamage());
            assertEquals(5.5, stats.getLifesteal());
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 스탯을 가진 객체는 동등해야 한다")
        void 동등성_테스트() {
            ItemStats stats1 = ItemStats.builder()
                    .damage(100)
                    .defense(50)
                    .build();

            ItemStats stats2 = ItemStats.builder()
                    .damage(100)
                    .defense(50)
                    .build();

            assertEquals(stats1, stats2);
            assertEquals(stats1.hashCode(), stats2.hashCode());
        }

        @Test
        @DisplayName("다른 스탯을 가진 객체는 동등하지 않아야 한다")
        void 비동등성_테스트() {
            ItemStats stats1 = ItemStats.builder()
                    .damage(100)
                    .build();

            ItemStats stats2 = ItemStats.builder()
                    .damage(200)
                    .build();

            assertNotEquals(stats1, stats2);
        }
    }
}
