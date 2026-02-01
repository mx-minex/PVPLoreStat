package kr.minex.pvplorestat.domain.model;

/**
 * 장비 슬롯 열거형
 * <p>
 * 스탯이 적용되는 장비 슬롯을 정의합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public enum EquipmentSlot {

    /**
     * 투구
     */
    HELMET("helmet", "투구"),

    /**
     * 갑옷
     */
    CHESTPLATE("chestplate", "갑옷"),

    /**
     * 레깅스
     */
    LEGGINGS("leggings", "레깅스"),

    /**
     * 부츠
     */
    BOOTS("boots", "부츠"),

    /**
     * 주무기 (메인핸드)
     */
    MAIN_HAND("main_hand", "주무기"),

    /**
     * 보조무기 (오프핸드)
     */
    OFF_HAND("off_hand", "보조무기");

    private final String configKey;
    private final String displayName;

    EquipmentSlot(String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }

    /**
     * 설정 파일에서 사용하는 키를 반환합니다.
     *
     * @return 설정 키
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 한글 표시명을 반환합니다.
     *
     * @return 한글 표시명
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 방어구 슬롯인지 확인합니다.
     *
     * @return 방어구 슬롯이면 true
     */
    public boolean isArmor() {
        return this == HELMET || this == CHESTPLATE || this == LEGGINGS || this == BOOTS;
    }

    /**
     * 무기 슬롯인지 확인합니다.
     *
     * @return 무기 슬롯이면 true
     */
    public boolean isWeapon() {
        return this == MAIN_HAND || this == OFF_HAND;
    }
}
