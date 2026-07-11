package org.zzq.pathSelection;

import org.zzq.pathSelection.database.CompletionRecord;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.zzq.pathSelection.util.TimeFormatter;

import java.util.List;

public class PlaceholderAPIExpansion extends PlaceholderExpansion {

    private final PathSelection plugin;

    public PlaceholderAPIExpansion(PathSelection plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "pathselection";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String[] args = params.split("_");

        if (args.length < 2) {
            return "Invalid placeholder";
        }

        String pathName = args[0];
        String type = args[1];

        switch (type.toLowerCase()) {
            case "daily":
                return getDailyLeaderboardPlaceholder(pathName, args);
            case "historical":
                return getHistoricalLeaderboardPlaceholder(pathName, args);
            case "player":
                return getPlayerPlaceholder(player, pathName, args);
            default:
                return "Unknown type";
        }
    }

    private String getDailyLeaderboardPlaceholder(String pathName, String[] args) {
        if (args.length < 4) {
            return "Invalid format: path_daily_rank_position";
        }

        try {
            int rank = Integer.parseInt(args[2]);
            String field = args[3];
            String date = TimeFormatter.getCurrentDate();

            List<CompletionRecord> leaderboard = plugin.getDatabaseManager()
                    .getDailyLeaderboard(pathName, date);

            if (rank < 1 || rank > leaderboard.size()) {
                return "N/A";
            }

            CompletionRecord record = leaderboard.get(rank - 1);
            return getRecordField(record, field);
        } catch (NumberFormatException e) {
            return "Invalid rank";
        }
    }

    private String getHistoricalLeaderboardPlaceholder(String pathName, String[] args) {
        if (args.length < 4) {
            return "Invalid format: path_historical_rank_position";
        }

        try {
            int rank = Integer.parseInt(args[2]);
            String field = args[3];

            List<CompletionRecord> leaderboard = plugin.getDatabaseManager()
                    .getHistoricalLeaderboard(pathName);

            if (rank < 1 || rank > leaderboard.size()) {
                return "N/A";
            }

            CompletionRecord record = leaderboard.get(rank - 1);
            return getRecordField(record, field);
        } catch (NumberFormatException e) {
            return "Invalid rank";
        }
    }

    private String getPlayerPlaceholder(OfflinePlayer player, String pathName, String[] args) {
        if (args.length < 3) {
            return "Invalid format: path_player_field";
        }

        if (player == null) {
            return "Player not found";
        }

        String field = args[2];
        CompletionRecord record = plugin.getDatabaseManager()
                .getPlayerBestRecord(player.getUniqueId(), pathName);

        if (record == null) {
            return "N/A";
        }

        return getRecordField(record, field);
    }

    private String getRecordField(CompletionRecord record, String field) {
        switch (field.toLowerCase()) {
            case "player":
                return record.getPlayerName();
            case "time":
                return record.getFormattedTime();
            case "time_simple":
                return record.getSimpleFormattedTime();
            case "timestamp":
                return String.valueOf(record.getCompletionTime());
            case "milliseconds":
                return String.valueOf(record.getTimeTaken());
            case "date":
                return new java.util.Date(record.getCompletionTime()).toString();
            default:
                return "Unknown field";
        }
    }
}
