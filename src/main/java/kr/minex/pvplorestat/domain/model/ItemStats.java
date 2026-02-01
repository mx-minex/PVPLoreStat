package kr.minex.pvplorestat.domain.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * 아이템 스탯 Value Object (불변)
 * <p>
 * 아이템 하나에 설정된 스탯을 나타냅니다.
 * 불변 객체로 설계되어 스레드 안전합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public final class ItemStats {

    private final double damage;
    private final double defense;
    private final double health;
    private final double lifesteal;
    private final double critChance;
    private final double critDamage;
    private final double dodge;

    /**
     * 빈 스탯 싱글톤 인스턴스
     */
    private static final ItemStats EMPTY = new ItemStats(0, 0, 0, 0, 0, 0, 0);

    private ItemStats(double damage, double defense, double health,
                      double lifesteal, double critChance, double critDamage, double dodge) {
        // 음수 방지
        this.damage = Math.max(0, damage);
        this.defense = Math.max(0, defense);
        this.health = Math.max(0, health);
        this.lifesteal = Math.max(0, lifesteal);
        this.critChance = Math.max(0, critChance);
        this.critDamage = Math.max(0, critDamage);
        this.dodge = Math.max(0, dodge);
    }

    /**
     * 빈 스탯 인스턴스를 반환합니다.
     *
     * @return 모든 값이 0인 스탯
     */
    public static ItemStats empty() {
        return EMPTY;
    }

    /**
     * 빌더를 생성합니다.
     *
     * @return 새 빌더 인스턴스
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 단일 스탯으로 인스턴스를 생성합니다.
     *
     * @param type  스탯 타입
     * @param value 스탯 값
     * @return 해당 스탯만 설정된 인스턴스
     */
    public static ItemStats of(StatType type, double value) {
        Builder builder = builder();
        switch (type) {
            case DAMAGE -> builder.damage(value);
            case DEFENSE -> builder.defense(value);
            case HEALTH -> builder.health(value);
            case LIFESTEAL -> builder.lifesteal(value);
            case CRIT_CHANCE -> builder.critChance(value);
            case CRIT_DAMAGE -> builder.critDamage(value);
            case DODGE -> builder.dodge(value);
        }
        return builder.build();
    }

    /**
     * Map에서 스탯을 생성합니다.
     *
     * @param statMap 스탯 타입과 값의 맵
     * @return 생성된 ItemStats
     */
    public static ItemStats fromMap(Map<StatType, Double> statMap) {
        Builder builder = builder();
        statMap.forEach((type, value) -> {
            switch (type) {
                case DAMAGE -> builder.damage(value);
                case DEFENSE -> builder.defense(value);
                case HEALTH -> builder.health(value);
                case LIFESTEAL -> builder.lifesteal(value);
                case CRIT_CHANCE -> builder.critChance(value);
                case CRIT_DAMAGE -> builder.critDamage(value);
                case DODGE -> builder.dodge(value);
            }
        });
        return builder.build();
    }

    // ===== Getters =====

    public double getDamage() {
        return damage;
    }

    public double getDefense() {
        return defense;
    }

    public double getHealth() {
        return health;
    }

    public double getLifesteal() {
        return lifesteal;
    }

    public double getCritChance() {
        return critChance;
    }

    public double getCritDamage() {
        return critDamage;
    }

    public double getDodge() {
        return dodge;
    }

    /**
     * StatType으로 스탯 값을 조회합니다.
     *
     * @param type 스탯 타입
     * @return 스탯 값
     */
    public double getStat(StatType type) {
        return switch (type) {
            case DAMAGE -> damage;
            case DEFENSE -> defense;
            case HEALTH -> health;
            case LIFESTEAL -> lifesteal;
            case CRIT_CHANCE -> critChance;
            case CRIT_DAMAGE -> critDamage;
            case DODGE -> dodge;
        };
    }

    /**
     * 특정 스탯이 설정되어 있는지 확인합니다.
     *
     * @param type 스탯 타입
     * @return 스탯이 0보다 크면 true
     */
    public boolean hasStat(StatType type) {
        return getStat(type) > 0;
    }

    /**
     * 모든 스탯이 0인지 확인합니다.
     *
     * @return 모든 스탯이 0이면 true
     */
    public boolean isEmpty() {
        return damage == 0 && defense == 0 && health == 0 &&
                lifesteal == 0 && critChance == 0 && critDamage == 0 && dodge == 0;
    }

    /**
     * 0이 아닌 스탯만 Map으로 반환합니다.
     *
     * @return 스탯 타입과 값의 맵
     */
    public Map<StatType, Double> getNonZeroStats() {
        Map<StatType, Double> result = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            double value = getStat(type);
            if (value > 0) {
                result.put(type, value);
            }
        }
        return result;
    }

    /**
     * 모든 스탯을 Map으로 반환합니다.
     *
     * @return 스탯 타입과 값의 맵
     */
    public Map<StatType, Double> toMap() {
        Map<StatType, Double> result = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            result.put(type, getStat(type));
        }
        return result;
    }

    // ===== With 메서드 (불변성 유지) =====

    public ItemStats withDamage(double damage) {
        return new ItemStats(damage, defense, health, lifesteal, critChance, critDamage, dodge);
    }

    public ItemStats withDefense(double defense) {
        return new ItemStats(damage, defense, health, lifesteal, critChance, critDamage, dodge);
    }

    public ItemStats withHealth(double health) {
        return new ItemStats(damage, defense, health, lifesteal, critChance, critDamage, dodge);
    }

    public ItemStats withLifesteal(double lifesteal) {
        return new ItemStats(damage, defense, health, lifesteal, critChance, critDamage, dodge);
    }

    public ItemStats withCritChance(double critChance) {
        return new ItemStats(damage, defense, health, lifesteal, critChance, critDamage, dodge);
    }

    public ItemStats withCritDamage(double critDamage) {
        return new ItemStats(damage, defense, health, lifesteal, critChance, critDamage, dodge);
    }

    public ItemStats withDodge(double dodge) {
        return new ItemStats(damage, defense, health, lifesteal, critChance, critDamage, dodge);
    }

    /**
     * 특정 스탯을 수정한 새 인스턴스를 반환합니다.
     *
     * @param type  스탯 타입
     * @param value 새 값
     * @return 수정된 새 인스턴스
     */
    public ItemStats withStat(StatType type, double value) {
        return switch (type) {
            case DAMAGE -> withDamage(value);
            case DEFENSE -> withDefense(value);
            case HEALTH -> withHealth(value);
            case LIFESTEAL -> withLifesteal(value);
            case CRIT_CHANCE -> withCritChance(value);
            case CRIT_DAMAGE -> withCritDamage(value);
            case DODGE -> withDodge(value);
        };
    }

    // ===== 연산 메서드 =====

    /**
     * 다른 스탯과 합산한 새 인스턴스를 반환합니다.
     *
     * @param other 합산할 스탯
     * @return 합산된 새 인스턴스
     */
    public ItemStats merge(ItemStats other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return other;
        }
        return new ItemStats(
                this.damage + other.damage,
                this.defense + other.defense,
                this.health + other.health,
                this.lifesteal + other.lifesteal,
                this.critChance + other.critChance,
                this.critDamage + other.critDamage,
                this.dodge + other.dodge
        );
    }

    /**
     * 특정 스탯을 제거한 새 인스턴스를 반환합니다.
     *
     * @param type 제거할 스탯 타입
     * @return 스탯이 제거된 새 인스턴스
     */
    public ItemStats removeStat(StatType type) {
        return withStat(type, 0);
    }

    // ===== equals, hashCode, toString =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemStats itemStats = (ItemStats) o;
        return Double.compare(damage, itemStats.damage) == 0 &&
                Double.compare(defense, itemStats.defense) == 0 &&
                Double.compare(health, itemStats.health) == 0 &&
                Double.compare(lifesteal, itemStats.lifesteal) == 0 &&
                Double.compare(critChance, itemStats.critChance) == 0 &&
                Double.compare(critDamage, itemStats.critDamage) == 0 &&
                Double.compare(dodge, itemStats.dodge) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(damage, defense, health, lifesteal, critChance, critDamage, dodge);
    }

    @Override
    public String toString() {
        return "ItemStats{" +
                "damage=" + damage +
                ", defense=" + defense +
                ", health=" + health +
                ", lifesteal=" + lifesteal +
                ", critChance=" + critChance +
                ", critDamage=" + critDamage +
                ", dodge=" + dodge +
                '}';
    }

    // ===== Builder =====

    /**
     * ItemStats 빌더 클래스
     */
    public static final class Builder {
        private double damage = 0;
        private double defense = 0;
        private double health = 0;
        private double lifesteal = 0;
        private double critChance = 0;
        private double critDamage = 0;
        private double dodge = 0;

        private Builder() {
        }

        public Builder damage(double damage) {
            this.damage = damage;
            return this;
        }

        public Builder defense(double defense) {
            this.defense = defense;
            return this;
        }

        public Builder health(double health) {
            this.health = health;
            return this;
        }

        public Builder lifesteal(double lifesteal) {
            this.lifesteal = lifesteal;
            return this;
        }

        public Builder critChance(double critChance) {
            this.critChance = critChance;
            return this;
        }

        public Builder critDamage(double critDamage) {
            this.critDamage = critDamage;
            return this;
        }

        public Builder dodge(double dodge) {
            this.dodge = dodge;
            return this;
        }

        public ItemStats build() {
            return new ItemStats(damage, defense, health, lifesteal, critChance, critDamage, dodge);
        }
    }
}
