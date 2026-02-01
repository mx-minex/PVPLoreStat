package kr.minex.pvplorestat.domain.model;

import java.util.*;

/**
 * 스탯 타입 열거형
 * <p>
 * 플러그인에서 지원하는 모든 스탯 타입을 정의합니다.
 * 각 스탯은 고유한 설정 키, 한글/영문 표시명, 별칭을 가집니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public enum StatType {

    /**
     * 공격력 - PVP 시 추가 데미지
     */
    DAMAGE(
            "damage",
            "공격력",
            "Damage",
            false,
            List.of("공격력", "atk", "attack", "damage")
    ),

    /**
     * 방어력 - PVP 시 데미지 감소
     */
    DEFENSE(
            "defense",
            "방어력",
            "Defense",
            false,
            List.of("방어력", "def", "defense")
    ),

    /**
     * 체력 - 최대 체력 증가
     */
    HEALTH(
            "health",
            "체력",
            "Health",
            false,
            List.of("체력", "hp", "health", "추가체력")
    ),

    /**
     * 피흡수 - 데미지의 일정 비율만큼 체력 회복
     */
    LIFESTEAL(
            "lifesteal",
            "피흡수",
            "Lifesteal",
            true,
            List.of("피흡수", "흡혈", "lifesteal")
    ),

    /**
     * 치명타 확률 - 크리티컬 발동 확률
     */
    CRIT_CHANCE(
            "critchance",
            "치명타 확률",
            "Crit Chance",
            true,
            List.of("치명타 확률", "치명타확률", "치확", "crit", "critchance", "crit_chance")
    ),

    /**
     * 치명타 데미지 - 크리티컬 발동 시 추가 데미지
     */
    CRIT_DAMAGE(
            "critdamage",
            "치명타 데미지",
            "Crit Damage",
            false,
            List.of("치명타 데미지", "치명타데미지", "치뎀", "critdamage", "crit_damage")
    ),

    /**
     * 회피율 - 공격 회피 확률
     */
    DODGE(
            "dodge",
            "회피율",
            "Dodge",
            true,
            List.of("회피율", "회피", "dodge")
    );

    private final String configKey;
    private final String displayName;
    private final String displayNameEn;
    private final boolean percent;
    private final List<String> keywords;

    /**
     * 키워드로 빠른 검색을 위한 맵 (소문자 정규화)
     */
    private static final Map<String, StatType> KEYWORD_MAP = new HashMap<>();

    static {
        for (StatType type : values()) {
            for (String keyword : type.keywords) {
                KEYWORD_MAP.put(keyword.toLowerCase(), type);
            }
        }
    }

    StatType(String configKey, String displayName, String displayNameEn,
             boolean percent, List<String> keywords) {
        this.configKey = configKey;
        this.displayName = displayName;
        this.displayNameEn = displayNameEn;
        this.percent = percent;
        this.keywords = keywords;
    }

    /**
     * 설정 파일에서 사용하는 키를 반환합니다.
     *
     * @return 설정 키 (예: "damage", "critchance")
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 한글 표시명을 반환합니다.
     *
     * @return 한글 표시명 (예: "공격력", "치명타 확률")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 영문 표시명을 반환합니다.
     *
     * @return 영문 표시명 (예: "Damage", "Crit Chance")
     */
    public String getDisplayNameEn() {
        return displayNameEn;
    }

    /**
     * 퍼센트로 표시되는 스탯인지 확인합니다.
     *
     * @return 퍼센트 스탯이면 true
     */
    public boolean isPercent() {
        return percent;
    }

    /**
     * 이 스탯 타입과 연관된 모든 키워드를 반환합니다.
     *
     * @return 키워드 목록 (불변)
     */
    public List<String> getKeywords() {
        return Collections.unmodifiableList(keywords);
    }

    /**
     * 키워드로 스탯 타입을 검색합니다.
     * <p>
     * 한글/영문, 별칭, 축약어 모두 지원하며 대소문자를 구분하지 않습니다.
     * </p>
     *
     * @param keyword 검색할 키워드
     * @return 찾은 스탯 타입, 없으면 빈 Optional
     */
    public static Optional<StatType> findByKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(KEYWORD_MAP.get(keyword.toLowerCase()));
    }

    /**
     * 모든 스탯 타입을 반환합니다.
     *
     * @return 스탯 타입 목록 (불변)
     */
    public static List<StatType> getAllTypes() {
        return List.of(values());
    }
}
