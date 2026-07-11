package org.zzq.pathSelection.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.zzq.pathSelection.PathSelection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 命令补全器
 */
public class CommandCompleter implements TabCompleter {

    private final PathSelection plugin;

    public CommandCompleter(PathSelection plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("CommandCompleter 被实例化，版本: 2026-01-31 20:30");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 第一级命令补全
            if (hasPermission(sender, "pathselection.create")) {
                completions.add("create");
            }
            if (hasPermission(sender, "pathselection.edit")) {
                completions.add("finish");
                completions.add("edit");
                completions.add("unload");
            }
            if (hasPermission(sender, "pathselection.delete")) {
                completions.add("deletepath");
            }
            if (hasPermission(sender, "pathselection.list")) {
                completions.add("list");
            }
            if (hasPermission(sender, "pathselection.tool")) {
                completions.add("tool");
            }
            if (hasPermission(sender, "pathselection.start")) {
                completions.add("start");
            }
            if (hasPermission(sender, "pathselection.cancel")) {
                completions.add("cancel");
            }
            if (hasPermission(sender, "pathselection.progress")) {
                completions.add("progress");
            }
            if (hasPermission(sender, "pathselection.leaderboard")) {
                completions.add("leaderboard");
                completions.add("lb");
            }
            if (hasPermission(sender, "pathselection.reload")) {
                completions.add("reload");
            }
            if (hasPermission(sender, "pathselection.reward")) {
                completions.add("reward");
            }
            if (hasPermission(sender, "pathselection.admin")) {
                completions.add("record");
            }

            return completions;
        }

        else if (args.length == 2) {
            String firstArg = args[0].toLowerCase();

            if (firstArg.equals("edit") && hasPermission(sender, "pathselection.edit")) {
                completions.addAll(plugin.getLoadedPathNames());
            }
            else if (firstArg.equals("unload") && hasPermission(sender, "pathselection.edit")) {
                completions.addAll(plugin.getLoadedPathNames());
            }
            else if (firstArg.equals("deletepath") && hasPermission(sender, "pathselection.delete")) {
                completions.addAll(plugin.getLoadedPathNames());
            }
            else if ((firstArg.equals("leaderboard") || firstArg.equals("lb")) &&
                    hasPermission(sender, "pathselection.leaderboard")) {
                completions.addAll(plugin.getLoadedPathNames());
            }
            else if (firstArg.equals("record") && hasPermission(sender, "pathselection.admin")) {
                completions.add("delete");
            }
            else if (firstArg.equals("start") && hasPermission(sender, "pathselection.start")) {
                // 智能补全：根据权限和上下文
                if (hasPermission(sender, "pathselection.start.other")) {
                    // 管理员：补全在线玩家
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        completions.add(onlinePlayer.getName());
                    }
                }
                // 所有用户都可以补全路径名
                completions.addAll(plugin.getLoadedPathNames());
            }
            else if (firstArg.equals("cancel") && hasPermission(sender, "pathselection.cancel")) {
                // 智能补全：根据权限
                if (hasPermission(sender, "pathselection.cancel.other")) {
                    // 管理员：补全有进度的在线玩家
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (plugin.getPlayerProgress(onlinePlayer.getUniqueId()) != null) {
                            completions.add(onlinePlayer.getName());
                        }
                    }
                }
            }
            else if (firstArg.equals("tool") && hasPermission(sender, "pathselection.tool")) {
                for (Material material : Material.values()) {
                    if (material.isItem()) {
                        completions.add(material.name().toLowerCase());
                    }
                }
            }
            else if (firstArg.equals("reward") && hasPermission(sender, "pathselection.reward")) {
                completions.addAll(Arrays.asList("add", "list", "remove", "clear"));
            }

            return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
        }

        else if (args.length == 3) {
            String firstArg = args[0].toLowerCase();
            String secondArg = args[1];

            if (firstArg.equals("edit") && hasPermission(sender, "pathselection.edit")) {
                completions.addAll(Arrays.asList("create", "delete", "finish", "clear", "particles"));
            }
            else if ((firstArg.equals("leaderboard") || firstArg.equals("lb")) &&
                    hasPermission(sender, "pathselection.leaderboard")) {
                completions.addAll(Arrays.asList("daily", "historical", "player"));
            }
            else if (firstArg.equals("reward") && hasPermission(sender, "pathselection.reward")) {
                if (secondArg.equals("add") || secondArg.equals("list") ||
                        secondArg.equals("remove") || secondArg.equals("clear")) {
                    completions.addAll(plugin.getLoadedPathNames());
                }
            }
            else if (firstArg.equals("start") && hasPermission(sender, "pathselection.start")) {
                // 第二参数是玩家名，第三参数需要路径名
                Player targetPlayer = Bukkit.getPlayer(secondArg);
                if (targetPlayer != null && hasPermission(sender, "pathselection.start.other")) {
                    completions.addAll(plugin.getLoadedPathNames());
                }
            }
            else if (firstArg.equals("record") && secondArg.equals("delete") &&
                    hasPermission(sender, "pathselection.admin")) {
                // 记录删除命令的路径名补全
                completions.addAll(plugin.getLoadedPathNames());
            }

            return StringUtil.copyPartialMatches(args[2], completions, new ArrayList<>());
        }

        else if (args.length == 4) {
            String firstArg = args[0].toLowerCase();
            String secondArg = args[1];
            String thirdArg = args[2];

            if (firstArg.equals("reward") && hasPermission(sender, "pathselection.reward")) {
                if (secondArg.equals("remove")) {
                    List<String> rewards = plugin.getRewardManager().getRewards(thirdArg);
                    for (int i = 1; i <= rewards.size(); i++) {
                        completions.add(String.valueOf(i));
                    }
                    if (completions.isEmpty()) {
                        completions.add("1");
                        completions.add("2");
                        completions.add("3");
                    }
                }
                else if (secondArg.equals("add")) {
                    completions.addAll(Arrays.asList(
                            "say 恭喜 {player} 完成了路径!",
                            "eco give {player} 500",
                            "give {player} diamond 1",
                            "effect give {player} speed 60 1"
                    ));
                }
            }
            else if (firstArg.equals("record") && secondArg.equals("delete") &&
                    hasPermission(sender, "pathselection.admin")) {
                // 删除记录命令的玩家名补全
                String pathName = thirdArg;
                completions.addAll(plugin.getDatabaseManager().getPlayersWithRecords(pathName));
            }

            return StringUtil.copyPartialMatches(args[3], completions, new ArrayList<>());
        }

        else if (args.length == 5) {
            String firstArg = args[0].toLowerCase();
            String secondArg = args[1];
            String thirdArg = args[2];
            String fourthArg = args[3];

            if (firstArg.equals("record") && secondArg.equals("delete") &&
                    hasPermission(sender, "pathselection.admin")) {
                // 删除记录命令的日期补全
                String pathName = thirdArg;
                String playerName = fourthArg;
                UUID playerUuid = plugin.getDatabaseManager().getPlayerUuidByName(playerName);
                if (playerUuid != null) {
                    completions.addAll(plugin.getDatabaseManager().getPlayerRecordDates(playerUuid, pathName));
                }
            }

            return StringUtil.copyPartialMatches(args[4], completions, new ArrayList<>());
        }

        return completions;
    }

    /**
     * 检查权限的辅助方法
     */
    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) ||
                sender.hasPermission("pathselection.*") ||
                sender.hasPermission("pathselection.admin");
    }
}