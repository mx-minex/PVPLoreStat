package kr.minex.pvplorestat.integration;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import kr.minex.pvplorestat.PVPLoreStat;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PVPLoreStat 리로드 통합 테스트")
class PluginReloadIntegrationTest {

    private ServerMock server;
    private PVPLoreStat plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(PVPLoreStat.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("리로드를 반복해도 리스너가 중복 등록되지 않는다")
    void reloadDoesNotDuplicateListeners() {
        int baseline = HandlerList.getRegisteredListeners(plugin).size();

        plugin.reload();
        assertEquals(baseline, HandlerList.getRegisteredListeners(plugin).size());

        plugin.reload();
        assertEquals(baseline, HandlerList.getRegisteredListeners(plugin).size());
    }
}

