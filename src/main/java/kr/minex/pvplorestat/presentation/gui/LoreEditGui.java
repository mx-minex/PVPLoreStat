package kr.minex.pvplorestat.presentation.gui;

import kr.minex.pvplorestat.PVPLoreStat;
import kr.minex.pvplorestat.application.ItemLoreService;
import kr.minex.pvplorestat.domain.model.ItemStats;
import kr.minex.pvplorestat.domain.model.StatType;
import kr.minex.pvplorestat.infrastructure.config.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 로어 편집 GUI
 * <p>
 * 스탯을 GUI로 편집할 수 있는 인터페이스를 제공합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class LoreEditGui implements Listener {

    private final PVPLoreStat plugin;
    private final MessageManager messageManager;
    private final ItemLoreService itemLoreService;
    private final Player player;
    private final ItemStack targetItem;
    private final Inventory inventory;

    // 편집 중인 스탯 값들
    private final Map<StatType, Double> editingStats = new EnumMap<>(StatType.class);

    // 이벤트 중복 등록 방지
    private volatile boolean registered = false;

    // GUI 슬롯 매핑
    private static final Map<Integer, StatType> SLOT_TO_STAT = new HashMap<>();
    private static final int CLEAR_SLOT = 49;
    private static final int SAVE_SLOT = 53;
    private static final int CANCEL_SLOT = 45;

    static {
        SLOT_TO_STAT.put(10, StatType.DAMAGE);
        SLOT_TO_STAT.put(11, StatType.DEFENSE);
        SLOT_TO_STAT.put(12, StatType.HEALTH);
        SLOT_TO_STAT.put(13, StatType.LIFESTEAL);
        SLOT_TO_STAT.put(14, StatType.CRIT_CHANCE);
        SLOT_TO_STAT.put(15, StatType.CRIT_DAMAGE);
        SLOT_TO_STAT.put(16, StatType.DODGE);
    }

    public LoreEditGui(PVPLoreStat plugin, MessageManager messageManager,
                       ItemLoreService itemLoreService, Player player, ItemStack targetItem) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.itemLoreService = itemLoreService;
        this.player = player;
        this.targetItem = targetItem;

        // 현재 스탯 로드
        ItemStats currentStats = itemLoreService.parseStats(targetItem);
        for (StatType type : StatType.values()) {
            editingStats.put(type, currentStats.getStat(type));
        }

        // GUI 생성
        String title = ChatColor.translateAlternateColorCodes('&',
                messageManager.get("gui.title"));
        this.inventory = Bukkit.createInventory(null, 54, title);

        setupInventory();
    }

    /**
     * GUI를 설정합니다.
     */
    private void setupInventory() {
        // 배경 채우기
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        // 스탯 아이템 배치
        updateStatItems();

        // 초기화 버튼
        inventory.setItem(CLEAR_SLOT, createItem(
                Material.BARRIER,
                messageManager.get("gui.clear-item.name"),
                messageManager.getList("gui.clear-item.lore")
        ));

        // 저장 버튼
        inventory.setItem(SAVE_SLOT, createItem(
                Material.EMERALD,
                messageManager.get("gui.save-item.name"),
                messageManager.getList("gui.save-item.lore")
        ));

        // 취소 버튼
        inventory.setItem(CANCEL_SLOT, createItem(
                Material.REDSTONE,
                messageManager.get("gui.cancel-item.name"),
                messageManager.getList("gui.cancel-item.lore")
        ));
    }

    /**
     * 스탯 아이템들을 업데이트합니다.
     */
    private void updateStatItems() {
        for (Map.Entry<Integer, StatType> entry : SLOT_TO_STAT.entrySet()) {
            int slot = entry.getKey();
            StatType type = entry.getValue();
            double value = editingStats.getOrDefault(type, 0.0);

            Material material = getStatMaterial(type);
            String name = messageManager.get("gui.stat-item.name", "name", type.getDisplayName());
            List<String> lore = messageManager.getList("gui.stat-item.lore",
                    "value", formatValue(value, type));

            inventory.setItem(slot, createItem(material, name, lore));
        }
    }

    /**
     * 스탯 타입에 맞는 Material을 반환합니다.
     */
    private Material getStatMaterial(StatType type) {
        return switch (type) {
            case DAMAGE -> Material.DIAMOND_SWORD;
            case DEFENSE -> Material.SHIELD;
            case HEALTH -> Material.GOLDEN_APPLE;
            case LIFESTEAL -> Material.GHAST_TEAR;
            case CRIT_CHANCE -> Material.LIGHTNING_ROD;
            case CRIT_DAMAGE -> Material.TNT;
            case DODGE -> Material.FEATHER;
        };
    }

    /**
     * GUI를 엽니다.
     */
    public void open() {
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.openInventory(inventory);
    }

    /**
     * GUI를 닫습니다.
     */
    private void close() {
        HandlerList.unregisterAll(this);
        registered = false;
        player.closeInventory();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory() != inventory) {
            return;
        }
        if (event.getWhoClicked() != player) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();
        ClickType clickType = event.getClick();
        int topSize = event.getView().getTopInventory().getSize();

        // 하단 인벤토리 클릭도 모두 차단 (아이템 이동/복사 방지)
        if (slot >= topSize) {
            return;
        }

        // 스탯 슬롯 클릭
        if (SLOT_TO_STAT.containsKey(slot)) {
            StatType type = SLOT_TO_STAT.get(slot);
            handleStatClick(type, clickType);
            return;
        }

        // 초기화 버튼
        if (slot == CLEAR_SLOT) {
            for (StatType type : StatType.values()) {
                editingStats.put(type, 0.0);
            }
            updateStatItems();
            return;
        }

        // 저장 버튼
        if (slot == SAVE_SLOT) {
            saveAndClose();
            return;
        }

        // 취소 버튼
        if (slot == CANCEL_SLOT) {
            close();
        }
    }

    /**
     * 스탯 클릭을 처리합니다.
     */
    private void handleStatClick(StatType type, ClickType clickType) {
        double current = editingStats.getOrDefault(type, 0.0);
        double increment = clickType.isShiftClick() ? 10 : 1;

        switch (clickType) {
            case LEFT, SHIFT_LEFT -> {
                editingStats.put(type, itemLoreService.clampStatValue(type, current + increment));
            }
            case RIGHT, SHIFT_RIGHT -> {
                editingStats.put(type, itemLoreService.clampStatValue(type, Math.max(0, current - increment)));
            }
            default -> {
                // 그 외 클릭은 무시
            }
        }

        updateStatItems();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != inventory) {
            return;
        }
        if (event.getPlayer() != player) {
            return;
        }

        HandlerList.unregisterAll(this);
        registered = false;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer() != player) {
            return;
        }
        HandlerList.unregisterAll(this);
        registered = false;
    }

    /**
     * 저장하고 닫습니다.
     */
    private void saveAndClose() {
        // 스탯 빌드
        ItemStats.Builder builder = ItemStats.builder();
        for (Map.Entry<StatType, Double> entry : editingStats.entrySet()) {
            double value = entry.getValue();
            switch (entry.getKey()) {
                case DAMAGE -> builder.damage(value);
                case DEFENSE -> builder.defense(value);
                case HEALTH -> builder.health(value);
                case LIFESTEAL -> builder.lifesteal(value);
                case CRIT_CHANCE -> builder.critChance(value);
                case CRIT_DAMAGE -> builder.critDamage(value);
                case DODGE -> builder.dodge(value);
            }
        }

        ItemStats newStats = builder.build();

        boolean ok;
        if (newStats.isEmpty()) {
            ok = itemLoreService.clearStats(targetItem);
        } else {
            ok = itemLoreService.setStats(targetItem, newStats);
        }

        close();
        if (ok) {
            messageManager.send(player, "gui.saved");
        } else {
            messageManager.send(player, "gui.save-failed");
        }
    }

    /**
     * 값을 포맷팅합니다.
     */
    private String formatValue(double value, StatType type) {
        String suffix = type.isPercent() ? "%" : "";
        if (value == Math.floor(value)) {
            return (int) value + suffix;
        }
        return value + suffix;
    }

    /**
     * 아이템을 생성합니다.
     */
    private ItemStack createItem(Material material, String name) {
        return createItem(material, name, null);
    }

    /**
     * 아이템을 생성합니다.
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore.stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
