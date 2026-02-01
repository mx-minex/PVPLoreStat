package kr.minex.pvplorestat.domain.service;

import kr.minex.pvplorestat.domain.model.ItemStats;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 데미지 계산 도메인 서비스
 * <p>
 * 순수한 데미지 계산 로직만을 담당합니다.
 * Bukkit 의존성 없이 도메인 로직만 포함합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class DamageCalculator {

    private final Config config;
    private final Random random;

    /**
     * 기본 Random으로 DamageCalculator를 생성합니다.
     *
     * @param config 계산 설정
     */
    public DamageCalculator(Config config) {
        this(config, ThreadLocalRandom.current());
    }

    /**
     * 지정된 Random으로 DamageCalculator를 생성합니다. (테스트용)
     *
     * @param config 계산 설정
     * @param random 랜덤 생성기
     */
    public DamageCalculator(Config config, Random random) {
        this.config = config;
        this.random = random;
    }

    /**
     * 데미지를 계산합니다.
     *
     * @param baseDamage    기본 데미지
     * @param attackerStats 공격자 스탯
     * @param victimStats   피해자 스탯
     * @return 계산 결과
     */
    public Result calculate(double baseDamage, ItemStats attackerStats, ItemStats victimStats) {
        // 1. 회피 체크 (가장 먼저)
        boolean dodged = checkDodge(victimStats.getDodge());
        if (dodged) {
            return Result.dodged();
        }

        // 2. 공격력 적용
        double damage = baseDamage + (attackerStats.getDamage() / config.damageDivisor);

        // 3. 치명타 체크 및 적용
        boolean critical = checkCritical(attackerStats.getCritChance());
        double criticalBonus = 0;
        if (critical) {
            criticalBonus = attackerStats.getCritDamage() / config.critDamageDivisor;
            damage += criticalBonus;
        }

        // 4. 방어력 적용
        double defenseReduction = victimStats.getDefense() / config.defenseDivisor;
        damage -= defenseReduction;

        // 5. 최소 데미지 0 보장
        damage = Math.max(0, damage);

        // 6. 피흡수 계산
        double lifesteal = calculateLifesteal(damage, attackerStats.getLifesteal());

        return new Result(damage, critical, criticalBonus, lifesteal, false);
    }

    /**
     * 회피 발동 여부를 체크합니다.
     *
     * @param dodgeChance 회피율 (0-100)
     * @return 회피 성공 시 true
     */
    private boolean checkDodge(double dodgeChance) {
        if (dodgeChance <= 0) {
            return false;
        }
        if (dodgeChance >= 100) {
            return true;
        }
        return random.nextDouble() * 100 < dodgeChance;
    }

    /**
     * 치명타 발동 여부를 체크합니다.
     *
     * @param critChance 치명타 확률 (0-100)
     * @return 치명타 발동 시 true
     */
    private boolean checkCritical(double critChance) {
        if (critChance <= 0) {
            return false;
        }
        if (critChance >= 100) {
            return true;
        }
        return random.nextDouble() * 100 < critChance;
    }

    /**
     * 피흡수 회복량을 계산합니다.
     *
     * @param damage          최종 데미지
     * @param lifestealPercent 피흡수율 (0-100)
     * @return 회복량
     */
    private double calculateLifesteal(double damage, double lifestealPercent) {
        if (lifestealPercent <= 0 || damage <= 0) {
            return 0;
        }
        return damage * (lifestealPercent / 100.0);
    }

    /**
     * 데미지 계산 설정
     */
    public static class Config {
        private final double damageDivisor;
        private final double defenseDivisor;
        private final double critDamageDivisor;

        /**
         * 계산 설정을 생성합니다.
         *
         * @param damageDivisor     공격력 나눗수 (예: 2.0이면 공격력/2)
         * @param defenseDivisor    방어력 나눗수 (예: 2.0이면 방어력/2)
         * @param critDamageDivisor 치명타 데미지 나눗수 (예: 2.0이면 치뎀/2)
         */
        public Config(double damageDivisor, double defenseDivisor, double critDamageDivisor) {
            this.damageDivisor = damageDivisor > 0 ? damageDivisor : 1.0;
            this.defenseDivisor = defenseDivisor > 0 ? defenseDivisor : 1.0;
            this.critDamageDivisor = critDamageDivisor > 0 ? critDamageDivisor : 1.0;
        }

        /**
         * 기본 설정 (모든 divisor = 2.0)
         *
         * @return 기본 설정
         */
        public static Config defaults() {
            return new Config(2.0, 2.0, 2.0);
        }

        public double getDamageDivisor() {
            return damageDivisor;
        }

        public double getDefenseDivisor() {
            return defenseDivisor;
        }

        public double getCritDamageDivisor() {
            return critDamageDivisor;
        }
    }

    /**
     * 데미지 계산 결과
     */
    public static class Result {
        private final double finalDamage;
        private final boolean critical;
        private final double criticalBonusDamage;
        private final double lifestealAmount;
        private final boolean dodged;

        private Result(double finalDamage, boolean critical, double criticalBonusDamage,
                       double lifestealAmount, boolean dodged) {
            this.finalDamage = finalDamage;
            this.critical = critical;
            this.criticalBonusDamage = criticalBonusDamage;
            this.lifestealAmount = lifestealAmount;
            this.dodged = dodged;
        }

        /**
         * 회피된 결과를 생성합니다.
         *
         * @return 회피 결과
         */
        public static Result dodged() {
            return new Result(0, false, 0, 0, true);
        }

        /**
         * 최종 데미지를 반환합니다.
         *
         * @return 최종 데미지 (0 이상)
         */
        public double getFinalDamage() {
            return finalDamage;
        }

        /**
         * 치명타 발동 여부를 반환합니다.
         *
         * @return 치명타 발동 시 true
         */
        public boolean isCritical() {
            return critical;
        }

        /**
         * 치명타 보너스 데미지를 반환합니다.
         *
         * @return 치명타 보너스 데미지
         */
        public double getCriticalBonusDamage() {
            return criticalBonusDamage;
        }

        /**
         * 피흡수 회복량을 반환합니다.
         *
         * @return 회복량
         */
        public double getLifestealAmount() {
            return lifestealAmount;
        }

        /**
         * 회피 여부를 반환합니다.
         *
         * @return 회피 성공 시 true
         */
        public boolean isDodged() {
            return dodged;
        }

        @Override
        public String toString() {
            if (dodged) {
                return "Result{DODGED}";
            }
            return "Result{" +
                    "finalDamage=" + finalDamage +
                    ", critical=" + critical +
                    ", criticalBonus=" + criticalBonusDamage +
                    ", lifesteal=" + lifestealAmount +
                    '}';
        }
    }
}
