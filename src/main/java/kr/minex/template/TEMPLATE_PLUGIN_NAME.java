package kr.minex.template;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * TEMPLATE_PLUGIN_NAME 메인 플러그인 클래스
 *
 * @author Minex
 * @version 1.0.0
 * @since 1.0.0
 */
public class TEMPLATE_PLUGIN_NAME extends JavaPlugin {

    private static TEMPLATE_PLUGIN_NAME instance;

    @Override
    public void onEnable() {
        instance = this;

        // 설정 파일 초기화
        saveDefaultConfig();

        // 매니저 초기화
        initializeManagers();

        // 이벤트 리스너 등록
        registerListeners();

        // 명령어 등록
        registerCommands();

        getLogger().info("========================================");
        getLogger().info("  TEMPLATE_PLUGIN_NAME Plugin v" + getDescription().getVersion());
        getLogger().info("  Created by Minex");
        getLogger().info("  https://github.com/mx-minex");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        instance = null;
        getLogger().info("플러그인이 비활성화되었습니다!");
    }

    private void initializeManagers() {
        // TODO: 매니저 초기화
    }

    private void registerListeners() {
        // TODO: 이벤트 리스너 등록
        // getServer().getPluginManager().registerEvents(new ExampleListener(this), this);
    }

    private void registerCommands() {
        // TODO: 명령어 등록
        // getCommand("template").setExecutor(new ExampleCommand(this));
    }

    public static TEMPLATE_PLUGIN_NAME getInstance() {
        return instance;
    }
}
