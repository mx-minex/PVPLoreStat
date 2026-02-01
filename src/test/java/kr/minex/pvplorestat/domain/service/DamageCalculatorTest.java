package kr.minex.pvplorestat.domain.service;

import kr.minex.pvplorestat.domain.model.ItemStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DamageCalculator 도메인 서비스 테스트
 */
@DisplayName("DamageCalculator 도메인 서비스 테스트")
class DamageCalculatorTest {

    private DamageCalculator calculator;
    private DamageCalculator.Config config;

    @BeforeEach
    void setUp() {
        // 기본 설정 (divisor = 2.0)
        config = new DamageCalculator.Config(2.0, 2.0, 2.0);
        calculator = new DamageCalculator(config);
    }

    @Nested
    @DisplayName("공격력 테스트")
    class DamageTest {

        @Test
        @DisplayName("공격력이 데미지에 추가되어야 한다")
        void 공격력_추가_테스트() {
            ItemStats attackerStats = ItemStats.builder().damage(100).build();
            ItemStats victimStats = ItemStats.empty();
            double baseDamage = 5.0;

            DamageCalculator.Result result = calculator.calculate(baseDamage, attackerStats, victimStats);

            // 기본 데미지 5 + (공격력 100 / 2) = 55
            assertEquals(55.0, result.getFinalDamage());
        }

        @ParameterizedTest
        @DisplayName("공격력 비율 계산 테스트")
        @CsvSource({
                "10, 100, 60",  // 10 + 100/2 = 60
                "5, 50, 30",    // 5 + 50/2 = 30
                "0, 100, 50"    // 0 + 100/2 = 50
        })
        void 공격력_비율_테스트(double baseDamage, double attackDamage, double expected) {
            ItemStats attackerStats = ItemStats.builder().damage(attackDamage).build();
            ItemStats victimStats = ItemStats.empty();

            DamageCalculator.Result result = calculator.calculate(baseDamage, attackerStats, victimStats);

            assertEquals(expected, result.getFinalDamage());
        }
    }

    @Nested
    @DisplayName("방어력 테스트")
    class DefenseTest {

        @Test
        @DisplayName("방어력이 데미지를 감소시켜야 한다")
        void 방어력_감소_테스트() {
            ItemStats attackerStats = ItemStats.empty();
            ItemStats victimStats = ItemStats.builder().defense(50).build();
            double baseDamage = 100.0;

            DamageCalculator.Result result = calculator.calculate(baseDamage, attackerStats, victimStats);

            // 기본 데미지 100 - (방어력 50 / 2) = 75
            assertEquals(75.0, result.getFinalDamage());
        }

        @Test
        @DisplayName("방어력이 데미지보다 높으면 최소 0이어야 한다")
        void 최소_데미지_0_테스트() {
            ItemStats attackerStats = ItemStats.empty();
            ItemStats victimStats = ItemStats.builder().defense(500).build();
            double baseDamage = 10.0;

            DamageCalculator.Result result = calculator.calculate(baseDamage, attackerStats, victimStats);

            // 10 - 250 = 음수 -> 0
            assertEquals(0.0, result.getFinalDamage());
        }

        @Test
        @DisplayName("공격력과 방어력이 함께 적용되어야 한다")
        void 공방_동시적용_테스트() {
            ItemStats attackerStats = ItemStats.builder().damage(100).build();
            ItemStats victimStats = ItemStats.builder().defense(60).build();
            double baseDamage = 10.0;

            DamageCalculator.Result result = calculator.calculate(baseDamage, attackerStats, victimStats);

            // (10 + 50) - 30 = 30
            assertEquals(30.0, result.getFinalDamage());
        }
    }

    @Nested
    @DisplayName("치명타 테스트")
    class CriticalTest {

        @Test
        @DisplayName("치명타 확률 100%면 항상 크리티컬이 발동해야 한다")
        void 치명타_100퍼센트_테스트() {
            ItemStats attackerStats = ItemStats.builder()
                    .critChance(100)
                    .critDamage(50)
                    .build();
            ItemStats victimStats = ItemStats.empty();

            // 여러 번 테스트해도 항상 크리티컬
            for (int i = 0; i < 10; i++) {
                DamageCalculator.Result result = calculator.calculate(10, attackerStats, victimStats);
                assertTrue(result.isCritical(), "치명타가 발동해야 함");
            }
        }

        @Test
        @DisplayName("치명타 확률 0%면 절대 크리티컬이 발동하지 않아야 한다")
        void 치명타_0퍼센트_테스트() {
            ItemStats attackerStats = ItemStats.builder()
                    .critChance(0)
                    .critDamage(100)
                    .build();
            ItemStats victimStats = ItemStats.empty();

            for (int i = 0; i < 10; i++) {
                DamageCalculator.Result result = calculator.calculate(10, attackerStats, victimStats);
                assertFalse(result.isCritical(), "치명타가 발동하면 안 됨");
            }
        }

        @Test
        @DisplayName("치명타 발동 시 추가 데미지가 적용되어야 한다")
        void 치명타_추가_데미지_테스트() {
            ItemStats attackerStats = ItemStats.builder()
                    .critChance(100)  // 100% 발동
                    .critDamage(100)  // +50 추가 데미지 (100/2)
                    .build();
            ItemStats victimStats = ItemStats.empty();

            DamageCalculator.Result result = calculator.calculate(10, attackerStats, victimStats);

            // 기본 10 + 치명타 50 = 60
            assertEquals(60.0, result.getFinalDamage());
            assertEquals(50.0, result.getCriticalBonusDamage());
        }

