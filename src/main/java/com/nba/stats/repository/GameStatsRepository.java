package com.nba.stats.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GameStatsRepository {
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void insertGameStats(int playerId, int teamId, int gameId, int points, int rebounds, int assists, int steals,
                                int blocks, int fouls, int turnovers, double minutesPlayed) {
        String sql = "INSERT INTO game_stats (player_id, team_id, game_id, points, rebounds, assists, steals, blocks, " +
                "fouls, turnovers, minutes_played) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            int rowsAffected = jdbcTemplate.update(sql, playerId, teamId, gameId, points, rebounds, assists, steals,
                    blocks, fouls, turnovers, minutesPlayed);
            if (rowsAffected == 0) {
                log.warn("No rows were inserted for player {} in game {}", playerId, gameId);
                throw new RuntimeException("Database insert failed");
            }
            log.info("Successfully inserted game stats for {} in game {}", playerId, gameId);
        } catch (DataAccessException e) {
            log.error("Database insert failed for player {} in game {}: {}", playerId, gameId, e.getMessage());
            throw new RuntimeException("Database insert failed: " + e.getMessage(), e);
        }

    }

    public Map<String, Object> getPlayerStats(int playerId) {
        return getStatsByColumn("player_id", playerId);
    }

    public Map<String, Object> getTeamStats(int teamId) {
        return getStatsByColumn("team_id", teamId);
    }

    private Map<String, Object> getStatsByColumn(String column, int value) {
        String sql = """
                SELECT
                    %s,
                    AVG(points) AS avg_points,
                    AVG(rebounds) AS avg_rebounds,
                    AVG(assists) AS avg_assists,
                    AVG(steals) AS avg_steals,
                    AVG(blocks) AS avg_blocks,
                    AVG(fouls) AS avg_fouls,
                    AVG(turnovers) AS avg_turnovers,
                    AVG(minutes_played) AS avg_minutes_played
                FROM game_stats
                WHERE %s = ?
                GROUP BY %s
                """.formatted(column, column, column);
        try {
            return jdbcTemplate.queryForMap(sql, value);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
