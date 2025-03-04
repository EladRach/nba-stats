package com.nba.stats.dto;

import lombok.Data;

@Data
public class GameStatsRequest {
    private int playerId;
    private int teamId;
    private int gameId;
    private int points;
    private int rebounds;
    private int assists;
    private int steals;
    private int blocks;
    private int fouls;
    private int turnovers;
    private double minutesPlayed;
}
