package org.zzq.pathSelection.database;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class DatabaseManager {
    private final Logger logger;
    private Connection connection;
    private final String databaseUrl;

    public DatabaseManager(Logger logger, String databasePath) {
        this.logger = logger;
        this.databaseUrl = "jdbc:sqlite:" + databasePath;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            // 创建表
            String[] createTables = {
                    "CREATE TABLE IF NOT EXISTS completions (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "player_uuid VARCHAR(36) NOT NULL, " +
                            "player_name VARCHAR(50) NOT NULL, " +
                            "path_name VARCHAR(100) NOT NULL, " +
                            "completion_time BIGINT NOT NULL, " +
                            "time_taken BIGINT NOT NULL, " +
                            "created_at BIGINT NOT NULL)",

                    "CREATE TABLE IF NOT EXISTS rewards (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "path_name VARCHAR(100) NOT NULL, " +
                            "command TEXT NOT NULL, " +
                            "created_at BIGINT NOT NULL)",

                    "CREATE TABLE IF NOT EXISTS daily_rewards (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "player_uuid VARCHAR(36) NOT NULL, " +
                            "path_name VARCHAR(100) NOT NULL, " +
                            "reward_date VARCHAR(10) NOT NULL, " +
                            "created_at BIGINT NOT NULL, " +
                            "UNIQUE(player_uuid, path_name, reward_date))"
            };

            for (String sql : createTables) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
            }

            // 创建索引
            String[] createIndexes = {
                    "CREATE INDEX IF NOT EXISTS idx_completions_path ON completions(path_name)",
                    "CREATE INDEX IF NOT EXISTS idx_completions_player ON completions(player_uuid)",
                    "CREATE INDEX IF NOT EXISTS idx_completions_time ON completions(completion_time)",
                    "CREATE INDEX IF NOT EXISTS idx_daily_rewards_date ON daily_rewards(reward_date)"
            };

            for (String sql : createIndexes) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
            }

            logger.info("数据库初始化完成");
        } catch (SQLException e) {
            logger.severe("数据库初始化失败: " + e.getMessage());
        }
    }

    // 修复：添加奖励
    public boolean addReward(String pathName, String command) {
        String sql = "INSERT INTO rewards (path_name, command, created_at) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pathName);
            stmt.setString(2, command);
            stmt.setLong(3, System.currentTimeMillis());

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            logger.severe("添加奖励失败: " + e.getMessage());
            return false;
        }
    }

    // 修复：获取奖励
    public List<String> getRewards(String pathName) {
        List<String> rewards = new ArrayList<>();
        String sql = "SELECT command FROM rewards WHERE path_name = ? ORDER BY id ASC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pathName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                rewards.add(rs.getString("command"));
            }
        } catch (SQLException e) {
            logger.severe("获取奖励失败: " + e.getMessage());
        }

        return rewards;
    }

    // 新增：清空奖励
    public boolean clearRewards(String pathName) {
        String sql = "DELETE FROM rewards WHERE path_name = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pathName);
            int result = stmt.executeUpdate();
            return result >= 0; // 即使没有记录也返回true
        } catch (SQLException e) {
            logger.severe("清空奖励失败: " + e.getMessage());
            return false;
        }
    }

    // 修复：记录每日奖励
    public boolean recordDailyReward(UUID playerUuid, String pathName, String rewardDate) {
        String sql = "INSERT OR IGNORE INTO daily_rewards (player_uuid, path_name, reward_date, created_at) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, pathName);
            stmt.setString(3, rewardDate);
            stmt.setLong(4, System.currentTimeMillis());

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            logger.severe("记录每日奖励失败: " + e.getMessage());
            return false;
        }
    }

    // 修复：检查每日奖励
    public boolean hasClaimedDailyReward(UUID playerUuid, String pathName, String rewardDate) {
        String sql = "SELECT 1 FROM daily_rewards WHERE player_uuid = ? AND path_name = ? AND reward_date = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, pathName);
            stmt.setString(3, rewardDate);

            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.severe("检查每日奖励失败: " + e.getMessage());
            return false;
        }
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(databaseUrl);
        }
        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.warning("关闭数据库连接时出错: " + e.getMessage());
            }
        }
    }

    // 记录完成
    public boolean recordCompletion(UUID playerUuid, String playerName, String pathName,
                                    long completionTime, long timeTaken) {
        String sql = "INSERT INTO completions (player_uuid, player_name, path_name, completion_time, time_taken, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, pathName);
            stmt.setLong(4, completionTime);
            stmt.setLong(5, timeTaken);
            stmt.setLong(6, System.currentTimeMillis());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("记录完成数据失败: " + e.getMessage());
            return false;
        }
    }

    // 获取每日排行榜（前10名）
    public List<CompletionRecord> getDailyLeaderboard(String pathName, String date) {
        return getLeaderboard(pathName, date, 10);
    }

    // 获取历史排行榜（前10名）
    public List<CompletionRecord> getHistoricalLeaderboard(String pathName) {
        return getLeaderboard(pathName, null, 10);
    }

    private List<CompletionRecord> getLeaderboard(String pathName, String date, int limit) {
        List<CompletionRecord> leaderboard = new ArrayList<>();
        String sql;

        if (date != null) {
            sql = "SELECT c.* FROM completions c " +
                    "WHERE c.path_name = ? AND strftime('%Y-%m-%d', datetime(c.completion_time/1000, 'unixepoch')) = ? " +
                    "ORDER BY c.time_taken ASC LIMIT ?";
        } else {
            sql = "SELECT c.* FROM completions c " +
                    "WHERE c.path_name = ? " +
                    "ORDER BY c.time_taken ASC LIMIT ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pathName);
            if (date != null) {
                stmt.setString(2, date);
                stmt.setInt(3, limit);
            } else {
                stmt.setInt(2, limit);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                CompletionRecord record = new CompletionRecord(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getString("path_name"),
                        rs.getLong("completion_time"),
                        rs.getLong("time_taken")
                );
                leaderboard.add(record);
            }
        } catch (SQLException e) {
            logger.severe("获取排行榜失败: " + e.getMessage());
        }

        return leaderboard;
    }

    // 获取玩家的最佳记录
    public CompletionRecord getPlayerBestRecord(UUID playerUuid, String pathName) {
        String sql = "SELECT * FROM completions WHERE player_uuid = ? AND path_name = ? ORDER BY time_taken ASC LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, pathName);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new CompletionRecord(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getString("path_name"),
                        rs.getLong("completion_time"),
                        rs.getLong("time_taken")
                );
            }
        } catch (SQLException e) {
            logger.severe("获取玩家最佳记录失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 删除整个路径的所有记录
     */
    public int deletePathRecords(String pathName) {
        String sql = "DELETE FROM completions WHERE path_name = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pathName);
            return stmt.executeUpdate();

        } catch (SQLException e) {
            logger.severe("删除路径记录失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 删除玩家在特定路径的所有记录
     */
    public int deletePlayerPathRecords(UUID playerUuid, String pathName) {
        String sql = "DELETE FROM completions WHERE player_uuid = ? AND path_name = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, pathName);
            return stmt.executeUpdate();

        } catch (SQLException e) {
            logger.severe("删除玩家路径记录失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 删除特定记录（按日期）
     */
    public int deleteSpecificRecord(UUID playerUuid, String pathName, String date) {
        String sql = "DELETE FROM completions WHERE player_uuid = ? AND path_name = ? AND date(completion_time/1000, 'unixepoch') = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, pathName);
            stmt.setString(3, date);
            return stmt.executeUpdate();

        } catch (SQLException e) {
            logger.severe("删除特定记录失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 根据玩家名获取UUID（从数据库中查找）
     */
    public UUID getPlayerUuidByName(String playerName) {
        String sql = "SELECT DISTINCT player_uuid FROM completions WHERE player_name = ? LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return UUID.fromString(rs.getString("player_uuid"));
            }
        } catch (SQLException e) {
            logger.severe("获取玩家UUID失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 获取在特定路径有记录的玩家名列表
     */
    public List<String> getPlayersWithRecords(String pathName) {
        List<String> players = new ArrayList<>();
        String sql = "SELECT DISTINCT player_name FROM completions WHERE path_name = ? ORDER BY player_name";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pathName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                players.add(rs.getString("player_name"));
            }
        } catch (SQLException e) {
            logger.severe("获取玩家列表失败: " + e.getMessage());
        }

        return players;
    }

    /**
     * 获取玩家在特定路径的记录日期列表
     */
    public List<String> getPlayerRecordDates(UUID playerUuid, String pathName) {
        List<String> dates = new ArrayList<>();
        String sql = "SELECT DISTINCT date(completion_time/1000, 'unixepoch') as record_date " +
                "FROM completions WHERE player_uuid = ? AND path_name = ? " +
                "ORDER BY record_date DESC LIMIT 10";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, pathName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                dates.add(rs.getString("record_date"));
            }
        } catch (SQLException e) {
            logger.severe("获取记录日期失败: " + e.getMessage());
        }

        return dates;
    }
}
