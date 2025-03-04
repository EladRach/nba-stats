package com.nba.stats.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PLAYER_CACHE_PREFIX = "player:stats:";
    private static final String TEAM_CACHE_PREFIX = "team:stats:";
    private static final String PLAYER_LOCK_PREFIX = "player:lock:";
    private static final String TEAM_LOCK_PREFIX = "team:lock:";

    public void acquireLocks(int playerId, int teamId) {
        String playerLockKey = PLAYER_LOCK_PREFIX + playerId;
        String teamLockKey = TEAM_LOCK_PREFIX + teamId;
        String lockValue = UUID.randomUUID().toString();
        while(!acquireLock(playerLockKey, lockValue) || !acquireLock(teamLockKey, lockValue)) {
            log.info("Waiting for lock release on player {} or team {}", playerId, teamId);
            waitForLock();
        }
        log.info("Locks acquired for player {} and team {}", playerId, teamId);
    }

    public void releaseLocks(int playerId, int teamId) {
        String playerLockKey = PLAYER_LOCK_PREFIX + playerId;
        String teamLockKey = TEAM_LOCK_PREFIX + teamId;
        releaseLock(playerLockKey);
        releaseLock(teamLockKey);
    }

    public void atomicInvalidateAndUpdateCache(int playerId, int teamId, Map<String, Object> playerStats, Map<String, Object> teamStats) {
        try {
            stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                public List<Object> execute(RedisOperations operations) {
                    operations.multi();
                    invalidateCache(playerId,teamId);
                    updatePlayerCache(playerId,playerStats);
                    updateTeamCache(teamId,teamStats);
                    return operations.exec();
                }
            });
        } catch (Exception e) {
            log.error("Failed to update Redis cache for player {} and team {}: {}", playerId, teamId, e.getMessage());
            throw new RuntimeException("Redis cache update failed", e);
        }
    }


    private boolean acquireLock(String lockKey, String lockValue) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue);
        return Boolean.TRUE.equals(success);
    }

    private void releaseLock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
        log.info("Releasing lock {}", lockKey);
    }

    private void waitForLock() {
        try {
            Thread.sleep(100);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for lock", e);
        }
    }

    public void invalidateCache(int playerId, int teamId) {
        try {
            stringRedisTemplate.delete(PLAYER_CACHE_PREFIX + playerId);
            log.info("Cache invalidated for player {}", playerId);

            stringRedisTemplate.delete(TEAM_CACHE_PREFIX + teamId);
            log.info("Cache invalidated for team {}", teamId);
        } catch (Exception e) {
            // might cause a user to get outdated information.
            log.error("Cache invalidation failed for player {} and team {}: {}", playerId, teamId, e.getMessage());
        }
    }

    public void updatePlayerCache(int playerId, Map<String, Object> stats) {
        updateCache(PLAYER_CACHE_PREFIX+playerId, stats);
    }

    public void updateTeamCache(int teamId, Map<String, Object> stats) {
        updateCache(TEAM_CACHE_PREFIX+teamId, stats);
    }

    public Map<String, Object> getCachedPlayerStats(int playerId) {
        return getCachedStats(PLAYER_CACHE_PREFIX + playerId);
    }

    public Map<String, Object> getCachedTeamStats(int teamId) {
        return getCachedStats(TEAM_CACHE_PREFIX + teamId);
    }

    private void updateCache(String cacheKey, Map<String, Object> stats) {
        try{
            String jsonStats = objectMapper.writeValueAsString(stats);
            stringRedisTemplate.opsForValue().set(cacheKey, jsonStats);
            log.info("Updated Redis cache for {}", cacheKey);
        } catch (Exception e) {
            log.error("Failed to update Redis cache for {}: {}", cacheKey, e.getMessage());
        }
    }

    private Map<String, Object> getCachedStats(String cacheKey) {
        try {
            String cachedStats = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedStats != null) {
                log.info("Cache hit for {}", cacheKey);
                return objectMapper.readValue(cachedStats, Map.class);
            }
        } catch (Exception e) {
            log.error("Failed to get cache for {}: {}", cacheKey, e.getMessage());
        }
        log.info("Cache miss for {}", cacheKey);
        return null;
    }
}
