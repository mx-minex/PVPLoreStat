package kr.minex.pvplorestat.application;

import kr.minex.pvplorestat.domain.model.EquipmentSlot;
import kr.minex.pvplorestat.domain.model.ItemStats;
import kr.minex.pvplorestat.domain.model.PlayerStats;
import kr.minex.pvplorestat.infrastructure.cache.PlayerStatsCache;
import kr.minex.pvplorestat.infrastructure.config.ConfigManager;
import kr.minex.pvplorestat.infrastructure.monitoring.PluginMetrics;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * 플레이어 스탯 서비스
 * <p>
 * 플레이어의 장비 스탯을 계산하고 캐싱합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class PlayerStatsService {

    private final ItemLoreService itemLoreService;
    private final PlayerStatsCache statsCache;
    private final ConfigManager configManager;
    private final PluginMetrics metrics;
    private final Logger logger;

    public PlayerStatsService(ItemLoreService itemLoreService,
                              PlayerStatsCache statsCache,
                              ConfigManager configManager,
                              PluginMetrics metrics,
                              Logger logger) {
        this.itemLoreService = Objects.requireNonNull(itemLoreService, "itemLoreService");
        this.statsCache = Objects.requireNonNull(statsCache, "statsCache");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * 플레이어의 모든 장비 스탯을 계산하고 캐싱합니다.
     *
     * @param player 플레이어
     * @return 계산된 스탯
     */
    public PlayerStats calculateAndCache(Player player) {
        long start = System.nanoTime();
        PlayerStats stats = calculate(player);
        statsCache.put(stats);

        // 최대 체력 업데이트
        updateMaxHealth(player, stats);

        metrics.recordPlayerStatCalc(System.nanoTime() - start);

        return stats;
    }

    /**
     * 플레이어의 모든 장비 스탯을 계산합니다 (캐싱 없음).
     *
     * @param player 플레이어
     * @return 계산된 스탯
     */
    public PlayerStats calculate(Player player) {
        UUID playerId = player.getUniqueId();
        EntityEquipment equipment = player.getEquipment();

        if (equipment == null) {
            return PlayerStats.empty(playerId);
        }

        PlayerStats stats = PlayerStats.empty(playerId);

        // 투구
        ItemStack helmet = equipment.getHelmet();
        if (helmet != null && helmet.getType() != Material.AIR) {
            stats = stats.withEquipmentStats(EquipmentSlot.HELMET,
                    clampStats(itemLoreService.parseStats(helmet)));
        }

        // 갑옷
        ItemStack chestplate = equipment.getChestplate();
        if (chestplate != null && chestplate.getType() != Material.AIR) {
            stats = stats.withEquipmentStats(EquipmentSlot.CHESTPLATE,
                    clampStats(itemLoreService.parseStats(chestplate)));
        }

        // 레깅스
        ItemStack leggings = equipment.getLeggings();
        if (leggings != null && leggings.getType() != Material.AIR) {
            stats = stats.withEquipmentStats(EquipmentSlot.LEGGINGS,
                    clampStats(itemLoreService.parseStats(leggings)));
        }

        // 부츠
        ItemStack boots = equipment.getBoots();
        if (boots != null && boots.getType() != Material.AIR) {
            stats = stats.withEquipmentStats(EquipmentSlot.BOOTS,
                    clampStats(itemLoreService.parseStats(boots)));
        }

        // 주무기 (무기로 인식되는 아이템만)
        ItemStack mainHand = equipment.getItemInMainHand();
        if (mainHand != null && mainHand.getType() != Material.AIR
                && configManager.isWeapon(mainHand.getType())) {
            stats = stats.withEquipmentStats(EquipmentSlot.MAIN_HAND,
                    clampStats(itemLoreService.parseStats(mainHand)));
        }

        // 보조무기 (오프핸드)
        ItemStack offHand = equipment.getItemInOffHand();
        if (offHand != null && offHand.getType() != Material.AIR
                && configManager.isWeapon(offHand.getType())) {
            stats = stats.withEquipmentStats(EquipmentSlot.OFF_HAND,
                    clampStats(itemLoreService.parseStats(offHand)));
        }

        return stats;
    }

    private ItemStats clampStats(ItemStats stats) {
        if (stats == null || stats.isEmpty()) {
            return ItemStats.empty();
        }

        ItemStats.Builder builder = ItemStats.builder();
        for (var type : kr.minex.pvplorestat.domain.model.StatType.values()) {
            double value = itemLoreService.clampStatValue(type, stats.getStat(type));
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
     * 캐시된 플레이어 스탯을 조회합니다.
     *
     * @param playerId 플레이어 UUID
     * @return 캐시된 스탯 (없으면 빈 스탯)
     */
    public PlayerStats getStats(UUID playerId) {
        return statsCache.getOrEmpty(playerId);
    }

    /**
     * 플레이어 스탯을 캐시에서 제거합니다.
     *
     * @param playerId 플레이어 UUID
     */
    public void removeStats(UUID playerId) {
        statsCache.remove(playerId);
    }

    /**
     * 모든 캐시를 비웁니다.
     */
    public void clearCache() {
        statsCache.clear();
    }

    /**
     * 플레이어의 최대 체력을 업데이트합니다.
     *
     * @param player 플레이어
     * @param stats  플레이어 스탯
     */
    public void updateMaxHealth(Player player, PlayerStats stats) {
        double baseHealth = configManager.getBaseHealth();

        double newMaxHealth = stats.calculateMaxHealth(baseHealth);
        if (Double.isNaN(newMaxHealth) || Double.isInfinite(newMaxHealth)) {
            logger.warning("잘못된 최대 체력 계산 결과를 감지했습니다. base=" + baseHealth +
                    ", totalHealthStat=" + stats.getTotalStats().getHealth());
            return;
        }
        if (newMaxHealth < 1.0) {
            newMaxHealth = 1.0;
        }

        // Attribute를 통해 최대 체력 설정
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            double currentMax = attribute.getBaseValue();

            if (Math.abs(currentMax - newMaxHealth) > 1e-9) {
                attribute.setBaseValue(newMaxHealth);

                // 현재 체력이 최대 체력을 초과하지 않도록
                if (player.getHealth() > newMaxHealth) {
                    player.setHealth(newMaxHealth);
                }
            }
        }
    }

    /**
     * 플레이어의 최대 체력을 기본값으로 리셋합니다.
     *
     * @param player 플레이어
     */
    public void resetMaxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(configManager.getBaseHealth());
        }
    }

    /**
     * 특정 장비의 스탯만 업데이트합니다.
     *
     * @param player 플레이어
     * @param slot   장비 슬롯
     * @param item   장비 아이템
     */
    public void updateEquipmentSlot(Player player, EquipmentSlot slot, ItemStack item) {
        UUID playerId = player.getUniqueId();
        PlayerStats currentStats = statsCache.getOrEmpty(playerId);

        ItemStats newSlotStats = ItemStats.empty();
        if (item != null && item.getType() != Material.AIR) {
            // 무기 슬롯인 경우 무기 타입 체크
            if (slot.isWeapon() && !configManager.isWeapon(item.getType())) {
                newSlotStats = ItemStats.empty();
            } else {
                newSlotStats = itemLoreService.parseStats(item);
            }
        }

        PlayerStats newStats = currentStats.withEquipmentStats(slot, newSlotStats);
        statsCache.put(newStats);

        // 최대 체력 업데이트
        updateMaxHealth(player, newStats);
    }

    /**
     * 스탯 캐시를 반환합니다.
     *
     * @return PlayerStatsCache
     */
    public PlayerStatsCache getCache() {
        return statsCache;
    }
}
