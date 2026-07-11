package org.zzq.pathSelection;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final PathSelection plugin;
    private FileConfiguration languageConfig;
    private File languageFile;
    private Map<String, String> messages;

    public LanguageManager(PathSelection plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
    }

    public void loadLanguage() {
        languageFile = new File(plugin.getDataFolder(), "language.yml");
        if (!languageFile.exists()) {
            if (!languageFile.getParentFile().exists()) {
                languageFile.getParentFile().mkdirs();
            }
            plugin.saveResource("language.yml", false);
        }

        reloadLanguage();
    }

    // 新增：公开的重载方法
    public void reloadLanguage() {
        languageConfig = YamlConfiguration.loadConfiguration(languageFile);
        loadMessages();
    }

    private void loadMessages() {
        messages.clear();

        for (String key : languageConfig.getKeys(true)) {
            if (languageConfig.isString(key)) {
                messages.put(key, languageConfig.getString(key));
            }
        }

        plugin.getLogger().info("已加载 " + messages.size() + " 条语言消息");
    }

    public String getMessage(String key) {
        return getMessage(key, new HashMap<>());
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = messages.getOrDefault(key, "&cMissing message: " + key);

        message = ChatColor.translateAlternateColorCodes('&', message);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return message;
    }

    public String getMessage(String key, String... placeholderPairs) {
        if (placeholderPairs.length % 2 != 0) {
            throw new IllegalArgumentException("占位符参数必须是成对的");
        }

        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < placeholderPairs.length; i += 2) {
            placeholders.put(placeholderPairs[i], placeholderPairs[i + 1]);
        }

        return getMessage(key, placeholders);
    }

    public void saveLanguage() {
        try {
            languageConfig.save(languageFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存语言文件时出错: " + e.getMessage());
        }
    }
}