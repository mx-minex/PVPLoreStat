package kr.minex.pvplorestat.infrastructure.cache;

import kr.minex.pvplorestat.domain.model.PlayerStats;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 플레이어 스탯 캐시
 * <p>
 * 플레이어의 계산된 스탯을 메모리에 캐싱하여 빠른 접근을 제공합니다.
 * 스레드 안전하게 설계되었습니다.
 * </p>
 *
 * @author Minex
 * @since 1.0.0
 */
public class PlayerStatsCache {

    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();

    /**
     * 플레이어 스탯을 캐시에 저장합니다.
     *
     * @param stats 저장할 스탯
     */
    public void put(PlayerStats stats) {
        if (stats == null) {
            return;
        }
        cache.put(stats.getPlayerId(), stats);
    }

    /**
     * 플레이어 스탯을 조회합니다.
     *
     * @param playerId 플레이어 UUID
     * @return 스탯, 없으면 빈 Optional
     */
    public Optional<PlayerStats> get(UUID playerId) {
        return Optional.ofNullable(cache.get(playerId));
    }

    /**
     * 플레이어 스탯을 조회하거나 빈 스탯을 반환합니다.
     *
     * @param playerId 플레이어 UUID
     * @return 스탯 (없으면 빈 스탯)
     */
    public PlayerStats getOrEmpty(UUID playerId) {
        return cache.getOrDefault(playerId, PlayerStats.empty(playerId));
    }

    /**
     * 플레이어 스탯을 캐시에서 제거합니다.
     *
     * @param playerId 플레이어 UUID
     * @return 제거된 스탯, 없었으면 빈 Optional
     */
    public Optional<PlayerStats> remove(UUID playerId) {
        return Optional.ofNullable(cache.remove(playerId));
    }

    /**
     * 플레이어가 캐시에 있는지 확인합니다.
     *
     * @param playerId 플레이어 UUID
     * @return 캐시에 있으면 true
     */
    public boolean contains(UUID playerId) {
        return cache.containsKey(playerId);
    }

    /**
     * 캐시를 비웁니다.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * 캐시된 플레이어 수를 반환합니다.
     *
     * @return 캐시된 플레이어 수
     */
    public int size() {
        return cache.size();
    }

    /**
     * 모든 캐시된 스탯을 반환합니다.
     *
     * @return UUID와 스탯의 맵 (읽기 전용)
     */
    public Map<UUID, PlayerStats> getAll() {
        return Map.copyOf(cache);
    }
}
