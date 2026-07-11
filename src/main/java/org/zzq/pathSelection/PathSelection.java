package org.zzq.pathSelection;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.zzq.pathSelection.command.CommandCompleter;
import org.zzq.pathSelection.command.PathSelectionCommand;
import org.zzq.pathSelection.database.DatabaseManager;
import org.zzq.pathSelection.util.TimeFormatter;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PathSelection extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private File configFile;
    private FileConfiguration selectionsConfig;
    private File selectionsFile;
    private Material selectionTool;

    private Map<String, SelectionPath> loadedPaths = new HashMap<>();
    private Map<UUID, PlayerEditSession> playerEditSessions = new HashMap<>();
    private Map<UUID, PathProgress> playerProgress = new HashMap<>();
    private Map<UUID, Long> lastActionBarUpdate = new HashMap<>();

    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    private RewardManager rewardManager;
    private PlaceholderAPIExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        // 加载配置
        loadConfig();
        loadSelectionsConfig();

        // 初始化数据库
        File databaseFile = new File(getDataFolder(), "data.db");
        databaseManager = new DatabaseManager(getLogger(), databaseFile.getAbsolutePath());

        // 初始化语言管理器
        languageManager = new LanguageManager(this);
        languageManager.loadLanguage();

        // 初始化奖励管理器
        rewardManager = new RewardManager(databaseManager, getLogger());

        // 加载所有选区集
        loadAllPaths();

        // 注册事件
        getServer().getPluginManager().registerEvents(this, this);
        // 创建命令执行器和补全器
        PathSelectionCommand commandExecutor = new PathSelectionCommand(this);
        CommandCompleter commandCompleter = new CommandCompleter(this);

