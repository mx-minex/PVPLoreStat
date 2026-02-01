package kr.minex.pvplorestat.infrastructure.monitoring;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * 주기적으로 메트릭 스냅샷을 로그로 출력합니다.
 * <p>
 * 디버그 모드에서만 활성화하는 것을 권장합니다.
 * </p>
 */
public final class MetricsLogTask extends BukkitRunnable {

    private final PluginMetrics metrics;
    private final Logger logger;

    public MetricsLogTask(PluginMetrics metrics, Logger logger) {
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void run() {
        logger.info("[Metrics] " + metrics.snapshot());
    }
}

