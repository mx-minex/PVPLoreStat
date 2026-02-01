package kr.minex.pvplorestat.presentation.command;

import kr.minex.pvplorestat.PVPLoreStat;
import kr.minex.pvplorestat.application.ItemLoreService;
import kr.minex.pvplorestat.application.PlayerStatsService;
import kr.minex.pvplorestat.domain.model.ItemStats;
import kr.minex.pvplorestat.domain.model.PlayerStats;
import kr.minex.pvplorestat.domain.model.StatType;
import kr.minex.pvplorestat.infrastructure.config.MessageManager;
import kr.minex.pvplorestat.presentation.gui.LoreEditGui;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * /pls 메인 명령어 핸들러
 * <p>
 * 모든 PVPLoreStat 명령어를 처리합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class PlsCommand implements CommandExecutor, TabCompleter {

    private final PVPLoreStat plugin;
    private final MessageManager messageManager;
    private final ItemLoreService itemLoreService;
    private final PlayerStatsService playerStatsService;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "set", "remove", "clear", "edit", "info", "check", "reload", "help"
    );

    private static final List<String> STAT_TYPES = Arrays.stream(StatType.values())
            .map(StatType::getConfigKey)
            .collect(Collectors.toList());

    public PlsCommand(PVPLoreStat plugin, MessageManager messageManager,
                      ItemLoreService itemLoreService, PlayerStatsService playerStatsService) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.itemLoreService = itemLoreService;
        this.playerStatsService = playerStatsService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleHelp(sender);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set" -> {
                return handleSet(sender, args);
            }
            case "remove" -> {
                return handleRemove(sender, args);
            }
            case "clear" -> {
                return handleClear(sender);
            }
            case "edit" -> {
                return handleEdit(sender);
            }
            case "info" -> {
                return handleInfo(sender);
            }
            case "check" -> {
                return handleCheck(sender);
            }
            case "reload" -> {
                return handleReload(sender);
            }
            case "help" -> {
                return handleHelp(sender);
            }
            default -> {
                messageManager.send(sender, "commands.help.header");
                return true;
            }
        }
    }

    /**
     * /pls set <스탯> <값>
     */
    private boolean handleSet(CommandSender sender, String[] args) {
        if (!checkPlayerAndPermission(sender, "pvplorestat.set")) {
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 3) {
            messageManager.send(sender, "commands.help.commands");
            return true;
        }

        // 아이템 확인
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            messageManager.send(sender, "common.no-item");
            return true;
        }

        // 스탯 타입 파싱
        String statKey = args[1].toLowerCase();
        StatType statType = StatType.findByKeyword(statKey).orElse(null);
        if (statType == null) {
            messageManager.send(sender, "common.invalid-stat");
            return true;
        }

        // 값 파싱
        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            messageManager.send(sender, "common.invalid-number");
            return true;
        }

        // 기존 스탯 확인
        ItemStats currentStats = itemLoreService.parseStats(item);
        double oldValue = currentStats.getStat(statType);

        // 스탯 설정
        ItemLoreService.StatApplyResult applyResult = itemLoreService.setStat(item, statType, value);
        if (!applyResult.success()) {
            messageManager.send(sender, "common.operation-failed");
            return true;
        }
        double applied = applyResult.appliedValue();

        // 메시지
        if (oldValue > 0) {
            messageManager.send(sender, "commands.set.updated",
                    "stat", statType.getDisplayName(),
                    "old", oldValue,
                    "new", applied);
        } else {
            messageManager.send(sender, "commands.set.success",
                    "stat", statType.getDisplayName(),
                    "value", applied);
        }

        return true;
    }

    /**
     * /pls remove <스탯>
     */
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!checkPlayerAndPermission(sender, "pvplorestat.remove")) {
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            messageManager.send(sender, "commands.help.commands");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            messageManager.send(sender, "common.no-item");
            return true;
        }

        String statKey = args[1].toLowerCase();
        StatType statType = StatType.findByKeyword(statKey).orElse(null);
        if (statType == null) {
            messageManager.send(sender, "common.invalid-stat");
            return true;
        }

        // 스탯 존재 확인
        ItemStats currentStats = itemLoreService.parseStats(item);
        if (!currentStats.hasStat(statType)) {
            messageManager.send(sender, "commands.remove.not-found");
            return true;
        }

        itemLoreService.removeStat(item, statType);
        messageManager.send(sender, "commands.remove.success",
                "stat", statType.getDisplayName());

        return true;
    }

    /**
     * /pls clear
     */
    private boolean handleClear(CommandSender sender) {
        if (!checkPlayerAndPermission(sender, "pvplorestat.clear")) {
            return true;
        }

        Player player = (Player) sender;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            messageManager.send(sender, "common.no-item");
            return true;
        }

        if (!itemLoreService.hasStats(item)) {
            messageManager.send(sender, "commands.clear.no-stats");
            return true;
        }

        itemLoreService.clearStats(item);
        messageManager.send(sender, "commands.clear.success");

        return true;
    }

    /**
     * /pls edit
     */
    private boolean handleEdit(CommandSender sender) {
        if (!checkPlayerAndPermission(sender, "pvplorestat.edit")) {
            return true;
        }

        Player player = (Player) sender;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            messageManager.send(sender, "common.no-item");
            return true;
        }

        LoreEditGui gui = new LoreEditGui(plugin, messageManager, itemLoreService, player, item);
        gui.open();

        return true;
    }

    /**
     * /pls info
     */
    private boolean handleInfo(CommandSender sender) {
        if (!checkPlayerAndPermission(sender, "pvplorestat.info")) {
            return true;
        }

        Player player = (Player) sender;
        PlayerStats stats = playerStatsService.getStats(player.getUniqueId());

        messageManager.sendRaw(sender, "commands.info.header", "player", player.getName());

        if (stats.getTotalStats().isEmpty()) {
            messageManager.sendRaw(sender, "commands.info.no-stats");
        } else {
            Map<StatType, Double> nonZeroStats = stats.getTotalStats().getNonZeroStats();
            for (Map.Entry<StatType, Double> entry : nonZeroStats.entrySet()) {
                String suffix = entry.getKey().isPercent() ? "%" : "";
                messageManager.sendRaw(sender, "commands.info.stat-line",
                        "name", entry.getKey().getDisplayName(),
                        "value", entry.getValue() + suffix);
            }
        }

        messageManager.sendRaw(sender, "commands.info.footer");

        return true;
    }

    /**
     * /pls check
     */
    private boolean handleCheck(CommandSender sender) {
        if (!checkPlayerAndPermission(sender, "pvplorestat.check")) {
            return true;
        }

        Player player = (Player) sender;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            messageManager.send(sender, "common.no-item");
            return true;
        }

        ItemStats stats = itemLoreService.parseStats(item);

        messageManager.sendRaw(sender, "commands.check.header");

        if (stats.isEmpty()) {
            messageManager.sendRaw(sender, "commands.check.no-stats");
        } else {
            Map<StatType, Double> nonZeroStats = stats.getNonZeroStats();
            for (Map.Entry<StatType, Double> entry : nonZeroStats.entrySet()) {
                String suffix = entry.getKey().isPercent() ? "%" : "";
                messageManager.sendRaw(sender, "commands.check.stat-line",
                        "name", entry.getKey().getDisplayName(),
                        "value", entry.getValue() + suffix);
            }
        }

        messageManager.sendRaw(sender, "commands.check.footer");

        return true;
    }

    /**
     * /pls reload
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("pvplorestat.reload")) {
            messageManager.send(sender, "common.no-permission");
            return true;
        }

        plugin.reload();
        messageManager.send(sender, "commands.reload.success");

        return true;
    }

    /**
     * /pls help
     */
    private boolean handleHelp(CommandSender sender) {
        messageManager.sendRaw(sender, "commands.help.header");
        for (String line : messageManager.getList("commands.help.commands")) {
            sender.sendMessage(line);
        }
        messageManager.sendRaw(sender, "commands.help.footer");
        return true;
    }

    /**
     * 플레이어 및 권한 체크
     */
    private boolean checkPlayerAndPermission(CommandSender sender, String permission) {
        if (!(sender instanceof Player)) {
            messageManager.send(sender, "common.player-only");
            return false;
        }

        if (!sender.hasPermission(permission)) {
            messageManager.send(sender, "common.no-permission");
            return false;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            completions.addAll(SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(input))
                    .toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();

            if (subCommand.equals("set") || subCommand.equals("remove")) {
                completions.addAll(STAT_TYPES.stream()
                        .filter(s -> s.startsWith(input))
                        .toList());
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("set")) {
                // 숫자 제안
                completions.addAll(Arrays.asList("10", "50", "100"));
            }
        }

        return completions;
    }
}
