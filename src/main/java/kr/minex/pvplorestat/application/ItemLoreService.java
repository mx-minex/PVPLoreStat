package kr.minex.pvplorestat.application;

import kr.minex.pvplorestat.domain.model.ItemStats;
import kr.minex.pvplorestat.domain.model.StatType;
import kr.minex.pvplorestat.infrastructure.config.ConfigManager;
import kr.minex.pvplorestat.infrastructure.monitoring.PluginMetrics;
import kr.minex.pvplorestat.infrastructure.lore.LoreManager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * 아이템 로어 서비스
 * <p>
 * 아이템에 스탯 로어를 설정, 수정, 제거하는 기능을 제공합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class ItemLoreService {

    private final LoreManager loreManager;
    private final ConfigManager configManager;
    private final PluginMetrics metrics;
    private final Logger logger;

    public ItemLoreService(LoreManager loreManager, ConfigManager configManager, PluginMetrics metrics, Logger logger) {
        this.loreManager = Objects.requireNonNull(loreManager, "loreManager");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public record StatApplyResult(boolean success, double appliedValue) {
    }

    /**
     * 아이템에서 스탯을 파싱합니다.
     *
     * @param item 아이템
     * @return 파싱된 스탯
     */
    public ItemStats parseStats(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return ItemStats.empty();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return ItemStats.empty();
        }

        long start = System.nanoTime();
        try {
            return loreManager.parseLore(meta.getLore());
        } finally {
            metrics.recordLoreParse(System.nanoTime() - start);
        }
    }

    /**
     * 아이템에 단일 스탯을 설정합니다.
     * 기존 같은 스탯이 있으면 덮어씁니다.
     *
     * @param item  아이템
     * @param type  스탯 타입
     * @param value 스탯 값
     * @return 성공 여부
     */
    public StatApplyResult setStat(ItemStack item, StatType type, double value) {
        if (item == null) {
            return new StatApplyResult(false, 0);
        }

        double applied = clampStatValue(type, value);

        // 기존 스탯 파싱
        ItemStats currentStats = parseStats(item);

        // 새 스탯 설정
        ItemStats newStats = currentStats.withStat(type, applied);

        // 로어 업데이트
        boolean ok = updateItemLore(item, newStats);
        if (!ok) {
            logger.warning("아이템 로어 업데이트에 실패했습니다. type=" + type + ", value=" + applied);
        }
        return new StatApplyResult(ok, applied);
    }

    /**
     * 아이템에 여러 스탯을 한 번에 설정합니다.
     *
     * @param item  아이템
     * @param stats 설정할 스탯
     * @return 성공 여부
     */
    public boolean setStats(ItemStack item, ItemStats stats) {
        if (item == null || stats == null) {
            return false;
        }

        // 최대값 제한 적용
        ItemStats limitedStats = applyMaxLimits(stats);

        return updateItemLore(item, limitedStats);
    }

    /**
     * 아이템에서 특정 스탯을 제거합니다.
     *
     * @param item 아이템
     * @param type 제거할 스탯 타입
     * @return 성공 여부
     */
    public boolean removeStat(ItemStack item, StatType type) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<String> currentLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        List<String> newLore = loreManager.removeStat(currentLore, type);

        meta.setLore(newLore);
        item.setItemMeta(meta);
        return true;
    }

    /**
     * 아이템에서 모든 스탯을 제거합니다.
     *
     * @param item 아이템
     * @return 성공 여부
     */
    public boolean clearStats(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<String> currentLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        List<String> newLore = loreManager.removeAllStats(currentLore);

        meta.setLore(newLore.isEmpty() ? null : newLore);
        item.setItemMeta(meta);
        return true;
    }

    /**
     * 아이템의 로어를 업데이트합니다.
     *
     * @param item     아이템
     * @param newStats 새 스탯
     * @return 성공 여부
     */
    private boolean updateItemLore(ItemStack item, ItemStats newStats) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = org.bukkit.Bukkit.getItemFactory().getItemMeta(item.getType());
            if (meta == null) {
                return false;
            }
        }

        List<String> currentLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // 기존 스탯 로어가 있으면 찾아서 그 위치에 업데이트
        // 없으면 맨 위에 추가 (인덱스 0)
        List<String> newLore = loreManager.addOrUpdateStats(currentLore, newStats, 0);

        meta.setLore(newLore.isEmpty() ? null : newLore);
        item.setItemMeta(meta);
        return true;
    }

    /**
     * 요청 값을 설정값(최대/최소) 기준으로 보정합니다.
     */
    public double clampStatValue(StatType type, double value) {
        if (type == null) {
            return Math.max(0, value);
        }

        // 음수 방지
        double clamped = Math.max(0, value);

        // 최대값 제한
        double maxValue = configManager.getMaxStat(type);
        if (maxValue > 0 && clamped > maxValue) {
            clamped = maxValue;
        }

        return clamped;
    }

    /**
     * 스탯에 최대값 제한을 적용합니다.
     *
     * @param stats 원본 스탯
     * @return 제한이 적용된 스탯
     */
    private ItemStats applyMaxLimits(ItemStats stats) {
        ItemStats.Builder builder = ItemStats.builder();

        for (StatType type : StatType.values()) {
            double value = clampStatValue(type, stats.getStat(type));

            switch (type) {
                case DAMAGE -> builder.damage(value);
                case DEFENSE -> builder.defense(value);
                case HEALTH -> builder.health(value);
                case LIFESTEAL -> builder.lifesteal(value);
                case CRIT_CHANCE -> builder.critChance(value);
                case CRIT_DAMAGE -> builder.critDamage(value);
                case DODGE -> builder.dodge(value);
            }
        }

        return builder.build();
    }

    /**
     * 아이템이 스탯을 가지고 있는지 확인합니다.
     *
     * @param item 아이템
     * @return 스탯이 있으면 true
     */
    public boolean hasStats(ItemStack item) {
        return !parseStats(item).isEmpty();
    }

    /**
     * LoreManager를 반환합니다.
     *
     * @return LoreManager
     */
    public LoreManager getLoreManager() {
        return loreManager;
    }
}