        @Test
        @DisplayName("치명타 미발동 시 치명타 보너스가 0이어야 한다")
        void 치명타_미발동_보너스_0_테스트() {
            ItemStats attackerStats = ItemStats.builder()
                    .critChance(0)
                    .critDamage(100)
                    .build();
            ItemStats victimStats = ItemStats.empty();

            DamageCalculator.Result result = calculator.calculate(10, attackerStats, victimStats);

            assertFalse(result.isCritical());
            assertEquals(0.0, result.getCriticalBonusDamage());
        }
    }

    @Nested
    @DisplayName("회피 테스트")
    class DodgeTest {

        @Test
        @DisplayName("회피율 100%면 항상 회피해야 한다")
        void 회피_100퍼센트_테스트() {
            ItemStats attackerStats = ItemStats.builder().damage(100).build();
            ItemStats victimStats = ItemStats.builder().dodge(100).build();

            for (int i = 0; i < 10; i++) {
                DamageCalculator.Result result = calculator.calculate(50, attackerStats, victimStats);
                assertTrue(result.isDodged(), "회피해야 함");
                assertEquals(0.0, result.getFinalDamage());
            }
        }

        @Test
        @DisplayName("회피율 0%면 절대 회피하지 않아야 한다")
        void 회피_0퍼센트_테스트() {
            ItemStats attackerStats = ItemStats.empty();
            ItemStats victimStats = ItemStats.builder().dodge(0).build();

            for (int i = 0; i < 10; i++) {
                DamageCalculator.Result result = calculator.calculate(50, attackerStats, victimStats);
                assertFalse(result.isDodged(), "회피하면 안 됨");
            }
        }

        @Test
        @DisplayName("회피 시 데미지가 0이어야 한다")
        void 회피_시_데미지_0_테스트() {
            ItemStats attackerStats = ItemStats.builder()
                    .damage(1000)
                    .critChance(100)
                    .critDamage(500)
                    .build();
            ItemStats victimStats = ItemStats.builder().dodge(100).build();

            DamageCalculator.Result result = calculator.calculate(100, attackerStats, victimStats);

            assertTrue(result.isDodged());
            assertEquals(0.0, result.getFinalDamage());
        }
    }

    @Nested
    @DisplayName("피흡수 테스트")
    class LifestealTest {

        @Test
        @DisplayName("피흡수 비율만큼 체력 회복량이 계산되어야 한다")
        void 피흡수_계산_테스트() {
            ItemStats attackerStats = ItemStats.builder()
                    .damage(100)      // +50 데미지
                    .lifesteal(10)    // 10% 피흡수
                    .build();
            ItemStats victimStats = ItemStats.empty();

            DamageCalculator.Result result = calculator.calculate(50, attackerStats, victimStats);

            // 최종 데미지 100 (50+50) 의 10% = 10
            assertEquals(100.0, result.getFinalDamage());
            assertEquals(10.0, result.getLifestealAmount());
        }

        @Test
        @DisplayName("피흡수가 없으면 회복량이 0이어야 한다")
        void 피흡수_없음_테스트() {
            ItemStats attackerStats = ItemStats.builder().damage(100).build();
            ItemStats victimStats = ItemStats.empty();

            DamageCalculator.Result result = calculator.calculate(50, attackerStats, victimStats);

            assertEquals(0.0, result.getLifestealAmount());
        }

        @Test
        @DisplayName("회피 시 피흡수도 0이어야 한다")
        void 회피_시_피흡수_0_테스트() {
            ItemStats attackerStats = ItemStats.builder()
                    .damage(100)
                    .lifesteal(50)
                    .build();
            ItemStats victimStats = ItemStats.builder().dodge(100).build();

            DamageCalculator.Result result = calculator.calculate(50, attackerStats, victimStats);

            assertTrue(result.isDodged());
            assertEquals(0.0, result.getLifestealAmount());
        }
    }

    @Nested
    @DisplayName("복합 시나리오 테스트")
    class ComplexScenarioTest {

        @Test
        @DisplayName("모든 스탯이 동시에 적용되어야 한다 (회피 미발동)")
        void 전체_스탯_적용_테스트() {
            ItemStats attackerStats = ItemStats.builder()
                    .damage(100)       // +50
                    .critChance(100)   // 100% 발동
                    .critDamage(60)    // +30
                    .lifesteal(20)     // 20%
                    .build();
            ItemStats victimStats = ItemStats.builder()
                    .defense(40)       // -20
                    .dodge(0)          // 회피 없음
                    .build();

            DamageCalculator.Result result = calculator.calculate(10, attackerStats, victimStats);

            // 기본 10 + 공격력 50 = 60
            // 크리티컬 +30 = 90
            // 방어력 -20 = 70
            assertEquals(70.0, result.getFinalDamage());
            assertTrue(result.isCritical());
            assertEquals(30.0, result.getCriticalBonusDamage());
            assertEquals(14.0, result.getLifestealAmount()); // 70 * 0.2 = 14
        }
    }

    @Nested
    @DisplayName("설정 테스트")
    class ConfigTest {

        @Test
        @DisplayName("다른 divisor 값으로 계산할 수 있어야 한다")
        void 커스텀_divisor_테스트() {
            // divisor = 4.0 (스탯/4)
            DamageCalculator.Config customConfig = new DamageCalculator.Config(4.0, 4.0, 4.0);
            DamageCalculator customCalculator = new DamageCalculator(customConfig);

            ItemStats attackerStats = ItemStats.builder().damage(100).build();
            ItemStats victimStats = ItemStats.empty();

            DamageCalculator.Result result = customCalculator.calculate(10, attackerStats, victimStats);

            // 10 + 100/4 = 35
            assertEquals(35.0, result.getFinalDamage());
        }
    }
}
