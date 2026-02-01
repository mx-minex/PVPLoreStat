package kr.minex.pvplorestat.domain.model;

import java.util.*;

/**
 * 플레이어 스탯 Value Object (불변)
 * <p>
 * 플레이어의 모든 장비 스탯을 합산하여 관리합니다.
 * 불변 객체로 설계되어 스레드 안전합니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public final class PlayerStats {

    private final UUID playerId;
    private final Map<EquipmentSlot, ItemStats> equipmentStats;
    private final ItemStats totalStats;

    private PlayerStats(UUID playerId, Map<EquipmentSlot, ItemStats> equipmentStats) {
        this.playerId = Objects.requireNonNull(playerId, "playerId는 null일 수 없습니다");
        // EnumMap 복사 생성자는 빈 맵을 허용하지 않으므로 새로 생성 후 putAll 사용
        EnumMap<EquipmentSlot, ItemStats> copy = new EnumMap<>(EquipmentSlot.class);
        copy.putAll(equipmentStats);
        this.equipmentStats = Collections.unmodifiableMap(copy);
        this.totalStats = calculateTotalStats(equipmentStats);
    }

    /**
     * 장비 스탯들을 합산하여 총 스탯을 계산합니다.
     */
    private static ItemStats calculateTotalStats(Map<EquipmentSlot, ItemStats> equipmentStats) {
        ItemStats result = ItemStats.empty();
        for (ItemStats stats : equipmentStats.values()) {
            result = result.merge(stats);
        }
        return result;
    }

    /**
     * 빈 플레이어 스탯을 생성합니다.
     *
     * @param playerId 플레이어 UUID
     * @return 빈 스탯
     */
    public static PlayerStats empty(UUID playerId) {
        return new PlayerStats(playerId, new EnumMap<>(EquipmentSlot.class));
    }

    /**
     * 단일 ItemStats로 플레이어 스탯을 생성합니다.
     * (테스트용 또는 간단한 생성용)
     *
     * @param playerId 플레이어 UUID
     * @param stats    적용할 스탯
     * @return 생성된 플레이어 스탯
     */
    public static PlayerStats of(UUID playerId, ItemStats stats) {
        Map<EquipmentSlot, ItemStats> equipmentStats = new EnumMap<>(EquipmentSlot.class);
        equipmentStats.put(EquipmentSlot.HELMET, stats); // 임시 슬롯에 할당
        return new PlayerStats(playerId, equipmentStats);
    }

    /**
     * 여러 ItemStats를 합산하여 플레이어 스탯을 생성합니다.
     *
     * @param playerId  플레이어 UUID
     * @param statsList 스탯 목록
     * @return 합산된 플레이어 스탯
     */
    public static PlayerStats of(UUID playerId, List<ItemStats> statsList) {
        Map<EquipmentSlot, ItemStats> equipmentStats = new EnumMap<>(EquipmentSlot.class);
        EquipmentSlot[] slots = EquipmentSlot.values();

        for (int i = 0; i < statsList.size() && i < slots.length; i++) {
            equipmentStats.put(slots[i], statsList.get(i));
        }

        return new PlayerStats(playerId, equipmentStats);
    }

    // ===== Getters =====

    /**
     * 플레이어 UUID를 반환합니다.
     *
     * @return 플레이어 UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * 모든 장비 스탯이 합산된 총 스탯을 반환합니다.
     *
     * @return 총 스탯
     */
    public ItemStats getTotalStats() {
        return totalStats;
    }

    /**
     * 특정 장비 슬롯의 스탯을 반환합니다.
     *
     * @param slot 장비 슬롯
     * @return 해당 슬롯의 스탯, 없으면 빈 스탯
     */
    public ItemStats getEquipmentStats(EquipmentSlot slot) {
        return equipmentStats.getOrDefault(slot, ItemStats.empty());
    }

    /**
     * 모든 장비 스탯을 Map으로 반환합니다.
     *
     * @return 장비 슬롯별 스탯 맵 (불변)
     */
    public Map<EquipmentSlot, ItemStats> getAllEquipmentStats() {
        return equipmentStats;
    }

    // ===== 수정 메서드 (불변성 유지) =====

    /**
     * 특정 장비 슬롯의 스탯을 설정한 새 인스턴스를 반환합니다.
     *
     * @param slot  장비 슬롯
     * @param stats 설정할 스탯
     * @return 수정된 새 인스턴스
     */
    public PlayerStats withEquipmentStats(EquipmentSlot slot, ItemStats stats) {
        // EnumMap 복사 생성자는 빈 맵을 허용하지 않으므로 새로 생성 후 putAll 사용
        Map<EquipmentSlot, ItemStats> newEquipmentStats = new EnumMap<>(EquipmentSlot.class);
        newEquipmentStats.putAll(this.equipmentStats);
        if (stats == null || stats.isEmpty()) {
            newEquipmentStats.remove(slot);
        } else {
            newEquipmentStats.put(slot, stats);
        }
        return new PlayerStats(playerId, newEquipmentStats);
    }

    /**
     * 특정 장비 슬롯의 스탯을 제거한 새 인스턴스를 반환합니다.
     *
     * @param slot 제거할 장비 슬롯
     * @return 수정된 새 인스턴스
     */
    public PlayerStats removeEquipmentStats(EquipmentSlot slot) {
        // EnumMap 복사 생성자는 빈 맵을 허용하지 않으므로 새로 생성 후 putAll 사용
        Map<EquipmentSlot, ItemStats> newEquipmentStats = new EnumMap<>(EquipmentSlot.class);
        newEquipmentStats.putAll(this.equipmentStats);
        newEquipmentStats.remove(slot);
        return new PlayerStats(playerId, newEquipmentStats);
    }

    /**
     * 모든 장비 스탯을 제거한 새 인스턴스를 반환합니다.
     *
     * @return 빈 스탯을 가진 새 인스턴스
     */
    public PlayerStats clearAllStats() {
        return empty(playerId);
    }

    // ===== 체력 계산 =====

    /**
     * 최대 체력을 계산합니다.
     * 체력 스탯은 그대로 최대 체력에 추가됩니다.
     *
     * @param baseHealth 기본 최대 체력 (일반적으로 20)
     * @return 계산된 최대 체력
     */
    public double calculateMaxHealth(double baseHealth) {
        return baseHealth + totalStats.getHealth();
    }

    // ===== equals, hashCode, toString =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerStats that = (PlayerStats) o;
        return Objects.equals(playerId, that.playerId) &&
                Objects.equals(equipmentStats, that.equipmentStats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, equipmentStats);
    }

    @Override
    public String toString() {
        return "PlayerStats{" +
                "playerId=" + playerId +
                ", totalStats=" + totalStats +
                ", equipmentSlots=" + equipmentStats.keySet() +
                '}';
    }
}
