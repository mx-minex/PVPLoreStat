package kr.minex.pvplorestat.infrastructure.lore;

import kr.minex.pvplorestat.domain.model.StatType;

import java.util.*;

/**
 * 로어 템플릿 설정
 * <p>
 * 로어에 표시되는 스탯의 형식과 순서를 정의합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class LoreTemplate {

    private final Map<StatType, String> formats;
    private final List<StatType> order;
    private final String separatorTop;
    private final String separatorBottom;
    private final boolean separatorEnabled;

    private LoreTemplate(Map<StatType, String> formats, List<StatType> order,
                         String separatorTop, String separatorBottom, boolean separatorEnabled) {
        this.formats = Collections.unmodifiableMap(new EnumMap<>(formats));
        this.order = Collections.unmodifiableList(new ArrayList<>(order));
        this.separatorTop = separatorTop;
        this.separatorBottom = separatorBottom;
        this.separatorEnabled = separatorEnabled;
    }

    /**
     * 기본 템플릿을 생성합니다.
     *
     * @return 기본 템플릿
     */
    public static LoreTemplate defaultTemplate() {
        Map<StatType, String> formats = new EnumMap<>(StatType.class);
        formats.put(StatType.DAMAGE, "&c⚔ 공격력 &f+{value}");
        formats.put(StatType.DEFENSE, "&9🛡 방어력 &f+{value}");
        formats.put(StatType.HEALTH, "&6❤ 체력 &f+{value}");
        formats.put(StatType.LIFESTEAL, "&4🩸 피흡수 &f{value}%");
        formats.put(StatType.CRIT_CHANCE, "&e⚡ 치명타 확률 &f{value}%");
        formats.put(StatType.CRIT_DAMAGE, "&5💥 치명타 데미지 &f+{value}");
        formats.put(StatType.DODGE, "&b💨 회피율 &f{value}%");

        List<StatType> order = Arrays.asList(
                StatType.DAMAGE,
                StatType.DEFENSE,
                StatType.HEALTH,
                StatType.LIFESTEAL,
                StatType.CRIT_CHANCE,
                StatType.CRIT_DAMAGE,
                StatType.DODGE
        );

        return new LoreTemplate(
                formats,
                order,
                "&8&m─────&r &6✦ 스탯 &8&m─────",
                "&8&m──────────────────",
                true
        );
    }

    /**
     * 스탯 타입에 대한 포맷 문자열을 반환합니다.
     *
     * @param type 스탯 타입
     * @return 포맷 문자열, 없으면 null
     */
    public String getFormat(StatType type) {
        return formats.get(type);
    }

    /**
     * 스탯 값을 포맷팅된 문자열로 변환합니다.
     *
     * @param type  스탯 타입
     * @param value 스탯 값
     * @return 포맷팅된 문자열
     */
    public String formatStat(StatType type, double value) {
        String format = formats.get(type);
        if (format == null) {
            return null;
        }

        // 정수인 경우 소수점 제거
        String valueStr = (value == Math.floor(value)) ?
                String.valueOf((int) value) :
                String.valueOf(value);

        return translateColorCodes(format.replace("{value}", valueStr));
    }

    /**
     * 스탯 표시 순서를 반환합니다.
     *
     * @return 순서 목록 (불변)
     */
    public List<StatType> getOrder() {
        return order;
    }

    /**
     * 상단 구분선을 반환합니다.
     *
     * @return 상단 구분선
     */
    public String getSeparatorTop() {
        return translateColorCodes(separatorTop);
    }

    /**
     * 하단 구분선을 반환합니다.
     *
     * @return 하단 구분선
     */
    public String getSeparatorBottom() {
        return translateColorCodes(separatorBottom);
    }

    /**
     * 구분선 사용 여부를 반환합니다.
     *
     * @return 구분선 사용 시 true
     */
    public boolean isSeparatorEnabled() {
        return separatorEnabled;
    }

    /**
     * & 색상 코드를 § 로 변환합니다.
     *
     * @param text 변환할 텍스트
     * @return 변환된 텍스트
     */
    public static String translateColorCodes(String text) {
        if (text == null) {
            return null;
        }
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                chars[i] = '§';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    /**
     * 빌더를 생성합니다.
     *
     * @return 새 빌더
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * LoreTemplate 빌더
     */
    public static class Builder {
        private final Map<StatType, String> formats = new EnumMap<>(StatType.class);
        private List<StatType> order = new ArrayList<>();
        private String separatorTop = "&8&m─────&r &6✦ 스탯 &8&m─────";
        private String separatorBottom = "&8&m──────────────────";
        private boolean separatorEnabled = true;

        public Builder format(StatType type, String format) {
            formats.put(type, format);
            return this;
        }

        public Builder order(List<StatType> order) {
            this.order = new ArrayList<>(order);
            return this;
        }

        public Builder separatorTop(String separatorTop) {
            this.separatorTop = separatorTop;
            return this;
        }

        public Builder separatorBottom(String separatorBottom) {
            this.separatorBottom = separatorBottom;
            return this;
        }

        public Builder separatorEnabled(boolean enabled) {
            this.separatorEnabled = enabled;
            return this;
        }

        public LoreTemplate build() {
            // 기본값 설정
            if (order.isEmpty()) {
                order = Arrays.asList(StatType.values());
            }
            return new LoreTemplate(formats, order, separatorTop, separatorBottom, separatorEnabled);
        }
    }
}