//        // 注册命令
//        Objects.requireNonNull(getCommand("pathselection")).setExecutor(commandExecutor);
//        Objects.requireNonNull(getCommand("pathselection")).setTabCompleter(commandCompleter);
        Objects.requireNonNull(getCommand("ps")).setExecutor(commandExecutor);
        Objects.requireNonNull(getCommand("ps")).setTabCompleter(commandCompleter);


        // 注册PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new PlaceholderAPIExpansion(this);
            placeholderExpansion.register();
            getLogger().info("已注册PlaceholderAPI扩展");
        }

        // 启动任务
        startParticleTask();
        startProgressCheckTask();
        startDailyResetTask();

        getLogger().info(languageManager.getMessage("plugin-enabled", "count", String.valueOf(loadedPaths.size())));
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }

        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }

        getLogger().info(languageManager.getMessage("plugin-disabled"));
    }

    // 新增：重载插件功能
    public boolean reloadPlugin() {
        try {
            // 清除所有当前状态
            playerEditSessions.clear();
            playerProgress.clear();
            lastActionBarUpdate.clear();

            // 重新加载配置
            loadConfig();
            loadSelectionsConfig();

            // 重新加载语言文件
            languageManager.reloadLanguage();

            // 重新加载选区集
            loadAllPaths();

            getLogger().info(languageManager.getMessage("plugin-reloaded",
                    "count", String.valueOf(loadedPaths.size())));
            return true;
        } catch (Exception e) {
            getLogger().severe(languageManager.getMessage("reload-failed", "error", e.getMessage()));
            return false;
        }
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    private void loadConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        selectionTool = Material.getMaterial(config.getString("selection-tool", "STICK"));
    }

    private void loadSelectionsConfig() {
        selectionsFile = new File(getDataFolder(), "selections.yml");
        if (!selectionsFile.exists()) {
            if (!selectionsFile.getParentFile().exists()) {
                selectionsFile.getParentFile().mkdirs();
            }
            try {
                selectionsFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe(languageManager.getMessage("config-error", "error", e.getMessage()));
            }
        }
        selectionsConfig = YamlConfiguration.loadConfiguration(selectionsFile);
    }

    private void loadAllPaths() {
        loadedPaths.clear();

        if (selectionsConfig.getConfigurationSection("paths") == null) {
            return;
        }

        for (String pathName : selectionsConfig.getConfigurationSection("paths").getKeys(false)) {
            SelectionPath path = loadSelectionPathFromConfig(pathName);
            if (path != null) {
                loadedPaths.put(pathName, path);
                getLogger().info(languageManager.getMessage("path-loaded",
                        "name", pathName, "count", String.valueOf(path.getRegions().size())));
            }
        }
    }

    private SelectionPath loadSelectionPathFromConfig(String pathName) {
        if (!selectionsConfig.contains("paths." + pathName)) {
            return null;
        }

        List<SelectionRegion> regions = new ArrayList<>();
        List<Map<?, ?>> regionDataList = selectionsConfig.getMapList("paths." + pathName);

        for (Map<?, ?> regionData : regionDataList) {
            SelectionRegion region = new SelectionRegion();

            Map<?, ?> startData = (Map<?, ?>) regionData.get("start");
            Map<?, ?> endData = (Map<?, ?>) regionData.get("end");
            String worldName = (String) regionData.get("world");

            Location start = new Location(
                    Bukkit.getWorld(worldName),
                    ((Number) startData.get("x")).intValue(),
                    ((Number) startData.get("y")).intValue(),
                    ((Number) startData.get("z")).intValue()
            );

            Location end = new Location(
                    Bukkit.getWorld(worldName),
                    ((Number) endData.get("x")).intValue(),
                    ((Number) endData.get("y")).intValue(),
                    ((Number) endData.get("z")).intValue()
            );

            region.setStartPoint(start);
            region.setEndPoint(end);
            regions.add(region);
        }

        return new SelectionPath(pathName, regions);
    }

    public void saveSelectionPathToConfig(String pathName, List<SelectionRegion> regions) {
        List<Map<String, Object>> serializedRegions = new ArrayList<>();
        for (int i = 0; i < regions.size(); i++) {
            SelectionRegion region = regions.get(i);
            if (region.isValid()) {
                Map<String, Object> regionData = new HashMap<>();
                regionData.put("index", i);
                regionData.put("type", getRegionType(i, regions.size()));
                regionData.put("start", serializeLocation(region.getStartPoint()));
                regionData.put("end", serializeLocation(region.getEndPoint()));
                regionData.put("world", region.getStartPoint().getWorld().getName());
                serializedRegions.add(regionData);
            }
        }

        selectionsConfig.set("paths." + pathName, serializedRegions);
        saveSelectionsConfig();

        loadedPaths.put(pathName, new SelectionPath(pathName, regions));
    }

    public boolean deleteSelectionPath(String pathName) {
        if (!selectionsConfig.contains("paths." + pathName)) {
            return false;
        }

        for (PlayerEditSession session : playerEditSessions.values()) {
            if (session.getEditingPathName() != null &&
                    session.getEditingPathName().equals(pathName)) {
                return false;
            }
        }

        selectionsConfig.set("paths." + pathName, null);
        saveSelectionsConfig();

        loadedPaths.remove(pathName);

        return true;
    }

    public void saveSelectionsConfig() {
        try {
            selectionsConfig.save(selectionsFile);
        } catch (IOException e) {
            getLogger().severe(languageManager.getMessage("config-save-error", "error", e.getMessage()));
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().severe(languageManager.getMessage("config-save-error", "error", e.getMessage()));
        }
    }

    public boolean unloadPath(String pathName) {
        if (!loadedPaths.containsKey(pathName)) {
            return false;
        }

        for (PlayerEditSession session : playerEditSessions.values()) {
            if (session.getEditingPathName() != null &&
                    session.getEditingPathName().equals(pathName)) {
                return false;
            }
        }

        loadedPaths.remove(pathName);
        return true;
    }

    public boolean startEditingPath(Player player, String pathName) {
        UUID playerId = player.getUniqueId();

        if (!loadedPaths.containsKey(pathName)) {
            return false;
        }

        if (playerEditSessions.containsKey(playerId)) {
            endEditingSession(playerId, false);
        }

        SelectionPath path = loadedPaths.get(pathName);
        PlayerEditSession session = new PlayerEditSession(playerId, pathName);

        for (SelectionRegion region : path.getRegions()) {
            session.addRegion(region.clone());
        }

        playerEditSessions.put(playerId, session);
        return true;
    }

    public boolean endEditingSession(UUID playerId, boolean saveChanges) {
        PlayerEditSession session = playerEditSessions.get(playerId);
        if (session == null) {
            return false;
        }

        if (saveChanges && session.hasRegions()) {
            saveSelectionPathToConfig(session.getEditingPathName(), session.getRegions());
        }

        playerEditSessions.remove(playerId);
        return true;
    }

    public boolean addRegionToEditingSession(UUID playerId, SelectionRegion region) {
        PlayerEditSession session = playerEditSessions.get(playerId);
        if (session == null) {
            return false;
        }

        session.addRegion(region);
        return true;
    }

    public boolean deleteRegionFromEditingSession(UUID playerId, int index) {
        PlayerEditSession session = playerEditSessions.get(playerId);
        if (session == null) {
            return false;
        }

        return session.removeRegion(index);
    }

    public boolean createNewPath(Player player, String pathName) {
        UUID playerId = player.getUniqueId();

        if (loadedPaths.containsKey(pathName)) {
            return false;
        }

        if (playerEditSessions.containsKey(playerId)) {
            endEditingSession(playerId, false);
        }

        PlayerEditSession session = new PlayerEditSession(playerId, pathName);
        playerEditSessions.put(playerId, session);

        return true;
    }

    public PlayerEditSession getPlayerEditSession(UUID playerId) {
        return playerEditSessions.get(playerId);
    }

    public Set<String> getLoadedPathNames() {
        return loadedPaths.keySet();
    }

    public SelectionPath getPath(String pathName) {
        return loadedPaths.get(pathName);
    }

    private void startParticleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();

                    PlayerEditSession editSession = playerEditSessions.get(playerId);
                    if (editSession != null) {
                        for (SelectionRegion region : editSession.getRegions()) {
                            showParticles(player, region);
                        }
                        if (editSession.getCurrentRegion() != null) {
                            showParticles(player, editSession.getCurrentRegion());
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 10L);
    }

    private void startProgressCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();
                    PathProgress progress = playerProgress.get(playerId);

                    if (progress != null) {
                        boolean updated = progress.checkProgress(player);

                        Long lastUpdate = lastActionBarUpdate.get(playerId);
                        if (lastUpdate == null || currentTime - lastUpdate > 100) {
                            sendActionBar(player, progress.getProgressMessage());
                            lastActionBarUpdate.put(playerId, currentTime);

                            if (progress.isCompleted() && !progress.isCleanupScheduled()) {
                                progress.setCleanupScheduled(true);

                                // 记录完成数据到数据库
                                recordCompletion(player, progress);

                                player.sendMessage("§a恭喜! 你完成了路径: §6" + progress.getPathName());
                                player.sendMessage("§a用时: §e" + TimeFormatter.formatMilliseconds(progress.getTimeTaken()));

                                // 发放奖励
                                boolean rewardSuccess = rewardManager.giveRewards(player, progress.getPathName());
                                if (!rewardSuccess) {
                                    player.sendMessage("§c奖励发放遇到问题，请联系管理员。");
                                }

                                Bukkit.getScheduler().runTaskLater(PathSelection.this, () -> {
                                    if (playerProgress.get(playerId) == progress) {
                                        playerProgress.remove(playerId);
                                        lastActionBarUpdate.remove(playerId);
                                        sendActionBar(player, "");
                                    }
                                }, 100L);
                            }
                        }

                        if (updated && !progress.isCompleted()) {
                            player.sendMessage("§a进度更新! 已通过第 " + progress.getCurrentRegionIndex() + "/" +
                                    progress.getTotalRegions() + " 个选区");
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    // 新增：记录完成数据
    private void recordCompletion(Player player, PathProgress progress) {
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        String pathName = progress.getPathName();
        long completionTime = System.currentTimeMillis();
        long timeTaken = progress.getTimeTaken();

        databaseManager.recordCompletion(playerId, playerName, pathName, completionTime, timeTaken);
    }

    // 新增：每日重置任务
    private void startDailyResetTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // 检查是否到了00:00
                Calendar now = Calendar.getInstance();
                if (now.get(Calendar.HOUR_OF_DAY) == 0 && now.get(Calendar.MINUTE) == 0) {
                    getLogger().info("每日重置时间到，准备重置奖励状态...");
                    // 实际重置逻辑在奖励领取时检查日期，这里只需要记录日志
                }
            }
        }.runTaskTimer(this, 0L, 1200L); // 每分钟检查一次
    }

    private void sendActionBar(Player player, String message) {
        try {
            player.sendActionBar(net.kyori.adventure.text.Component.text(message));
        } catch (Exception e) {
            player.sendTitle("", message, 0, 20, 0);
        }
    }

    public boolean startPathProgress(Player player, String pathName) {
        UUID playerId = player.getUniqueId();

        if (playerProgress.containsKey(playerId)) {
            return false;
        }

        SelectionPath path = loadedPaths.get(pathName);
        if (path == null || path.getRegions().isEmpty()) {
            return false;
        }

        PathProgress progress = new PathProgress(this, playerId, pathName, path.getRegions().size());
        playerProgress.put(playerId, progress);

        return true;
    }

    public PathProgress getPlayerProgress(UUID playerId) {
        return playerProgress.get(playerId);
    }

    public void cancelPathProgress(UUID playerId) {
        PathProgress progress = playerProgress.get(playerId);
        if (progress != null) {
            progress.setCleanupScheduled(true);
        }
        playerProgress.remove(playerId);
        lastActionBarUpdate.remove(playerId);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            sendActionBar(player, "");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("pathselection.pathselect")) {
            player.sendMessage(languageManager.getMessage("no-permission"));
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != selectionTool) {
            return;
        }

        if (event.getAction() != Action.LEFT_CLICK_BLOCK &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        event.setCancelled(true);

        PlayerEditSession session = playerEditSessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage(languageManager.getMessage("not-editing"));
            return;
        }

        Location clickedLoc = event.getClickedBlock().getLocation();
        SelectionRegion currentRegion = session.getCurrentRegion();

        if (currentRegion == null) {
            currentRegion = new SelectionRegion();
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            currentRegion.setStartPoint(clickedLoc);
            player.sendMessage(languageManager.getMessage("start-point-set",
                    "location", formatLocation(clickedLoc)));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            currentRegion.setEndPoint(clickedLoc);
            player.sendMessage(languageManager.getMessage("end-point-set",
                    "location", formatLocation(clickedLoc)));
        }

        session.setCurrentRegion(currentRegion);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (playerProgress.containsKey(playerId)) {
            return;
        }

        for (String pathName : loadedPaths.keySet()) {
            SelectionPath path = loadedPaths.get(pathName);
            if (path != null && !path.getRegions().isEmpty()) {
                SelectionRegion firstRegion = path.getRegions().get(0);
                if (firstRegion.contains(player.getLocation())) {
                    if (startPathProgress(player, pathName)) {
                        player.sendMessage(languageManager.getMessage("progress-auto-started",
                                "name", pathName));
                        player.sendMessage(languageManager.getMessage("hint-cancel-timer"));
                    }
                    break;
                }
            }
        }
    }

    private void showParticles(Player player, SelectionRegion region) {
        if (!region.isValid()) {
            return;
        }

        Location start = region.getStartPoint();
        Location end = region.getEndPoint();

        // 获取两个方块的整数坐标
        int x1 = Math.min(start.getBlockX(), end.getBlockX());
        int y1 = Math.min(start.getBlockY(), end.getBlockY());
        int z1 = Math.min(start.getBlockZ(), end.getBlockZ());
        int x2 = Math.max(start.getBlockX(), end.getBlockX());
        int y2 = Math.max(start.getBlockY(), end.getBlockY());
        int z2 = Math.max(start.getBlockZ(), end.getBlockZ());

        // 计算包含这两个方块的最小立方体的边界
        int minX = x1;
        int maxX = x2 + 1;
        int minY = y1;
        int maxY = y2 + 1;
        int minZ = z1;
        int maxZ = z2 + 1;

        World world = start.getWorld();

        // 计算选区尺寸，动态调整粒子密度
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        // 动态步长：大选区减少粒子密度
        int stepX = Math.max(1, sizeX / 20);
        int stepY = Math.max(1, sizeY / 10);
        int stepZ = Math.max(1, sizeZ / 20);

        // 显示包含两个选中方块的最小立方体的12条边

        // 底部4条边 (Y = minY)
        for (int x = minX; x <= maxX; x += stepX) {
            // 前下边
            spawnParticleAtEdge(player, world, x, minY, minZ);
            // 后下边
            spawnParticleAtEdge(player, world, x, minY, maxZ);
        }
        for (int z = minZ; z <= maxZ; z += stepZ) {
            // 左下边
            spawnParticleAtEdge(player, world, minX, minY, z);
            // 右下边
            spawnParticleAtEdge(player, world, maxX, minY, z);
        }

        // 顶部4条边 (Y = maxY)
        for (int x = minX; x <= maxX; x += stepX) {
            // 前上边
            spawnParticleAtEdge(player, world, x, maxY, minZ);
            // 后上边
            spawnParticleAtEdge(player, world, x, maxY, maxZ);
        }
        for (int z = minZ; z <= maxZ; z += stepZ) {
            // 左上边
            spawnParticleAtEdge(player, world, minX, maxY, z);
            // 右上边
            spawnParticleAtEdge(player, world, maxX, maxY, z);
        }

        // 4条垂直边
        for (int y = minY; y <= maxY; y += stepY) {
            // 左前竖边
            spawnParticleAtEdge(player, world, minX, y, minZ);
            // 右前竖边
            spawnParticleAtEdge(player, world, maxX, y, minZ);
            // 左后竖边
            spawnParticleAtEdge(player, world, minX, y, maxZ);
            // 右后竖边
            spawnParticleAtEdge(player, world, maxX, y, maxZ);
        }
    }

    /**
     * 在方块边缘生成粒子（不是在中心）
     */
    private void spawnParticleAtEdge(Player player, World world, int x, int y, int z) {
        // 在方块的边缘生成粒子，而不是中心
        // 这里我们选择在方块的底部前角生成粒子
        Location particleLoc = new Location(world, x, y, z);
        player.spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 1, 0, 0, 0, 0);
    }

    private String formatLocation(Location loc) {
        return String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private String getRegionType(int index, int total) {
        if (index == 0) return "start";
        if (index == total - 1) return "end";
        return "middle";
    }

    private Map<String, Object> serializeLocation(Location loc) {
        Map<String, Object> data = new HashMap<>();
        data.put("x", loc.getBlockX());
        data.put("y", loc.getBlockY());
        data.put("z", loc.getBlockZ());
        return data;
    }

    public Material getSelectionTool() {
        return selectionTool;
    }

    public void setSelectionTool(Material tool) {
        this.selectionTool = tool;
        config.set("selection-tool", tool.name());
        saveConfig();
    }
}