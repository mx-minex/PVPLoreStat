package kr.minex.pvplorestat.infrastructure.monitoring;

import java.util.concurrent.atomic.LongAdder;

/**
 * 플러그인 메트릭(경량)
 * <p>
 * 운영 디버깅과 성능 병목 추적을 위해 핵심 경로의 호출 횟수/지연을 누적합니다.
 * </p>
 */
public final class PluginMetrics {

    private final LongAdder loreParseCount = new LongAdder();
    private final LongAdder loreParseNanos = new LongAdder();

    private final LongAdder playerStatCalcCount = new LongAdder();
    private final LongAdder playerStatCalcNanos = new LongAdder();

    private final LongAdder combatCalcCount = new LongAdder();
    private final LongAdder combatCalcNanos = new LongAdder();

    private final LongAdder statUpdateTaskRuns = new LongAdder();
    private final LongAdder statUpdateTaskNanos = new LongAdder();

    public void recordLoreParse(long nanos) {
        loreParseCount.increment();
        loreParseNanos.add(nanos);
    }

    public void recordPlayerStatCalc(long nanos) {
        playerStatCalcCount.increment();
        playerStatCalcNanos.add(nanos);
    }

    public void recordCombatCalc(long nanos) {
        combatCalcCount.increment();
        combatCalcNanos.add(nanos);
    }

    public void recordStatUpdateTaskRun(long nanos) {
        statUpdateTaskRuns.increment();
        statUpdateTaskNanos.add(nanos);
    }

    public String snapshot() {
        return "PluginMetrics{" +
                "loreParseCount=" + loreParseCount.sum() +
                ", loreParseAvgMs=" + avgMillis(loreParseNanos.sum(), loreParseCount.sum()) +
                ", playerStatCalcCount=" + playerStatCalcCount.sum() +
                ", playerStatCalcAvgMs=" + avgMillis(playerStatCalcNanos.sum(), playerStatCalcCount.sum()) +
                ", combatCalcCount=" + combatCalcCount.sum() +
                ", combatCalcAvgMs=" + avgMillis(combatCalcNanos.sum(), combatCalcCount.sum()) +
                ", statUpdateTaskRuns=" + statUpdateTaskRuns.sum() +
                ", statUpdateTaskAvgMs=" + avgMillis(statUpdateTaskNanos.sum(), statUpdateTaskRuns.sum()) +
                '}';
    }

    private static double avgMillis(long nanos, long count) {
        if (count <= 0) {
            return 0.0;
        }
        return (nanos / 1_000_000.0) / count;
    }
}

