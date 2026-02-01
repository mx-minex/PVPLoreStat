package kr.minex.pvplorestat.presentation.listener;

import kr.minex.pvplorestat.PVPLoreStat;
import kr.minex.pvplorestat.application.PlayerStatsService;
import kr.minex.pvplorestat.domain.model.EquipmentSlot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * 장비 이벤트 리스너
 * <p>
 * 플레이어 접속/퇴장, 장비 변경 시 스탯을 갱신합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class EquipmentListener implements Listener {

    private final PlayerStatsService playerStatsService;
    private final PVPLoreStat plugin;
    private final Logger logger;

    public EquipmentListener(PlayerStatsService playerStatsService, PVPLoreStat plugin, Logger logger) {
        this.playerStatsService = Objects.requireNonNull(playerStatsService, "playerStatsService");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * 플레이어 접속 시 스탯 계산
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerStatsService.calculateAndCache(player);
    }

    /**
     * 플레이어 퇴장 시 캐시 정리
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerStatsService.removeStats(player.getUniqueId());
        playerStatsService.resetMaxHealth(player);
    }

    /**
     * 손 아이템 변경 시 스탯 갱신
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        // 다음 틱에 갱신 (아이템 변경 적용 후)
        player.getServer().getScheduler().runTask(plugin, () -> {
            try {
                playerStatsService.updateEquipmentSlot(player, EquipmentSlot.MAIN_HAND, player.getInventory().getItemInMainHand());
                playerStatsService.updateEquipmentSlot(player, EquipmentSlot.OFF_HAND, player.getInventory().getItemInOffHand());
            } catch (Exception e) {
                logger.warning("손 아이템 스탯 갱신 중 오류가 발생했습니다: " + e.getMessage());
            }
        });
    }

    /**
     * 핫바 슬롯 변경 시 스탯 갱신
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        // 다음 틱에 갱신 (아이템 변경 적용 후)
        player.getServer().getScheduler().runTask(plugin, () -> {
            try {
                playerStatsService.updateEquipmentSlot(player, EquipmentSlot.MAIN_HAND, player.getInventory().getItemInMainHand());
            } catch (Exception e) {
                logger.warning("손 아이템 스탯 갱신 중 오류가 발생했습니다: " + e.getMessage());
            }
        });
    }
}
