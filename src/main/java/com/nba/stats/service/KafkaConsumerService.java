package com.nba.stats.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.stats.dto.GameStatsUpdateMessage;
import com.nba.stats.repository.GameStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final GameStatsRepository gameStatsRepository;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "nba-stats-topic", groupId = "nba-stats-group")
    public void consume(String message) {
        try {
            GameStatsUpdateMessage stats = objectMapper.readValue(message, GameStatsUpdateMessage.class);
            log.info("Received Kafka message: Updating averages in cache for player {} and team {}", stats.getPlayerId(), stats.getTeamId());

            Map<String, Object> playerStats = gameStatsRepository.getPlayerStats(stats.getPlayerId());
            Map<String, Object> teamStats = gameStatsRepository.getTeamStats(stats.getTeamId());

            cacheService.updatePlayerCache(stats.getPlayerId(), playerStats);
            cacheService.updateTeamCache(stats.getTeamId(), teamStats);

            log.info("Updated averages in cache for player {} and team {}", stats.getPlayerId(), stats.getTeamId());
        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", e.getMessage());
        }
    }
}
