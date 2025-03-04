package com.nba.stats.service;

import com.nba.stats.repository.GameStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameStatsService {
    private final GameStatsRepository gameStatsRepository;
    private final CacheService cacheService;

    @Transactional
    public void logGameStats(int playerId, int teamId, int gameId, int points, int rebounds, int assists, int steals,
                             int blocks, int fouls, int turnovers, double minutesPlayed) {
        validateStats(playerId, teamId, gameId, points, rebounds, assists, steals, blocks, fouls, turnovers, minutesPlayed);

        cacheService.acquireLocks(playerId, teamId);
        try {
            saveGameStatsToDatabase(playerId, teamId, gameId, points, rebounds, assists, steals, blocks, fouls, turnovers, minutesPlayed);
            Map<String, Object> playerStats = gameStatsRepository.getPlayerStats(playerId);
            Map<String, Object> teamStats = gameStatsRepository.getTeamStats(teamId);
            cacheService.atomicInvalidateAndUpdateCache(playerId, teamId, playerStats, teamStats);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process game stats update", e);
        } finally {
            cacheService.releaseLocks(playerId, teamId);
        }
    }

    public Map<String, Object> getPlayerStats(int playerId) {
        return cacheService.getCachedPlayerStats(playerId);
    }

    public Map<String, Object> getTeamStats(int teamId) {
        return cacheService.getCachedTeamStats(teamId);
    }

    private void validateStats(int playerId, int teamId, int gameId, int points, int rebounds, int assists, int steals,
                               int blocks, int fouls, int turnovers, double minutesPlayed) {
        if (playerId < 1 || teamId < 1 || gameId < 1) {
            log.error("Validation failed: Ids must be bigger than 1.");
            throw new IllegalArgumentException("Ids must be bigger than 1.");
        }
        if (points < 0 || rebounds < 0 || assists < 0 || steals < 0 || blocks < 0 || fouls < 0 || turnovers < 0) {
            log.error("Validation failed: Stats cannot be negative.");
            throw new IllegalArgumentException("Stats cannot be negative.");
        }
        if (fouls > 6) {
            log.error("Validation failed: Fouls ({}) cannot be greater than 6.", fouls);
            throw new IllegalArgumentException("Fouls cannot be greater than 6.");
        }
        if (minutesPlayed < 0 || minutesPlayed > 48.0) {
            log.error("Validation failed: Minutes played ({}) must be between 0 and 48.", minutesPlayed);
            throw new IllegalArgumentException("Minutes played must be between 0 and 48.");
        }
    }

    private void saveGameStatsToDatabase(int playerId, int teamId, int gameId, int points, int rebounds, int assists, int steals, int blocks, int fouls, int turnovers, double minutesPlayed) {
        try {
            gameStatsRepository.insertGameStats(playerId, teamId, gameId, points, rebounds, assists, steals, blocks, fouls, turnovers, minutesPlayed);
            log.info("Inserted game stats into PostgresSQL for player {}", playerId);
        } catch (Exception e) {
            log.error("Failed to write game stats to database for player {}: {}", playerId, e.getMessage());
            throw new RuntimeException("Database write failed", e);
        }
    }
}
