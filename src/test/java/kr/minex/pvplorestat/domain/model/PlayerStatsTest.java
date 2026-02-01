package kr.minex.pvplorestat.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlayerStats Value Object 테스트
 */
@DisplayName("PlayerStats Value Object 테스트")
class PlayerStatsTest {

    private static final UUID TEST_UUID = UUID.randomUUID();

    @Nested
    @DisplayName("생성 테스트")
    class CreationTest {

        @Test
        @DisplayName("빈 스탯으로 생성할 수 있어야 한다")
        void 빈_스탯_생성_테스트() {
            PlayerStats stats = PlayerStats.empty(TEST_UUID);

            assertNotNull(stats);
            assertEquals(TEST_UUID, stats.getPlayerId());
            assertTrue(stats.getTotalStats().isEmpty());
        }

        @Test
        @DisplayName("ItemStats에서 PlayerStats를 생성할 수 있어야 한다")
        void ItemStats_기반_생성_테스트() {
            ItemStats itemStats = ItemStats.builder()
                    .damage(100)
                    .defense(50)
                    .build();

            PlayerStats stats = PlayerStats.of(TEST_UUID, itemStats);

            assertEquals(100, stats.getTotalStats().getDamage());
            assertEquals(50, stats.getTotalStats().getDefense());
        }

        @Test
        @DisplayName("여러 ItemStats를 합산하여 생성할 수 있어야 한다")
        void 다중_ItemStats_합산_생성_테스트() {
            ItemStats helmet = ItemStats.builder().defense(20).health(50).build();
            ItemStats chest = ItemStats.builder().defense(40).health(100).build();
            ItemStats weapon = ItemStats.builder().damage(100).critChance(25).build();

            PlayerStats stats = PlayerStats.of(TEST_UUID, List.of(helmet, chest, weapon));

            assertEquals(100, stats.getTotalStats().getDamage());
            assertEquals(60, stats.getTotalStats().getDefense());  // 20 + 40
            assertEquals(150, stats.getTotalStats().getHealth());   // 50 + 100
            assertEquals(25, stats.getTotalStats().getCritChance());
        }
    }

    @Nested
    @DisplayName("장비 스탯 관리 테스트")
    class EquipmentStatsTest {

        @Test
        @DisplayName("장비 슬롯별 스탯을 설정할 수 있어야 한다")
        void 장비_슬롯_스탯_설정_테스트() {
            PlayerStats stats = PlayerStats.empty(TEST_UUID);

            ItemStats helmetStats = ItemStats.builder().defense(20).build();
            PlayerStats updated = stats.withEquipmentStats(EquipmentSlot.HELMET, helmetStats);

            assertEquals(20, updated.getEquipmentStats(EquipmentSlot.HELMET).getDefense());
            assertEquals(20, updated.getTotalStats().getDefense());
        }

        @Test
        @DisplayName("장비 스탯을 변경하면 총 스탯도 갱신되어야 한다")
        void 장비_스탯_변경_총_스탯_갱신_테스트() {
            ItemStats helmet = ItemStats.builder().defense(20).build();
            PlayerStats stats = PlayerStats.of(TEST_UUID, helmet);

            ItemStats newHelmet = ItemStats.builder().defense(40).build();
            PlayerStats updated = stats.withEquipmentStats(EquipmentSlot.HELMET, newHelmet);

            assertEquals(40, updated.getTotalStats().getDefense());
        }

        @Test
        @DisplayName("장비 스탯을 제거할 수 있어야 한다")
        void 장비_스탯_제거_테스트() {
            ItemStats helmet = ItemStats.builder().defense(20).build();
            PlayerStats stats = PlayerStats.empty(TEST_UUID)
                    .withEquipmentStats(EquipmentSlot.HELMET, helmet);

            PlayerStats removed = stats.removeEquipmentStats(EquipmentSlot.HELMET);

            assertTrue(removed.getEquipmentStats(EquipmentSlot.HELMET).isEmpty());
            assertEquals(0, removed.getTotalStats().getDefense());
        }

        @Test
        @DisplayName("여러 장비의 스탯이 모두 합산되어야 한다")
        void 전체_장비_합산_테스트() {
            PlayerStats stats = PlayerStats.empty(TEST_UUID)
                    .withEquipmentStats(EquipmentSlot.HELMET, ItemStats.builder().defense(10).build())
                    .withEquipmentStats(EquipmentSlot.CHESTPLATE, ItemStats.builder().defense(20).build())
                    .withEquipmentStats(EquipmentSlot.LEGGINGS, ItemStats.builder().defense(15).build())
                    .withEquipmentStats(EquipmentSlot.BOOTS, ItemStats.builder().defense(10).build())
                    .withEquipmentStats(EquipmentSlot.MAIN_HAND, ItemStats.builder().damage(100).build());

            assertEquals(55, stats.getTotalStats().getDefense());  // 10+20+15+10
            assertEquals(100, stats.getTotalStats().getDamage());
        }
    }

    @Nested
    @DisplayName("체력 계산 테스트")
    class HealthCalculationTest {

        @Test
        @DisplayName("추가 체력으로 최대 체력을 계산할 수 있어야 한다")
        void 최대_체력_계산_테스트() {
            double baseHealth = 20.0;

            ItemStats stats = ItemStats.builder().health(100).build();
            PlayerStats playerStats = PlayerStats.of(TEST_UUID, stats);

            // 기본 체력 20 + 추가 체력 100 = 120
            double expectedMaxHealth = baseHealth + stats.getHealth();
            assertEquals(expectedMaxHealth, playerStats.calculateMaxHealth(baseHealth));
        }

        @Test
        @DisplayName("체력 스탯이 없으면 기본 체력만 반환해야 한다")
        void 기본_체력_반환_테스트() {
            double baseHealth = 20.0;
            PlayerStats playerStats = PlayerStats.empty(TEST_UUID);

            assertEquals(baseHealth, playerStats.calculateMaxHealth(baseHealth));
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("PlayerStats는 불변 객체여야 한다")
        void 불변성_테스트() {
            PlayerStats original = PlayerStats.empty(TEST_UUID);
            ItemStats helmet = ItemStats.builder().defense(20).build();

            PlayerStats modified = original.withEquipmentStats(EquipmentSlot.HELMET, helmet);

            // 원본은 변경되지 않아야 함
            assertTrue(original.getTotalStats().isEmpty());
            assertEquals(20, modified.getTotalStats().getDefense());
            assertNotSame(original, modified);
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 UUID와 스탯을 가진 객체는 동등해야 한다")
        void 동등성_테스트() {
            ItemStats helmet = ItemStats.builder().defense(20).build();

            PlayerStats stats1 = PlayerStats.empty(TEST_UUID)
                    .withEquipmentStats(EquipmentSlot.HELMET, helmet);
            PlayerStats stats2 = PlayerStats.empty(TEST_UUID)
                    .withEquipmentStats(EquipmentSlot.HELMET, helmet);

            assertEquals(stats1, stats2);
            assertEquals(stats1.hashCode(), stats2.hashCode());
        }

        @Test
        @DisplayName("다른 UUID를 가진 객체는 동등하지 않아야 한다")
        void UUID_비동등성_테스트() {
            PlayerStats stats1 = PlayerStats.empty(UUID.randomUUID());
            PlayerStats stats2 = PlayerStats.empty(UUID.randomUUID());

            assertNotEquals(stats1, stats2);
        }
    }
}
