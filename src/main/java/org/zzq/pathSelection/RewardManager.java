package org.zzq.pathSelection;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.zzq.pathSelection.database.DatabaseManager;
import org.zzq.pathSelection.util.TimeFormatter;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class RewardManager {
    private final DatabaseManager database;
    private final Logger logger;

    public RewardManager(DatabaseManager database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public boolean giveRewards(Player player, String pathName) {
        UUID playerUuid = player.getUniqueId();
        String currentDate = TimeFormatter.getCurrentDate();

        // 检查是否已领取今日奖励
        if (database.hasClaimedDailyReward(playerUuid, pathName, currentDate)) {
            player.sendMessage("§c你今天已经领取过这个路径的奖励了！");
            return false;
        }

        // 获取奖励命令
        List<String> rewards = database.getRewards(pathName);
        if (rewards.isEmpty()) {
            player.sendMessage("§e这个路径没有配置奖励。");
            return true; // 没有奖励也算成功
        }

        // 执行奖励命令
        boolean overallSuccess = true;
        for (String command : rewards) {
            if (!executeRewardCommand(player, command)) {
                overallSuccess = false;
                logger.warning("执行奖励命令失败: " + command + " 给玩家 " + player.getName());
            } else {
                logger.info("成功执行奖励命令: " + command + " 给玩家 " + player.getName());
            }
        }

        if (overallSuccess) {
            // 记录奖励领取
            database.recordDailyReward(playerUuid, pathName, currentDate);
            player.sendMessage("§a奖励已发放！");
        } else {
            player.sendMessage("§c部分奖励发放失败，请联系管理员。");
        }

        return overallSuccess;
    }

    private boolean executeRewardCommand(Player player, String command) {
        try {
            // 替换所有可能的占位符
            String processedCommand = command
                    .replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString())
                    .replace("%player%", player.getName())
                    .replace("%uuid%", player.getUniqueId().toString())
                    .replace("{name}", player.getName())
                    .replace("%name%", player.getName())
                    .replace("{displayname}", player.getDisplayName())
                    .replace("%displayname%", player.getDisplayName());

            // 确保命令以正确的格式执行
            if (processedCommand.startsWith("/")) {
                processedCommand = processedCommand.substring(1);
            }

            // 使用控制台执行命令，确保有足够权限
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);

            if (!success) {
                logger.warning("命令执行返回失败: " + processedCommand);
                // 即使返回失败，我们也继续执行，因为有些命令可能没有返回值
            }

            return true; // 只要没有异常就认为成功
        } catch (Exception e) {
            logger.severe("执行奖励命令时出现异常: " + command + " - " + e.getMessage());
            return false;
        }
    }

    public boolean addReward(String pathName, String command) {
        // 验证命令格式
        if (command == null || command.trim().isEmpty()) {
            return false;
        }

        return database.addReward(pathName, command.trim());
    }

    public List<String> getRewards(String pathName) {
        return database.getRewards(pathName);
    }

    public boolean removeReward(String pathName, int index) {
        List<String> rewards = getRewards(pathName);
        if (index < 0 || index >= rewards.size()) {
            return false;
        }

        // 这里需要实现删除特定奖励的逻辑
        // 暂时简单实现：清空所有奖励然后重新添加（除了要删除的）
        database.clearRewards(pathName);
        for (int i = 0; i < rewards.size(); i++) {
            if (i != index) {
                database.addReward(pathName, rewards.get(i));
            }
        }
        return true;
    }

    public void clearRewards(String pathName) {
        database.clearRewards(pathName);
    }
}