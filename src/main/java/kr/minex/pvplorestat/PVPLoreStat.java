package kr.minex.pvplorestat;

import kr.minex.pvplorestat.application.CombatService;
import kr.minex.pvplorestat.application.ItemLoreService;
import kr.minex.pvplorestat.application.PlayerStatsService;
import kr.minex.pvplorestat.infrastructure.cache.PlayerStatsCache;
import kr.minex.pvplorestat.infrastructure.config.ConfigManager;
import kr.minex.pvplorestat.infrastructure.config.MessageManager;
import kr.minex.pvplorestat.infrastructure.lore.LoreManager;
import kr.minex.pvplorestat.infrastructure.monitoring.MetricsLogTask;
import kr.minex.pvplorestat.infrastructure.monitoring.PluginMetrics;
import kr.minex.pvplorestat.presentation.command.PlsCommand;
import kr.minex.pvplorestat.presentation.listener.CombatListener;
import kr.minex.pvplorestat.presentation.listener.EquipmentListener;
import kr.minex.pvplorestat.presentation.task.StatUpdateTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * PVPLoreStat 메인 플러그인 클래스
 * <p>
 * 아이템 로어 기반 PVP 스탯 시스템을 제공합니다.
 * </p>
 *
 * @author Minex
 * @version 1.0.0
 * @since 1.0.0
 */
public class PVPLoreStat extends JavaPlugin {

    private static PVPLoreStat instance;

    // 인프라
    private ConfigManager configManager;
    private MessageManager messageManager;
    private LoreManager loreManager;
    private PlayerStatsCache statsCache;
    private PluginMetrics metrics;

    // 애플리케이션
    private ItemLoreService itemLoreService;
    private PlayerStatsService playerStatsService;
    private CombatService combatService;

    // 태스크
    private BukkitTask statUpdateTask;
    private BukkitTask metricsLogTask;

    // 리스너(리로드 시 중복 등록 방지)
    private CombatListener combatListener;
    private EquipmentListener equipmentListener;

    @Override
    public void onEnable() {
        instance = this;

        try {
            // 설정 파일 초기화
            saveDefaultConfig();
            saveResource("messages.yml", false);

            // 매니저 초기화
            initializeManagers();

            // 서비스 초기화
            initializeServices();

            // 이벤트 리스너 등록
            registerListeners();

            // 명령어 등록
            registerCommands();

            // 태스크 시작
            startTasks();

            // 이미 접속해 있는 플레이어 처리 (리로드 대응)
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerStatsService.calculateAndCache(player);
            }

            getLogger().info("========================================");
            getLogger().info("  PVPLoreStat Plugin v" + getDescription().getVersion());
            getLogger().info("  Created by Minex");
            getLogger().info("  https://github.com/mx-minex");
            getLogger().info("========================================");
        } catch (Exception e) {
            getLogger().severe("플러그인 초기화 중 치명적인 오류가 발생했습니다. 플러그인을 비활성화합니다.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // 태스크 정리
        if (statUpdateTask != null) {
            statUpdateTask.cancel();
        }
        if (metricsLogTask != null) {
            metricsLogTask.cancel();
        }

        // 모든 플레이어 체력 리셋
        if (playerStatsService != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerStatsService.resetMaxHealth(player);
            }
        }

        // 캐시 정리
        if (statsCache != null) {
            statsCache.clear();
        }

        // 리스너 정리 (리로드/비활성화 시 안전)
        HandlerList.unregisterAll(this);

        instance = null;
        getLogger().info("플러그인이 비활성화되었습니다.");
    }

    /**
     * 매니저를 초기화합니다.
     */
    private void initializeManagers() {
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        loreManager = new LoreManager(configManager.getLoreTemplate());
        statsCache = new PlayerStatsCache();
        metrics = new PluginMetrics();
    }

    /**
     * 서비스를 초기화합니다.
     */
    private void initializeServices() {
        itemLoreService = new ItemLoreService(loreManager, configManager, metrics, getLogger());
        playerStatsService = new PlayerStatsService(itemLoreService, statsCache, configManager, metrics, getLogger());
        combatService = new CombatService(playerStatsService, configManager, messageManager, metrics, getLogger());
    }

    /**
     * 이벤트 리스너를 등록합니다.
     */
    private void registerListeners() {
        // 리로드 시 중복 등록 방지
        HandlerList.unregisterAll(this);

        combatListener = new CombatListener(combatService, configManager, getLogger());
        equipmentListener = new EquipmentListener(playerStatsService, this, getLogger());

        getServer().getPluginManager().registerEvents(combatListener, this);
        getServer().getPluginManager().registerEvents(equipmentListener, this);
    }

    /**
     * 명령어를 등록합니다.
     */
    private void registerCommands() {
        PlsCommand plsCommand = new PlsCommand(this, messageManager, itemLoreService, playerStatsService);

        var command = getCommand("pvplorestat");
        if (command != null) {
            command.setExecutor(plsCommand);
            command.setTabCompleter(plsCommand);
        }
    }

    /**
     * 주기적 태스크를 시작합니다.
     */
    private void startTasks() {
        int interval = configManager.getUpdateInterval();
        statUpdateTask = new StatUpdateTask(playerStatsService, metrics, getLogger(), configManager)
                .runTaskTimer(this, interval, interval);

        // 디버그 모드에서만 주기적으로 메트릭 로그 출력
        if (configManager.isDebug()) {
            int logIntervalTicks = 20 * 60 * 5; // 5분
            metricsLogTask = new MetricsLogTask(metrics, getLogger())
                    .runTaskTimer(this, logIntervalTicks, logIntervalTicks);
        }
    }

    /**
     * 설정을 리로드합니다.
     */
    public void reload() {
        try {
            // 태스크 중지
            if (statUpdateTask != null) {
                statUpdateTask.cancel();
            }
            if (metricsLogTask != null) {
                metricsLogTask.cancel();
            }

            // 설정 리로드
            configManager.reload();
            messageManager.reload();

            // LoreManager 재생성
            loreManager = new LoreManager(configManager.getLoreTemplate());

            // 서비스 전체 재생성 (의존성 갱신)
            itemLoreService = new ItemLoreService(loreManager, configManager, metrics, getLogger());
            playerStatsService = new PlayerStatsService(itemLoreService, statsCache, configManager, metrics, getLogger());
            combatService = new CombatService(playerStatsService, configManager, messageManager, metrics, getLogger());

            // 리스너 재등록 (새 서비스 참조를 위해)
            registerListeners();

            // 명령어 재등록
            registerCommands();

            // 태스크 재시작
            startTasks();

            // 모든 플레이어 스탯 재계산
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerStatsService.calculateAndCache(player);
            }

            if (configManager.isDebug()) {
                getLogger().info("[Debug] 설정 리로드 완료");
            }
        } catch (Exception e) {
            getLogger().severe("설정 리로드 중 오류가 발생했습니다. 자세한 내용은 스택트레이스를 확인하세요.");
            e.printStackTrace();
        }
    }

    /**
     * 플러그인 인스턴스를 반환합니다.
     *
     * @return 플러그인 인스턴스
     */
    public static PVPLoreStat getInstance() {
        return instance;
    }

    // ===== Getters =====

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public LoreManager getLoreManager() {
        return loreManager;
    }

    public ItemLoreService getItemLoreService() {
        return itemLoreService;
    }

    public PlayerStatsService getPlayerStatsService() {
        return playerStatsService;
    }

    public CombatService getCombatService() {
        return combatService;
    }
}
