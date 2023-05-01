package club.hellin.hft.util;

import org.apache.commons.io.FileUtils;
import org.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class Config {
    private static final YamlConfiguration config = getConfig();
    private static final ConfigurationSection general = config.getConfigurationSection("general");
    private static final ConfigurationSection client = config.getConfigurationSection("client");
    private static final ConfigurationSection server = config.getConfigurationSection("server");

    public static Mode getMode() {
        return Mode.valueOf(config.getString("mode"));
    }

    public static String getHost() {
        return general.getString("host");
    }

    public static int getPort() {
        return general.getInt("port");
    }

    public static String getProtocol() { return client.getString("protocol"); }

    public static List<String> getIpWhitelist() {
        return server.getStringList("ip-whitelist");
    }

    public static String getRootFilePath() { return server.getString("root-file-path"); }

    public static boolean shouldOverride() { return server.getBoolean("override"); }

    private static YamlConfiguration getConfig() {
        final File config = new File("./config.yml");

        try {
            if (!config.exists() || config.length() == 0) { // If it already exists just skip over this
                final InputStream resource = Config.class.getClassLoader().getResourceAsStream("config.yml");
                FileUtils.copyInputStreamToFile(resource, config);

                System.out.println("Generated config for first time run.");
                System.exit(0);
            }
        } catch (final IOException ignored) {
            return null;
        }

        return YamlConfiguration.loadConfiguration(config);
    }
}