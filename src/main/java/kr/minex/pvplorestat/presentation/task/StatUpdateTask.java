package kr.minex.pvplorestat.presentation.task;

import kr.minex.pvplorestat.application.PlayerStatsService;
import kr.minex.pvplorestat.infrastructure.config.ConfigManager;
import kr.minex.pvplorestat.infrastructure.monitoring.PluginMetrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * 스탯 업데이트 태스크
 * <p>
 * 주기적으로 모든 플레이어의 장비 스탯을 갱신합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class StatUpdateTask extends BukkitRunnable {

    private final PlayerStatsService playerStatsService;
    private final PluginMetrics metrics;
    private final Logger logger;
    private final ConfigManager configManager;

    public StatUpdateTask(PlayerStatsService playerStatsService, PluginMetrics metrics, Logger logger, ConfigManager configManager) {
        this.playerStatsService = Objects.requireNonNull(playerStatsService, "playerStatsService");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
    }

    @Override
    public void run() {
        long start = System.nanoTime();
        int players = 0;
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                players++;
                playerStatsService.calculateAndCache(player);
            }
        } finally {
            long nanos = System.nanoTime() - start;
            metrics.recordStatUpdateTaskRun(nanos);
            if (configManager.isDebug() && nanos > 10_000_000) { // 10ms
                logger.info("[Debug] statUpdateTask took " + (nanos / 1_000_000.0) + "ms for " + players + " players");
            }
        }
    }
}
