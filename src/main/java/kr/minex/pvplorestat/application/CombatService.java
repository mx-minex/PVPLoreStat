package kr.minex.pvplorestat.application;

import kr.minex.pvplorestat.domain.model.ItemStats;
import kr.minex.pvplorestat.domain.model.PlayerStats;
import kr.minex.pvplorestat.domain.service.DamageCalculator;
import kr.minex.pvplorestat.infrastructure.config.ConfigManager;
import kr.minex.pvplorestat.infrastructure.config.MessageManager;
import kr.minex.pvplorestat.infrastructure.monitoring.PluginMetrics;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * 전투 서비스
 * <p>
 * PVP 전투 시 스탯을 적용하고 데미지를 계산합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class CombatService {

    private final PlayerStatsService playerStatsService;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final DamageCalculator damageCalculator;
    private final PluginMetrics metrics;
    private final Logger logger;

    public CombatService(PlayerStatsService playerStatsService,
                         ConfigManager configManager,
                         MessageManager messageManager,
                         PluginMetrics metrics,
                         Logger logger) {
        this.playerStatsService = Objects.requireNonNull(playerStatsService, "playerStatsService");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.messageManager = Objects.requireNonNull(messageManager, "messageManager");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.damageCalculator = new DamageCalculator(configManager.getDamageConfig());
    }

    /**
     * PVP 데미지를 계산합니다.
     *
     * @param attacker   공격자
     * @param victim     피해자
     * @param baseDamage 기본 데미지
     * @return 계산 결과
     */
    public CombatResult calculateDamage(Player attacker, Player victim, double baseDamage) {
        if (Double.isNaN(baseDamage) || Double.isInfinite(baseDamage) || baseDamage < 0) {
            baseDamage = 0;
        }

        long start = System.nanoTime();
        PlayerStats attackerStats = playerStatsService.getStats(attacker.getUniqueId());
        PlayerStats victimStats = playerStatsService.getStats(victim.getUniqueId());

        ItemStats attackerItemStats = attackerStats.getTotalStats();
        ItemStats victimItemStats = victimStats.getTotalStats();

        DamageCalculator.Result calcResult = damageCalculator.calculate(
                baseDamage, attackerItemStats, victimItemStats);

        long nanos = System.nanoTime() - start;
        metrics.recordCombatCalc(nanos);
        if (configManager.isDebug() && nanos > 2_000_000) { // 2ms
            logger.info("[Debug] combat.calculateDamage took " + (nanos / 1_000_000.0) + "ms");
        }

        return new CombatResult(calcResult, attackerStats, victimStats);
    }

    /**
     * 전투 결과를 적용합니다.
     *
     * @param attacker 공격자
     * @param victim   피해자
     * @param result   전투 결과
     */
    public void applyResult(Player attacker, Player victim, CombatResult result) {
        DamageCalculator.Result calcResult = result.getCalculatorResult();

        // 회피 메시지
        if (calcResult.isDodged()) {
            messageManager.sendRaw(victim, "combat.dodge.victim");
            messageManager.sendRaw(attacker, "combat.dodge.attacker");
            return;
        }

        // 치명타 메시지
        if (calcResult.isCritical()) {
            messageManager.sendRaw(attacker, "combat.critical.attacker",
                    "damage", calcResult.getCriticalBonusDamage());
        }

        // 피흡수 적용
        double lifestealAmount = calcResult.getLifestealAmount();
        if (lifestealAmount > 0) {
            var maxHealthAttr = attacker.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
            if (maxHealthAttr != null) {
                double newHealth = Math.min(
                        attacker.getHealth() + lifestealAmount,
                        maxHealthAttr.getValue()
                );
                attacker.setHealth(newHealth);
            }
        }
    }

    /**
     * PVP 여부를 확인합니다.
     *
     * @param attacker 공격자
     * @param victim   피해자
     * @return PVP이면 true
     */
    public boolean isPvP(Object attacker, Object victim) {
        return attacker instanceof Player && victim instanceof Player;
    }

    /**
     * PVE 포함 여부를 확인합니다.
     *
     * @return PVE도 적용하면 true
     */
    public boolean isApplyToPvE() {
        return !configManager.isPvpOnly();
    }

    /**
     * 전투 결과 클래스
     */
    public static class CombatResult {
        private final DamageCalculator.Result calculatorResult;
        private final PlayerStats attackerStats;
        private final PlayerStats victimStats;

        public CombatResult(DamageCalculator.Result calculatorResult,
                            PlayerStats attackerStats,
                            PlayerStats victimStats) {
            this.calculatorResult = calculatorResult;
            this.attackerStats = attackerStats;
            this.victimStats = victimStats;
        }

        public DamageCalculator.Result getCalculatorResult() {
            return calculatorResult;
        }

        public PlayerStats getAttackerStats() {
            return attackerStats;
        }

        public PlayerStats getVictimStats() {
            return victimStats;
        }

        public double getFinalDamage() {
            return calculatorResult.getFinalDamage();
        }

        public boolean isDodged() {
            return calculatorResult.isDodged();
        }

        public boolean isCritical() {
            return calculatorResult.isCritical();
        }

        public double getLifestealAmount() {
            return calculatorResult.getLifestealAmount();
        }
    }
}
