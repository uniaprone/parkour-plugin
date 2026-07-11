package org.zzq.pathSelection;

import org.bukkit.entity.Player;
import org.zzq.pathSelection.util.TimeFormatter;

import java.util.UUID;

public class PathProgress {
    private final PathSelection plugin;
    private final UUID playerId;
    private final String pathName;
    private final long startTime;
    private int currentRegionIndex;
    private final int totalRegions;
    private boolean completed;
    private Long completionTime;
    private boolean cleanupScheduled = false;

    public PathProgress(PathSelection plugin, UUID playerId, String pathName, int totalRegions) {
        this.plugin = plugin;
        this.playerId = playerId;
        this.pathName = pathName;
        this.startTime = System.currentTimeMillis();
        this.currentRegionIndex = 0;
        this.totalRegions = totalRegions;
        this.completed = false;
    }

    public boolean checkProgress(Player player) {
        if (completed || cleanupScheduled) return true;

        SelectionPath path = plugin.getPath(pathName);
        if (path == null || path.getRegions().size() != totalRegions) {
            return false;
        }

        if (currentRegionIndex < path.getRegions().size()) {
            SelectionRegion targetRegion = path.getRegions().get(currentRegionIndex);
            if (targetRegion.contains(player.getLocation())) {
                currentRegionIndex++;

                if (currentRegionIndex >= totalRegions) {
                    completed = true;
                    completionTime = System.currentTimeMillis();
                    return true;
                }
                return true;
            }
        }

        return false;
    }

    public String getProgressMessage() {
        if (completed) {
            long timeTaken = getTimeTaken();
            return String.format("§a路径完成! §6%s §a- 时间: §e%s",
                    pathName, TimeFormatter.formatMilliseconds(timeTaken));
        }

        long currentTime = getCurrentTime();
        return String.format("§b路径: §6%s §b- 进度: §e%d/%d §b- 时间: §e%s",
                pathName, currentRegionIndex, totalRegions, TimeFormatter.formatMilliseconds(currentTime));
    }

    public long getTimeTaken() {
        if (completionTime != null) {
            return completionTime - startTime;
        }
        return getCurrentTime();
    }

    public long getCurrentTime() {
        return System.currentTimeMillis() - startTime;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCleanupScheduled(boolean scheduled) {
        this.cleanupScheduled = scheduled;
    }

    public boolean isCleanupScheduled() {
        return cleanupScheduled;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPathName() {
        return pathName;
    }

    public int getCurrentRegionIndex() {
        return currentRegionIndex;
    }

    public int getTotalRegions() {
        return totalRegions;
    }

    public long getStartTime() {
        return startTime;
    }

    public Long getCompletionTime() {
        return completionTime;
    }
}