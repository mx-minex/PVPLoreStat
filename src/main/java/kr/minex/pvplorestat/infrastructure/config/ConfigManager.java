package kr.minex.pvplorestat.infrastructure.config;

import kr.minex.pvplorestat.domain.model.StatType;
import kr.minex.pvplorestat.domain.service.DamageCalculator;
import kr.minex.pvplorestat.infrastructure.lore.LoreTemplate;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 설정 관리자
 * <p>
 * config.yml을 로드하고 캐싱하여 빠른 접근을 제공합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class ConfigManager {

    private final JavaPlugin plugin;

    // 캐시된 설정값
    private int updateInterval;
    private boolean pvpOnly;
    private boolean debug;

    // 스탯 설정
    private DamageCalculator.Config damageConfig;
    private double baseHealth;
    private final Map<StatType, Double> maxStats = new EnumMap<>(StatType.class);

    // 무기 패턴
    private final List<Pattern> weaponPatterns = new ArrayList<>();

    // 로어 템플릿
    private LoreTemplate loreTemplate;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * 설정을 리로드합니다.
     */
    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        // 일반 설정
        updateInterval = Math.max(1, config.getInt("settings.update-interval", 10));
        pvpOnly = config.getBoolean("settings.pvp-only", true);
        debug = config.getBoolean("settings.debug", false);

        // 스탯 계산 설정
        double damageDivisor = config.getDouble("stats.damage.divisor", 2.0);
        double defenseDivisor = config.getDouble("stats.defense.divisor", 2.0);
        double critDamageDivisor = config.getDouble("stats.critdamage.divisor", 2.0);
        damageConfig = new DamageCalculator.Config(damageDivisor, defenseDivisor, critDamageDivisor);

        baseHealth = config.getDouble("stats.health.base", 20.0);
        if (baseHealth < 1.0) {
            baseHealth = 20.0;
        }

        // 최대값 설정
        maxStats.clear();
        maxStats.put(StatType.DAMAGE, config.getDouble("stats.damage.max", 0));
        maxStats.put(StatType.DEFENSE, config.getDouble("stats.defense.max", 0));
        maxStats.put(StatType.HEALTH, config.getDouble("stats.health.max", 0));
        maxStats.put(StatType.LIFESTEAL, config.getDouble("stats.lifesteal.max", 100));
        maxStats.put(StatType.CRIT_CHANCE, config.getDouble("stats.critchance.max", 100));
        maxStats.put(StatType.CRIT_DAMAGE, config.getDouble("stats.critdamage.max", 0));
        maxStats.put(StatType.DODGE, config.getDouble("stats.dodge.max", 80));

        // 무기 패턴 로드
        loadWeaponPatterns(config.getStringList("weapons"));

        // 로어 템플릿 로드
        loadLoreTemplate(config);

        if (debug) {
            plugin.getLogger().info("[Debug] 설정 로드 완료");
        }
    }

    /**
     * 무기 패턴을 로드합니다.
     */
    private void loadWeaponPatterns(List<String> weaponList) {
        weaponPatterns.clear();

        if (weaponList == null || weaponList.isEmpty()) {
            return;
        }

        for (String weapon : weaponList) {
            if (weapon == null || weapon.isBlank()) {
                continue;
            }
            try {
                weaponPatterns.add(compileGlobPattern(weapon));
            } catch (Exception e) {
                plugin.getLogger().warning("무기 패턴을 로드할 수 없습니다: " + weapon + " (" + e.getMessage() + ")");
            }
        }
    }

    /**
     * config.yml의 glob 패턴(*, ?)을 안전한 정규식으로 컴파일합니다.
     * <p>
     * 운영자가 실수로 정규식 메타 문자를 넣더라도 리로드가 깨지지 않도록 방어합니다.
     * </p>
     */
    private static Pattern compileGlobPattern(String glob) {
        String upper = glob.toUpperCase(Locale.ROOT);
        StringBuilder regex = new StringBuilder("^");

        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append('.');
                default -> {
                    if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
                        regex.append('\\');
                    }
                    regex.append(c);
                }
            }
        }

        regex.append('$');
        return Pattern.compile(regex.toString());
    }

    /**
     * 로어 템플릿을 로드합니다.
     */
    private void loadLoreTemplate(FileConfiguration config) {
        LoreTemplate.Builder builder = LoreTemplate.builder();

        // 구분선 설정
        builder.separatorEnabled(config.getBoolean("lore.separator.enabled", true));
        builder.separatorTop(config.getString("lore.separator.top", "&8&m─────&r &6✦ 스탯 &8&m─────"));
        builder.separatorBottom(config.getString("lore.separator.bottom", "&8&m──────────────────"));

        // 스탯 형식 로드
        for (StatType type : StatType.values()) {
            String format = config.getString("lore.format." + type.getConfigKey());
            if (format != null) {
                builder.format(type, format);
            }
        }

        // 순서 로드
        List<String> orderConfig = config.getStringList("lore.order");
        if (!orderConfig.isEmpty()) {
            List<StatType> order = new ArrayList<>();
            for (String key : orderConfig) {
                StatType.findByKeyword(key).ifPresent(order::add);
            }
            builder.order(order);
        }

        loreTemplate = builder.build();
    }

    // ===== Getters =====

    /**
     * 스탯 업데이트 간격 (틱)
     */
    public int getUpdateInterval() {
        return updateInterval;
    }

    /**
     * PVP만 적용 여부
     */
    public boolean isPvpOnly() {
        return pvpOnly;
    }

    /**
     * 디버그 모드 여부
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * 데미지 계산 설정
     */
    public DamageCalculator.Config getDamageConfig() {
        return damageConfig;
    }

    /**
     * 기본 최대 체력
     */
    public double getBaseHealth() {
        return baseHealth;
    }

    /**
     * 스탯 최대값 조회
     *
     * @param type 스탯 타입
     * @return 최대값 (0이면 무제한)
     */
    public double getMaxStat(StatType type) {
        return maxStats.getOrDefault(type, 0.0);
    }

    /**
     * 해당 재료가 무기인지 확인합니다.
     *
     * @param material 아이템 재료
     * @return 무기이면 true
     */
    public boolean isWeapon(Material material) {
        if (material == null) {
            return false;
        }

        String name = material.name();
        for (Pattern pattern : weaponPatterns) {
            if (pattern.matcher(name).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 로어 템플릿
     */
    public LoreTemplate getLoreTemplate() {
        return loreTemplate;
    }

    /**
     * 원본 설정 파일
     */
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
