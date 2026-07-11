package org.zzq.pathSelection.database;

import java.util.UUID;

public class CompletionRecord {
    private final UUID playerUuid;
    private final String playerName;
    private final String pathName;
    private final long completionTime;
    private final long timeTaken;

    public CompletionRecord(UUID playerUuid, String playerName, String pathName,
                            long completionTime, long timeTaken) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.pathName = pathName;
        this.completionTime = completionTime;
        this.timeTaken = timeTaken;
    }

    // Getters
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getPathName() { return pathName; }
    public long getCompletionTime() { return completionTime; }
    public long getTimeTaken() { return timeTaken; }

    // 格式化时间
    public String getFormattedTime() {
        long minutes = timeTaken / 60000;
        long seconds = (timeTaken % 60000) / 1000;
        long milliseconds = timeTaken % 1000;
        return String.format("%d:%02d.%03d", minutes, seconds, milliseconds);
    }

    public String getSimpleFormattedTime() {
        long seconds = timeTaken / 1000;
        long milliseconds = timeTaken % 1000;
        return String.format("%d.%03d秒", seconds, milliseconds);
    }
}
