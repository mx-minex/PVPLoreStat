package kr.minex.pvplorestat.infrastructure.lore;

import kr.minex.pvplorestat.domain.model.ItemStats;
import kr.minex.pvplorestat.domain.model.StatType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 로어 관리자
 * <p>
 * 아이템 로어에서 스탯을 파싱하고, 생성하고, 수정하는 기능을 제공합니다.
 * config.yml의 lore.format 설정을 기반으로 정확히 일치하는 로어만 파싱합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class LoreManager {

    private final LoreTemplate template;
    private final Map<StatType, Pattern> parsePatterns;
    private final String separatorTopStripped;
    private final String separatorBottomStripped;

    private static final Pattern RGB_COLOR_PATTERN = Pattern.compile("§x(§[0-9a-fA-F]){6}");
    private static final Pattern SECTION_COLOR_PATTERN = Pattern.compile("§[0-9a-fA-Fk-oK-OrRxX]");
    private static final Pattern AMPERSAND_COLOR_PATTERN = Pattern.compile("&[0-9a-fA-Fk-oK-OrRxX]");
    private static final Pattern SEPARATOR_LINE_PATTERN = Pattern.compile("^[─\\-━═]+$");

    // {value} 플레이스홀더 패턴
    private static final Pattern VALUE_PLACEHOLDER = Pattern.compile("\\{value}");

    /**
     * LoreManager를 생성합니다.
     *
     * @param template 로어 템플릿
     */
    public LoreManager(LoreTemplate template) {
        this.template = template;
        this.parsePatterns = buildParsePatternsFromTemplate();
        this.separatorTopStripped = stripColor(template.getSeparatorTop());
        this.separatorBottomStripped = stripColor(template.getSeparatorBottom());
    }

    /**
     * 템플릿의 format 설정을 기반으로 파싱 패턴을 생성합니다.
     * <p>
     * 예: "&c⚔ 공격력 &f+{value}" -> "⚔ 공격력 \+([0-9.]+)"
     * </p>
     */
    private Map<StatType, Pattern> buildParsePatternsFromTemplate() {
        Map<StatType, Pattern> patterns = new EnumMap<>(StatType.class);

        for (StatType type : StatType.values()) {
            String format = template.getFormat(type);
            if (format == null || format.isEmpty()) {
                continue;
            }

            // 색상 코드 제거
            String stripped = stripColor(format);

            // {value} 위치 확인
            if (!stripped.contains("{value}")) {
                continue;
            }

            // 정규식 특수문자 이스케이프 (단, {value}는 나중에 처리)
            String escaped = escapeRegexExceptValue(stripped);

            // {value}를 숫자 캡처 그룹으로 치환
            // 퍼센트 스탯이면 %도 선택적으로 매칭
            String valuePattern = type.isPercent() ? "([0-9.]+)%?" : "([0-9.]+)";
            String regex = escaped.replace("\\{value\\}", valuePattern);

            try {
                patterns.put(type, Pattern.compile(regex));
            } catch (Exception e) {
                // 패턴 컴파일 실패 시 무시 (잘못된 설정)
            }
        }

        return patterns;
    }

    /**
     * 정규식 특수문자를 이스케이프합니다. ({value}는 보존)
     */
    private String escapeRegexExceptValue(String text) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            // {value} 패턴 확인
            if (i + 7 <= text.length() && text.substring(i, i + 7).equals("{value}")) {
                sb.append("\\{value\\}");
                i += 7;
                continue;
            }

            char c = text.charAt(i);
            // 정규식 메타문자 이스케이프
            if ("\\[]{}()^$.|*+?".indexOf(c) >= 0) {
                sb.append('\\');
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /**
     * 로어에서 스탯을 파싱합니다.
     * <p>
     * config.yml의 lore.format과 정확히 일치하는 라인만 파싱합니다.
     * </p>
     *
     * @param lore 로어 라인 목록
     * @return 파싱된 스탯
     */
    public ItemStats parseLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return ItemStats.empty();
        }

        ItemStats.Builder builder = ItemStats.builder();
        Map<StatType, Double> foundStats = new EnumMap<>(StatType.class);

        for (String line : lore) {
            String stripped = stripColor(line);

            for (Map.Entry<StatType, Pattern> entry : parsePatterns.entrySet()) {
                StatType type = entry.getKey();
                Pattern pattern = entry.getValue();

                Matcher matcher = pattern.matcher(stripped);
                if (matcher.find()) {
                    try {
                        double value = Double.parseDouble(matcher.group(1));
                        if (value >= 0) {
                            foundStats.put(type, value);
                        }
                    } catch (NumberFormatException ignored) {
                        // 숫자 파싱 실패 시 무시
                    }
                    break; // 이 라인에서 스탯을 찾았으면 다음 라인으로
                }
            }
        }

        // 찾은 스탯을 빌더에 적용
        foundStats.forEach((type, value) -> {
            switch (type) {
                case DAMAGE -> builder.damage(value);
                case DEFENSE -> builder.defense(value);
                case HEALTH -> builder.health(value);
                case LIFESTEAL -> builder.lifesteal(value);
                case CRIT_CHANCE -> builder.critChance(value);
                case CRIT_DAMAGE -> builder.critDamage(value);
                case DODGE -> builder.dodge(value);
            }
        });

        return builder.build();
    }

    /**
     * 스탯으로 로어를 생성합니다.
     *
     * @param stats 생성할 스탯
     * @return 생성된 로어 라인 목록
     */
    public List<String> generateLore(ItemStats stats) {
        if (stats == null || stats.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> lore = new ArrayList<>();

        // 구분선 (상단)
        if (template.isSeparatorEnabled()) {
            lore.add(template.getSeparatorTop());
        }

        // 템플릿 순서대로 스탯 추가
        for (StatType type : template.getOrder()) {
            double value = stats.getStat(type);
            if (value > 0) {
                String formatted = template.formatStat(type, value);
                if (formatted != null) {
                    lore.add(formatted);
                }
            }
        }

        // 구분선 (하단)
        if (template.isSeparatorEnabled() && lore.size() > 1) {
            lore.add(template.getSeparatorBottom());
        } else if (lore.size() == 1 && template.isSeparatorEnabled()) {
            // 상단 구분선만 있고 스탯이 없으면 제거
            lore.clear();
        }

        return lore;
    }

    /**
     * 기존 로어에 스탯을 추가하거나 업데이트합니다.
     *
     * @param existingLore 기존 로어
     * @param newStats     추가/수정할 스탯
     * @param insertIndex  삽입할 위치 (기존 스탯이 없을 경우)
     * @return 수정된 로어
     */
    public List<String> addOrUpdateStats(List<String> existingLore, ItemStats newStats, int insertIndex) {
        if (existingLore == null) {
            existingLore = new ArrayList<>();
        }
        if (newStats == null || newStats.isEmpty()) {
            return new ArrayList<>(existingLore);
        }

        // 기존 로어에서 스탯/구분선이 아닌 라인과 첫 스탯 위치 파악
        List<String> beforeStats = new ArrayList<>();
        List<String> afterStats = new ArrayList<>();
        int firstStatIndex = -1;
        boolean foundStats = false;

        for (int i = 0; i < existingLore.size(); i++) {
            String line = existingLore.get(i);
            boolean isStatOrSeparator = isStatLine(line) || isSeparatorLine(line);

            if (isStatOrSeparator) {
                if (firstStatIndex == -1) {
                    firstStatIndex = i;
                }
                foundStats = true;
                // 스탯/구분선은 건너뜀 (새로 생성할 것임)
            } else if (!foundStats) {
                // 스탯 이전의 라인
                beforeStats.add(line);
            } else {
                // 스탯 이후의 라인
                afterStats.add(line);
            }
        }

        // 새 스탯 로어 생성
        List<String> newStatLore = generateLore(newStats);

        // 결과 조합
        List<String> result = new ArrayList<>();

        if (firstStatIndex == -1) {
            // 기존 스탯이 없으면 insertIndex 위치에 삽입
            int actualIndex = Math.min(insertIndex, beforeStats.size());
            for (int i = 0; i < actualIndex; i++) {
                result.add(beforeStats.get(i));
            }
            result.addAll(newStatLore);
            for (int i = actualIndex; i < beforeStats.size(); i++) {
                result.add(beforeStats.get(i));
            }
        } else {
            // 기존 스탯 위치에 새 스탯 삽입
            result.addAll(beforeStats);
            result.addAll(newStatLore);
            result.addAll(afterStats);
        }

        return result;
    }

    /**
     * 특정 스탯을 제거합니다.
     *
     * @param existingLore 기존 로어
     * @param typeToRemove 제거할 스탯 타입
     * @return 수정된 로어
     */
    public List<String> removeStat(List<String> existingLore, StatType typeToRemove) {
        if (existingLore == null || existingLore.isEmpty()) {
            return new ArrayList<>();
        }

        // 현재 스탯 파싱
        ItemStats currentStats = parseLore(existingLore);
        // 해당 스탯 제거
        ItemStats newStats = currentStats.removeStat(typeToRemove);

        // 모든 스탯이 제거되면 스탯/구분선 없이 반환
        if (newStats.isEmpty()) {
            return removeAllStats(existingLore);
        }

        // 로어 재생성
        return addOrUpdateStats(existingLore, newStats, 0);
    }

    /**
     * 모든 스탯을 제거합니다.
     *
     * @param existingLore 기존 로어
     * @return 스탯이 제거된 로어
     */
    public List<String> removeAllStats(List<String> existingLore) {
        if (existingLore == null || existingLore.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (String line : existingLore) {
            if (!isStatLine(line) && !isSeparatorLine(line)) {
                result.add(line);
            }
        }
        return result;
    }

    /**
     * 해당 라인이 스탯 라인인지 확인합니다.
     *
     * @param line 확인할 라인
     * @return 스탯 라인이면 true
     */
    public boolean isStatLine(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }

        String stripped = stripColor(line);

        for (Pattern pattern : parsePatterns.values()) {
            if (pattern.matcher(stripped).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 해당 라인이 구분선인지 확인합니다.
     *
     * @param line 확인할 라인
     * @return 구분선이면 true
     */
    public boolean isSeparatorLine(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }

        String stripped = stripColor(line);

        // 템플릿의 구분선과 비교
        if (stripped.equals(separatorTopStripped) || stripped.equals(separatorBottomStripped)) {
            return true;
        }

        // 일반적인 구분선 패턴 (대시 연속)
        return SEPARATOR_LINE_PATTERN.matcher(stripped).matches() ||
                stripped.contains("────") ||
                stripped.contains("----");
    }

    /**
     * 문자열에서 색상 코드를 제거합니다.
     *
     * @param text 원본 텍스트
     * @return 색상 코드가 제거된 텍스트
     */
    public static String stripColor(String text) {
        if (text == null) {
            return null;
        }

        // 빠른 경로 (색상 코드가 없으면 그대로 반환)
        if (text.indexOf('§') < 0 && text.indexOf('&') < 0) {
            return text;
        }

        String result = text;

        // §x§r§r§g§g§b§b RGB 형식 제거 (먼저 처리)
        if (result.contains("§x")) {
            result = RGB_COLOR_PATTERN.matcher(result).replaceAll("");
        }
        if (result.indexOf('§') >= 0) {
            result = SECTION_COLOR_PATTERN.matcher(result).replaceAll("");
        }
        if (result.indexOf('&') >= 0) {
            result = AMPERSAND_COLOR_PATTERN.matcher(result).replaceAll("");
        }

        return result;
    }

    /**
     * 템플릿을 반환합니다.
     *
     * @return 로어 템플릿
     */
    public LoreTemplate getTemplate() {
        return template;
    }
}
