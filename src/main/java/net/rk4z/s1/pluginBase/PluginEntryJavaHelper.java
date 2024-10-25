package net.rk4z.s1.pluginBase;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class PluginEntryJavaHelper {
    private final File configFile;
    private final Yaml yaml;

    public PluginEntryJavaHelper(PluginEntry pe) {
        this.configFile = pe.getConfigFile();
        this.yaml = pe.getYaml();
    }

    public <T> T lc(String key, Class<T> type) {
        try (InputStream inputStream = Files.newInputStream(configFile.toPath())) {
            Map<String, Object> config = yaml.load(inputStream);
            Object value = config.get(key);

            if (value == null) {
                return null;
            }

            if (type == String.class) {
                return type.cast(value.toString());
            } else if (type == Integer.class) {
                return type.cast(Integer.parseInt(value.toString()));
            } else if (type == Boolean.class) {
                return type.cast(toBooleanOrNull(value.toString()));
            } else if (type == Double.class) {
                return type.cast(Double.parseDouble(value.toString()));
            } else if (type == Short.class) {
                return type.cast(Short.parseShort(value.toString()));
            } else if (type == Long.class) {
                return type.cast(Long.parseLong(value.toString()));
            } else if (type == Float.class) {
                return type.cast(Float.parseFloat(value.toString()));
            } else if (type == Byte.class) {
                return type.cast(Byte.parseByte(value.toString()));
            } else if (type == Character.class) {
                return type.cast(value.toString().charAt(0));
            } else if (type == List.class) {
                return type.cast(value);
            } else if (type == Map.class) {
                return type.cast(value);
            } else if (type == BigInteger.class) {
                return type.cast(new BigInteger(value.toString()));
            } else if (type == BigDecimal.class) {
                return type.cast(new BigDecimal(value.toString()));
            }

            return type.cast(value);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Boolean toBooleanOrNull(String value) {
        return switch (value.trim().toLowerCase()) {
            case "true", "1", "t" -> true;
            case "false", "0", "f" -> false;
            default -> null;
        };
    }
}
