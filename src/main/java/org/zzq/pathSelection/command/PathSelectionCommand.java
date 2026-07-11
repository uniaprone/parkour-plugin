package org.zzq.pathSelection.command;

import org.bukkit.Bukkit;
import org.zzq.pathSelection.*;
import org.zzq.pathSelection.database.CompletionRecord;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zzq.pathSelection.util.TimeFormatter;

import java.util.*;

public class PathSelectionCommand implements CommandExecutor{

    private final PathSelection plugin;
    private final LanguageManager language;

    public PathSelectionCommand(PathSelection plugin) {
        this.plugin = plugin;
        this.language = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                showHelp((Player) sender);
            } else {
                showConsoleHelp(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 重载命令可以由控制台执行
        if (subCommand.equals("reload")) {
            return handleReloadCommand(sender);
        }

        // 排行榜命令可以由控制台执行
        if (subCommand.equals("leaderboard") || subCommand.equals("lb")) {
            return handleLeaderboardCommand(sender, args);
        }

        // 奖励管理命令可以由控制台执行
        if (subCommand.equals("reward")) {
            return handleRewardCommand(sender, args);
        }

        // 其他命令需要玩家执行
        if (!(sender instanceof Player)) {
            sender.sendMessage(language.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("pathselection.pathselect")) {
            player.sendMessage(language.getMessage("no-permission"));
            return true;
        }

        switch (subCommand) {
            case "create":
                return handleCreateCommand(player, args);
            case "finish":
                return handleFinishCommand(player);
            case "edit":
                return handleEditCommand(player, args);
            case "unload":
                return handleUnloadCommand(player, args);
            case "deletepath":
                return handleDeletePathCommand(player, args);
            case "list":
                return handleListCommand(player);
            case "tool":
                return handleToolCommand(player, args);
            case "start":
                return handleStartCommand(player, args);
            case "cancel":
                return handleCancelCommand(player, args);
            case "progress":
                return handleProgressCommand(player);
            case "leaderboard":
            case "lb":
                return handleLeaderboardCommand(player, args);
            default:
                showHelp(player);
                return true;
        }
    }

    // === 命令处理方法 ===

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("pathselection.reload")) {
            sender.sendMessage(language.getMessage("no-permission"));
            return true;
        }

        if (plugin.reloadPlugin()) {
            sender.sendMessage(language.getMessage("reload-success"));
        } else {
            sender.sendMessage(language.getMessage("reload-failed"));
        }
        return true;
    }

