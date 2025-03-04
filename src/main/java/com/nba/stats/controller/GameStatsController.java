package com.nba.stats.controller;

import com.nba.stats.dto.ErrorResponse;
import com.nba.stats.dto.GameStatsRequest;
import com.nba.stats.dto.SuccessResponse;
import com.nba.stats.service.GameStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class GameStatsController {
    private final GameStatsService gameStatsService;

    @PostMapping
    public ResponseEntity<?> logStats (@RequestBody GameStatsRequest request) {
        log.info("Received request to log stats for player {} in game {}", request.getPlayerId(), request.getGameId());
        try {
            gameStatsService.logGameStats(
                    request.getPlayerId(),
                    request.getTeamId(),
                    request.getGameId(),
                    request.getPoints(),
                    request.getRebounds(),
                    request.getAssists(),
                    request.getSteals(),
                    request.getBlocks(),
                    request.getFouls(),
                    request.getTurnovers(),
                    request.getMinutesPlayed()
            );
            return ResponseEntity.ok(new SuccessResponse("Game stats logged successfully!"));
        } catch (IllegalArgumentException e) {
            log.error("Failed to log stats for player {} in game {}: {}", request.getPlayerId(), request.getGameId(), e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
        catch (RuntimeException e) {
            log.error("Processing error for player {}: {}", request.getPlayerId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<?> getPlayerStats(@PathVariable int playerId) {
        log.info("Fetching stats for player {}", playerId);
        Map<String, Object> playerStats = gameStatsService.getPlayerStats(playerId);
        if (playerStats == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Player stats not found"));
        }
        return ResponseEntity.ok(playerStats);
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<?> getTeamStats(@PathVariable int teamId) {
        log.info("Fetching stats for team {}", teamId);
        Map<String, Object> teamStats = gameStatsService.getTeamStats(teamId);
        if (teamStats == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Team stats not found"));
        }
        return ResponseEntity.ok(teamStats);
    }
}
