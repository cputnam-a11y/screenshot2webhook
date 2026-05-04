package io.github.cputnama11y.screenshot2webhook.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record Config(
        String discordWebhookUrl
) {
    public static Config loadConfig() throws IOException {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("screenshot2webhook.properties");
        Properties properties = new Properties();

        if (Files.exists(path)) {
            properties.load(Files.newBufferedReader(path));
        }

        var config = new Config(
                get(properties, "discordWebhookUrl", "")
        );

        try (OutputStream os = Files.newOutputStream(path)) {
            properties.store(os, "Screenshot to webhook config");
        }

        return config;
    }

    private static String get(Properties properties, String key, String defaultValue) {
        if (!properties.containsKey(key)) {
            properties.setProperty(key, defaultValue);
        }

        return properties.getProperty(key);
    }
}