    private boolean handleLeaderboardCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(language.getMessage("invalid-usage",
                    "usage", "/ps leaderboard <路径名> <daily|historical>"));
            return true;
        }

        String pathName = args[1];
        String type = args[2].toLowerCase();

        if (!type.equals("daily") && !type.equals("historical")) {
            sender.sendMessage(language.getMessage("invalid-type"));
            return true;
        }

        showLeaderboard(sender, pathName, type);
        return true;
    }

    private boolean handleRewardCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pathselection.admin")) {
            sender.sendMessage(language.getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(language.getMessage("invalid-usage",
                    "usage", "/ps reward <add|list|remove|clear> <路径名> [命令]"));
            return true;
        }

        String action = args[1].toLowerCase();
        String pathName = args[2];

        switch (action) {
            case "add":
                if (args.length < 4) {
                    sender.sendMessage(language.getMessage("invalid-usage",
                            "usage", "/ps reward add <路径名> <命令>"));
                    return true;
                }
                String rewardCommand = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                return addReward(sender, pathName, rewardCommand);

            case "list":
                return listRewards(sender, pathName);

            case "remove":
                if (args.length < 4) {
                    sender.sendMessage(language.getMessage("invalid-usage",
                            "usage", "/ps reward remove <路径名> <索引>"));
                    return true;
                }
                return removeReward(sender, pathName, args[3]);

            case "clear":
                return clearRewards(sender, pathName);

            default:
                sender.sendMessage(language.getMessage("invalid-usage",
                        "usage", "/ps reward <add|list|remove|clear> <路径名>"));
                return true;
        }
    }

    private boolean handleCreateCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(language.getMessage("invalid-usage",
                    "usage", "/ps create <选区集名称>"));
            return true;
        }

        String pathName = args[1];
        if (plugin.createNewPath(player, pathName)) {
            player.sendMessage(language.getMessage("path-created", "name", pathName));
            player.sendMessage(language.getMessage("hint-use-tool", "command", "/ps edit " + pathName + " create"));
            player.sendMessage(language.getMessage("hint-finish-creation"));
        } else {
            player.sendMessage(language.getMessage("already-exists", "name", pathName));
        }
        return true;
    }

    private boolean handleFinishCommand(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerEditSession session = plugin.getPlayerEditSession(playerId);

        if (session == null) {
            player.sendMessage(language.getMessage("not-editing"));
            return true;
        }

        String pathName = session.getEditingPathName();

        if (session.getRegions().isEmpty()) {
            player.sendMessage(language.getMessage("empty-path"));
            return true;
        }

        plugin.saveSelectionPathToConfig(pathName, session.getRegions());
        plugin.endEditingSession(playerId, false);

        player.sendMessage(language.getMessage("path-saved",
                "name", pathName, "count", String.valueOf(session.getRegions().size())));
        return true;
    }

    private boolean handleEditCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(language.getMessage("invalid-usage",
                    "usage", "/ps edit <选区集名称> [create|delete <索引>|finish]"));
            return true;
        }

        String pathName = args[1];

        if (args.length == 2) {
            // 开始编辑模式
            if (plugin.startEditingPath(player, pathName)) {
                player.sendMessage(language.getMessage("path-edit-started", "name", pathName));
                player.sendMessage(language.getMessage("hint-use-tool", "command", "/ps edit " + pathName + " create"));
            } else {
                player.sendMessage(language.getMessage("not-found", "name", pathName));
            }
        } else {
            // 处理编辑子命令
            String[] subArgs = Arrays.copyOfRange(args, 2, args.length);
            handleEditSubcommand(player, pathName, subArgs);
        }
        return true;
    }

    private boolean handleUnloadCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(language.getMessage("invalid-usage",
                    "usage", "/ps unload <选区集名称>"));
            return true;
        }

        String pathName = args[1];
        if (plugin.unloadPath(pathName)) {
            player.sendMessage(language.getMessage("path-unloaded", "name", pathName));
        } else {
            player.sendMessage(language.getMessage("in-use"));
        }
        return true;
    }

    private boolean handleDeletePathCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(language.getMessage("invalid-usage",
                    "usage", "/ps deletepath <选区集名称>"));
            return true;
        }

        String pathName = args[1];
        if (plugin.deleteSelectionPath(pathName)) {
            player.sendMessage(language.getMessage("path-deleted", "name", pathName));
        } else {
            player.sendMessage(language.getMessage("in-use"));
        }
        return true;
    }

    private boolean handleListCommand(Player player) {
        Set<String> pathNames = plugin.getLoadedPathNames();

        if (pathNames.isEmpty()) {
            player.sendMessage(language.getMessage("list-empty"));
            return true;
        }

        player.sendMessage(language.getMessage("list-header"));
        for (String name : pathNames) {
            SelectionPath path = plugin.getPath(name);
            if (path != null) {
                player.sendMessage(language.getMessage("list-item",
                        "name", name, "count", String.valueOf(path.getRegions().size())));
            }
        }
        return true;
    }

    private boolean handleToolCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(language.getMessage("invalid-usage",
                    "usage", "/ps tool <物品名称>"));
            return true;
        }

        String materialName = args[1];
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            plugin.setSelectionTool(material);
            player.sendMessage(language.getMessage("tool-set", "tool", material.name()));
        } catch (IllegalArgumentException e) {
            player.sendMessage(language.getMessage("invalid-material", "material", materialName));
        }
        return true;
    }

    private boolean handleStartCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(language.getMessage("invalid-usage",
                    "usage", "/ps start <选区集名称>"));
            return true;
        }

        String pathName = args[1];
        if (plugin.startPathProgress(player, pathName)) {
            player.sendMessage(language.getMessage("progress-started", "name", pathName));
        } else {
            player.sendMessage(language.getMessage("not-found", "name", pathName));
        }
        return true;
    }

    private boolean handleCancelCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pathselection.cancel")) {
            sender.sendMessage(language.getMessage("no-permission"));
            return true;
        }

        Player targetPlayer;
        boolean isConsole = !(sender instanceof Player);

        if (args.length == 1) {
            // 格式: /ps cancel - 玩家取消自己的计时
            if (isConsole) {
                // 控制台必须指定玩家
                sender.sendMessage(language.getMessage("console-need-player"));
                sender.sendMessage(language.getMessage("invalid-usage",
                        "usage", "/ps cancel <玩家>"));
                return true;
            }
            targetPlayer = (Player) sender;
        }
        else if (args.length == 2) {
            // 格式: /ps cancel <玩家> - 取消指定玩家的计时
            if (!sender.hasPermission("pathselection.cancel.other")) {
                sender.sendMessage(language.getMessage("no-permission-other"));
                return true;
            }

            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(language.getMessage("player-not-found", "player", args[1]));
                return true;
            }
        }
        else {
            sender.sendMessage(language.getMessage("invalid-usage",
                    "usage", "/ps cancel [玩家]"));
            return true;
        }

        // 检查目标玩家是否有进行中的计时
        PathProgress progress = plugin.getPlayerProgress(targetPlayer.getUniqueId());
        if (progress == null) {
            if (sender.equals(targetPlayer)) {
                sender.sendMessage(language.getMessage("progress-none"));
            } else {
                sender.sendMessage(language.getMessage("progress-none-other",
                        "player", targetPlayer.getName()));
            }
            return true;
        }

        String pathName = progress.getPathName();
        long timeTaken = progress.getTimeTaken();
        String timeDisplay = TimeFormatter.formatMilliseconds(timeTaken);

        // 执行取消
        plugin.cancelPathProgress(targetPlayer.getUniqueId());

        if (sender.equals(targetPlayer)) {
            // 自己取消
            sender.sendMessage(language.getMessage("progress-cancelled"));
            sender.sendMessage(language.getMessage("cancelled-time", "time", timeDisplay));
        } else {
            // 管理员/控制台取消他人
            if (isConsole) {
                sender.sendMessage(language.getMessage("progress-cancelled-console",
                        "player", targetPlayer.getName(), "name", pathName, "time", timeDisplay));
            } else {
                sender.sendMessage(language.getMessage("progress-cancelled-other",
                        "player", targetPlayer.getName(), "name", pathName, "time", timeDisplay));
            }

            // 可选：通知目标玩家（如果需要）
            if (plugin.getConfig().getBoolean("notify-player-on-admin-cancel", false)) {
                targetPlayer.sendMessage(language.getMessage("progress-cancelled-by-admin",
                        "admin", sender.getName(), "name", pathName));
            }
        }

        return true;
    }

    private boolean handleProgressCommand(Player player) {
        PathProgress progress = plugin.getPlayerProgress(player.getUniqueId());
        if (progress != null) {
            player.sendMessage(language.getMessage("progress-header"));
            player.sendMessage(language.getMessage("progress-path", "name", progress.getPathName()));
            player.sendMessage(language.getMessage("progress-index",
                    "current", String.valueOf(progress.getCurrentRegionIndex()),
                    "total", String.valueOf(progress.getTotalRegions())));

            long time = progress.getTimeTaken() / 1000;
            player.sendMessage(language.getMessage("progress-time", "time", String.valueOf(time)));
        } else {
            player.sendMessage(language.getMessage("progress-none"));
            player.sendMessage(language.getMessage("hint-start-manual"));
        }
        return true;
    }

    // === 辅助方法 ===

    private void showHelp(Player player) {
        player.sendMessage(language.getMessage("help-header"));
        player.sendMessage(language.getMessage("help-create"));
        player.sendMessage(language.getMessage("help-finish"));
        player.sendMessage(language.getMessage("help-edit"));
        player.sendMessage(language.getMessage("help-edit-create"));
        player.sendMessage(language.getMessage("help-edit-delete"));
        player.sendMessage(language.getMessage("help-edit-finish"));
        player.sendMessage(language.getMessage("help-unload"));
        player.sendMessage(language.getMessage("help-deletepath"));
        player.sendMessage(language.getMessage("help-list"));
        player.sendMessage(language.getMessage("help-tool"));
        player.sendMessage(language.getMessage("help-start"));
        player.sendMessage(language.getMessage("help-cancel"));
        player.sendMessage(language.getMessage("help-progress"));
        player.sendMessage(language.getMessage("help-leaderboard"));
        player.sendMessage(language.getMessage("help-reload"));

        if (player.hasPermission("pathselection.admin")) {
            player.sendMessage(language.getMessage("help-reward-add"));
            player.sendMessage(language.getMessage("help-reward-list"));
            player.sendMessage(language.getMessage("help-reward-remove"));
            player.sendMessage(language.getMessage("help-reward-clear"));
        }
    }

    private void showConsoleHelp(CommandSender sender) {
        sender.sendMessage("=== 路径选区插件控制台命令 ===");
        sender.sendMessage("/ps reload - 重载插件配置");
        sender.sendMessage("/ps leaderboard <路径名> <daily|historical> - 查看排行榜");
        sender.sendMessage("/ps reward add <路径名> <命令> - 添加奖励");
        sender.sendMessage("/ps reward list <路径名> - 查看奖励列表");
        sender.sendMessage("/ps reward remove <路径名> <索引> - 删除奖励");
        sender.sendMessage("/ps reward clear <路径名> - 清空奖励");
    }

    private void handleEditSubcommand(Player player, String pathName, String[] subArgs) {
        UUID playerId = player.getUniqueId();
        PlayerEditSession session = plugin.getPlayerEditSession(playerId);

        if (session == null || !session.getEditingPathName().equals(pathName)) {
            player.sendMessage(language.getMessage("not-editing"));
            return;
        }

        if (subArgs.length == 0) {
            player.sendMessage(language.getMessage("invalid-usage",
                    "usage", "/ps edit " + pathName + " [create|delete <索引>|finish]"));
            return;
        }

        String subCommand = subArgs[0].toLowerCase();

        switch (subCommand) {
            case "create":
                finishCurrentRegion(player, session);
                break;

            case "delete":
                if (subArgs.length < 2) {
                    player.sendMessage(language.getMessage("invalid-usage",
                            "usage", "/ps edit " + pathName + " delete <索引>"));
                    return;
                }
                try {
                    int index = Integer.parseInt(subArgs[1]);
                    deleteRegion(player, session, index);
                } catch (NumberFormatException e) {
                    player.sendMessage(language.getMessage("invalid-index", "index", subArgs[1]));
                }
                break;

            case "finish":
                finishEditingSession(player, session);
                break;

            default:
                player.sendMessage(language.getMessage("invalid-usage",
                        "usage", "/ps edit " + pathName + " [create|delete <索引>|finish]"));
                break;
        }
    }

    private void finishCurrentRegion(Player player, PlayerEditSession session) {
        if (session.getCurrentRegion() == null || !session.getCurrentRegion().isValid()) {
            player.sendMessage(language.getMessage("empty-selection"));
            return;
        }

        session.finishCurrentRegion();
        int count = session.getRegions().size();

        player.sendMessage(language.getMessage("region-added", "count", String.valueOf(count)));
        session.clearCurrentRegion();
    }

    private void finishEditingSession(Player player, PlayerEditSession session) {
        String pathName = session.getEditingPathName();

        if (session.getRegions().isEmpty()) {
            player.sendMessage(language.getMessage("empty-path"));
            return;
        }

        plugin.saveSelectionPathToConfig(pathName, session.getRegions());
        plugin.endEditingSession(player.getUniqueId(), false);

        player.sendMessage(language.getMessage("path-saved",
                "name", pathName, "count", String.valueOf(session.getRegions().size())));
    }

    private void deleteRegion(Player player, PlayerEditSession session, int index) {
        if (plugin.deleteRegionFromEditingSession(player.getUniqueId(), index)) {
            player.sendMessage(language.getMessage("region-deleted",
                    "index", String.valueOf(index + 1),
                    "count", String.valueOf(session.getRegions().size())));
        } else {
            player.sendMessage(language.getMessage("invalid-index", "index", String.valueOf(index)));
        }
    }

    private boolean addReward(CommandSender sender, String pathName, String command) {
        if (plugin.getRewardManager().addReward(pathName, command)) {
            sender.sendMessage(language.getMessage("reward-added", "name", pathName, "command", command));
            return true;
        } else {
            sender.sendMessage(language.getMessage("reward-add-failed"));
            return false;
        }
    }

    private boolean listRewards(CommandSender sender, String pathName) {
        List<String> rewards = plugin.getRewardManager().getRewards(pathName);

        if (rewards.isEmpty()) {
            sender.sendMessage(language.getMessage("reward-empty", "name", pathName));
            return true;
        }

        sender.sendMessage(language.getMessage("reward-list-header", "name", pathName));
        for (int i = 0; i < rewards.size(); i++) {
            sender.sendMessage(language.getMessage("reward-list-item",
                    "index", String.valueOf(i + 1),
                    "command", rewards.get(i)));
        }
        return true;
    }

    private boolean removeReward(CommandSender sender, String pathName, String indexStr) {
        try {
            int index = Integer.parseInt(indexStr) - 1;
            if (plugin.getRewardManager().removeReward(pathName, index)) {
                sender.sendMessage(language.getMessage("reward-removed",
                        "name", pathName, "index", indexStr));
                return true;
            } else {
                sender.sendMessage(language.getMessage("reward-remove-failed"));
                return false;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(language.getMessage("invalid-index", "index", indexStr));
            return false;
        }
    }

    private boolean clearRewards(CommandSender sender, String pathName) {
        plugin.getRewardManager().clearRewards(pathName);
        sender.sendMessage(language.getMessage("reward-cleared", "name", pathName));
        return true;
    }

    private void showLeaderboard(CommandSender sender, String pathName, String type) {
        List<CompletionRecord> leaderboard;
        String title;

        if (type.equals("daily")) {
            String date = TimeFormatter.getCurrentDate();
            leaderboard = plugin.getDatabaseManager().getDailyLeaderboard(pathName, date);
            title = language.getMessage("leaderboard-daily-title", "name", pathName, "date", date);
        } else {
            leaderboard = plugin.getDatabaseManager().getHistoricalLeaderboard(pathName);
            title = language.getMessage("leaderboard-historical-title", "name", pathName);
        }

        if (leaderboard.isEmpty()) {
            sender.sendMessage(language.getMessage("leaderboard-empty"));
            return;
        }

        sender.sendMessage(title);
        for (int i = 0; i < leaderboard.size() && i < 10; i++) {
            CompletionRecord record = leaderboard.get(i);
            sender.sendMessage(language.getMessage("leaderboard-entry",
                    "rank", String.valueOf(i + 1),
                    "player", record.getPlayerName(),
                    "time", record.getFormattedTime(),
                    "date", new java.util.Date(record.getCompletionTime()).toString()));
        }
    }


    private boolean handleRecordDeleteCommand(CommandSender sender, String[] args) {
        // 权限检查 - 只有管理员可以删除
        if (!sender.hasPermission("pathselection.admin")) {
            sender.sendMessage(language.getMessage("no-permission-admin"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(language.getMessage("invalid-usage",
                    "usage", "/ps record delete <路径名> [玩家名] [日期]"));
            return true;
        }

        String pathName = args[2];

        // 检查路径是否存在
        if (plugin.getPath(pathName) == null) {
            sender.sendMessage(language.getMessage("not-found", "name", pathName));
            return true;
        }

        // 根据参数数量判断删除类型
        if (args.length == 3) {
            // 删除整个路径的所有记录
            return deletePathRecords(sender, pathName);
        } else if (args.length == 4) {
            // 删除玩家在该路径的所有记录
            String playerName = args[3];
            return deletePlayerPathRecords(sender, pathName, playerName);
        } else if (args.length >= 5) {
            // 删除特定记录
            String playerName = args[3];
            String date = args[4];
            return deleteSpecificRecord(sender, pathName, playerName, date);
        } else {
            sender.sendMessage(language.getMessage("invalid-usage",
                    "usage", "/ps record delete <路径名> [玩家名] [日期]"));
            return true;
        }
    }

    /**
     * 删除整个路径的所有记录
     */
    private boolean deletePathRecords(CommandSender sender, String pathName) {
        int deletedCount = plugin.getDatabaseManager().deletePathRecords(pathName);

        if (deletedCount > 0) {
            // 记录日志
            plugin.getLogger().info(String.format("管理员 %s 删除了路径 %s 的所有记录 (%d 条)",
                    sender.getName(), pathName, deletedCount));

            sender.sendMessage(language.getMessage("records-deleted",
                    "count", String.valueOf(deletedCount), "target", "路径 " + pathName));
        } else {
            sender.sendMessage(language.getMessage("no-records-found", "target", "路径 " + pathName));
        }

        return true;
    }

    /**
     * 删除玩家在特定路径的所有记录
     */
    private boolean deletePlayerPathRecords(CommandSender sender, String pathName, String playerName) {
        // 获取玩家UUID
        UUID playerUuid = getPlayerUuidByName(playerName);
        if (playerUuid == null) {
            sender.sendMessage(language.getMessage("player-not-found", "player", playerName));
            return true;
        }

        int deletedCount = plugin.getDatabaseManager().deletePlayerPathRecords(playerUuid, pathName);

        if (deletedCount > 0) {
            // 记录日志
            plugin.getLogger().info(String.format("管理员 %s 删除了玩家 %s 在路径 %s 的所有记录 (%d 条)",
                    sender.getName(), playerName, pathName, deletedCount));

            sender.sendMessage(language.getMessage("records-deleted",
                    "count", String.valueOf(deletedCount),
                    "target", "玩家 " + playerName + " 在路径 " + pathName + " 的记录"));
        } else {
            sender.sendMessage(language.getMessage("no-records-found",
                    "target", "玩家 " + playerName + " 在路径 " + pathName + " 的记录"));
        }

        return true;
    }

    /**
     * 删除特定记录
     */
    private boolean deleteSpecificRecord(CommandSender sender, String pathName, String playerName, String date) {
        // 获取玩家UUID
        UUID playerUuid = getPlayerUuidByName(playerName);
        if (playerUuid == null) {
            sender.sendMessage(language.getMessage("player-not-found", "player", playerName));
            return true;
        }

        // 验证日期格式
        if (!isValidDate(date)) {
            sender.sendMessage(language.getMessage("invalid-date", "date", date));
            return true;
        }

        int deletedCount = plugin.getDatabaseManager().deleteSpecificRecord(playerUuid, pathName, date);

        if (deletedCount > 0) {
            // 记录日志
            plugin.getLogger().info(String.format("管理员 %s 删除了玩家 %s 在路径 %s 日期为 %s 的记录 (%d 条)",
                    sender.getName(), playerName, pathName, date, deletedCount));

            sender.sendMessage(language.getMessage("records-deleted",
                    "count", String.valueOf(deletedCount),
                    "target", "玩家 " + playerName + " 在路径 " + pathName + " 日期 " + date + " 的记录"));
        } else {
            sender.sendMessage(language.getMessage("no-records-found",
                    "target", "玩家 " + playerName + " 在路径 " + pathName + " 日期 " + date + " 的记录"));
        }

        return true;
    }

    /**
     * 根据玩家名获取UUID
     */
    private UUID getPlayerUuidByName(String playerName) {
        // 先检查在线玩家
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        }

        // 然后检查数据库中的记录
        return plugin.getDatabaseManager().getPlayerUuidByName(playerName);
    }

    /**
     * 验证日期格式 (YYYY-MM-DD)
     */
    private boolean isValidDate(String date) {
        return date.matches("\\d{4}-\\d{2}-\\d{2}");
    }
}