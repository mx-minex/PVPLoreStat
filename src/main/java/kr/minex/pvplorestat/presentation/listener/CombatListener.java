package kr.minex.pvplorestat.presentation.listener;

import kr.minex.pvplorestat.application.CombatService;
import kr.minex.pvplorestat.infrastructure.config.ConfigManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * 전투 이벤트 리스너
 * <p>
 * 플레이어 간 전투 시 스탯을 적용합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class CombatListener implements Listener {

    private final CombatService combatService;
    private final ConfigManager configManager;
    private final Logger logger;

    public CombatListener(CombatService combatService, ConfigManager configManager, Logger logger) {
        this.combatService = Objects.requireNonNull(combatService, "combatService");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        long start = System.nanoTime();
        Entity victim = event.getEntity();
        Entity damager = event.getDamager();

        // 투사체 처리
        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Entity shooter) {
                damager = shooter;
            }
        }

        // 공격자가 플레이어인지 확인
        if (!(damager instanceof Player attacker)) {
            return;
        }

        // PVP 모드 확인
        boolean isPvP = victim instanceof Player;
        if (!isPvP && configManager.isPvpOnly()) {
            return;
        }

        // 피해자가 플레이어인 경우에만 방어 스탯 적용
        if (isPvP) {
            Player victimPlayer = (Player) victim;

            double baseDamage = event.getDamage();
            CombatService.CombatResult result = combatService.calculateDamage(
                    attacker, victimPlayer, baseDamage);

            // 회피 시 데미지 0
            if (result.isDodged()) {
                event.setDamage(0);
                combatService.applyResult(attacker, victimPlayer, result);
                return;
            }

            // 데미지 적용
            event.setDamage(result.getFinalDamage());

            // 결과 적용 (피흡수, 메시지 등)
            combatService.applyResult(attacker, victimPlayer, result);
        } else {
            // PVE: 공격자의 공격력만 적용 (방어력/회피 없음)
            // 추후 구현 가능
        }

        if (configManager.isDebug()) {
            long nanos = System.nanoTime() - start;
            if (nanos > 1_000_000) {
                logger.info("[Debug] combat.listener took " + (nanos / 1_000_000.0) + "ms");
            }
        }
    }
}
