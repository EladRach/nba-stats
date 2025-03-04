package com.nba.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GameStatsUpdateMessage {
    private int playerId;
    private int teamId;
}
