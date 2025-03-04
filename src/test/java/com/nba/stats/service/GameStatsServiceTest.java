package com.nba.stats.service;

import com.nba.stats.repository.GameStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GameStatsServiceTest {

    @Mock
    private GameStatsRepository gameStatsRepository;
    @Mock
    private CacheService cacheService;
    @InjectMocks
    GameStatsService gameStatsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void logGameStats_ValidInput_SuccessfulExecution() {
        int playerId = 1, teamId = 2, gameId = 3;
        int points = 20, rebounds = 5, assists = 3, steals = 1, blocks = 1, fouls = 2, turnovers = 1;
        double minutesPlayed = 35.5;
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("player_id", 1);
        doNothing().when(cacheService).acquireLocks(anyInt(), anyInt());
        doNothing().when(gameStatsRepository).insertGameStats(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyDouble());
        when(gameStatsRepository.getPlayerStats(anyInt())).thenReturn(mockStats);
        when(gameStatsRepository.getTeamStats(anyInt())).thenReturn(mockStats);
        doNothing().when(cacheService).atomicInvalidateAndUpdateCache(anyInt(), anyInt(), any(), any());
        doNothing().when(cacheService).releaseLocks(anyInt(), anyInt());

        assertDoesNotThrow(() -> gameStatsService.logGameStats(playerId, teamId, gameId, points, rebounds, assists,
                steals, blocks, fouls, turnovers, minutesPlayed));
    }
}
