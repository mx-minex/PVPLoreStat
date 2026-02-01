package kr.minex.pvplorestat.infrastructure.config;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.List;

/**
 * 메시지 관리자
 * <p>
 * messages.yml을 관리하고 플레이스홀더 치환 기능을 제공합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class MessageManager {

    private final JavaPlugin plugin;
    private FileConfiguration messages;
    private String prefix;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * 메시지 설정을 리로드합니다.
     */
    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");

        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(file);

        // 기본값 병합
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
        }

        prefix = translateColor(messages.getString("prefix", "&6[PLS] &f"));
    }

    /**
     * 메시지를 가져옵니다.
     *
     * @param key 메시지 키 (예: "commands.set.success")
     * @return 메시지 (접두사 미포함)
     */
    public String get(String key) {
        String message = messages.getString(key);
        if (message == null) {
            plugin.getLogger().warning("메시지 키를 찾을 수 없습니다: " + key);
            return "§c[메시지 없음: " + key + "]";
        }
        return translateColor(message);
    }

    /**
     * 메시지를 가져와서 플레이스홀더를 치환합니다.
     *
     * @param key          메시지 키
     * @param placeholders 플레이스홀더 쌍 (키, 값, 키, 값, ...)
     * @return 치환된 메시지 (접두사 미포함)
     */
    public String get(String key, Object... placeholders) {
        String message = get(key);
        return replacePlaceholders(message, placeholders);
    }

    /**
     * 메시지를 발송자에게 전송합니다 (접두사 포함).
     *
     * @param sender       발송 대상
     * @param key          메시지 키
     * @param placeholders 플레이스홀더 쌍
     */
    public void send(CommandSender sender, String key, Object... placeholders) {
        String message = get(key, placeholders);
        sender.sendMessage(prefix + message);
    }

    /**
     * 메시지를 발송자에게 전송합니다 (접두사 미포함).
     *
     * @param sender       발송 대상
     * @param key          메시지 키
     * @param placeholders 플레이스홀더 쌍
     */
    public void sendRaw(CommandSender sender, String key, Object... placeholders) {
        String message = get(key, placeholders);
        sender.sendMessage(message);
    }

    /**
     * 원시 메시지를 전송합니다.
     *
     * @param sender  발송 대상
     * @param message 원시 메시지
     */
    public void sendRawMessage(CommandSender sender, String message) {
        sender.sendMessage(translateColor(message));
    }

    /**
     * 메시지 목록을 가져옵니다.
     *
     * @param key 메시지 키
     * @return 메시지 목록
     */
    public List<String> getList(String key) {
        List<String> list = messages.getStringList(key);
        return list.stream()
                .map(this::translateColor)
                .toList();
    }

    /**
     * 메시지 목록을 가져와서 플레이스홀더를 치환합니다.
     *
     * @param key          메시지 키
     * @param placeholders 플레이스홀더 쌍
     * @return 치환된 메시지 목록
     */
    public List<String> getList(String key, Object... placeholders) {
        List<String> list = getList(key);
        return list.stream()
                .map(line -> replacePlaceholders(line, placeholders))
                .toList();
    }

    /**
     * 접두사를 반환합니다.
     *
     * @return 접두사
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * 플레이스홀더를 치환합니다.
     *
     * @param message      원본 메시지
     * @param placeholders 플레이스홀더 쌍 (키, 값, 키, 값, ...)
     * @return 치환된 메시지
     */
    private String replacePlaceholders(String message, Object... placeholders) {
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = "{" + placeholders[i] + "}";
            String value = formatValue(placeholders[i + 1]);
            message = message.replace(placeholder, value);
        }
        return message;
    }

    /**
     * 값을 문자열로 포맷팅합니다.
     *
     * @param value 값
     * @return 포맷팅된 문자열
     */
    private String formatValue(Object value) {
        if (value instanceof Number number) {
            // 정수인 경우 소수점 없이
            if (number.doubleValue() == Math.floor(number.doubleValue())) {
                return String.valueOf(number.intValue());
            }
            // 숫자는 천 단위 구분
            return NumberFormat.getInstance().format(number);
        }
        return String.valueOf(value);
    }

    /**
     * & 색상 코드를 § 로 변환합니다.
     *
     * @param text 원본 텍스트
     * @return 변환된 텍스트
     */
    private String translateColor(String text) {
        if (text == null) {
            return null;
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * 원본 메시지 설정을 반환합니다.
     *
     * @return 메시지 설정
     */
    public FileConfiguration getMessages() {
        return messages;
    }
}
