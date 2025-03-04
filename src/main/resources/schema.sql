CREATE TABLE IF NOT EXISTS game_stats (
    id SERIAL PRIMARY KEY,
    player_id INT NOT NULL,
    team_id INT NOT NULL,
    game_id INT NOT NULL,
    points INT NOT NULL,
    rebounds INT NOT NULL,
    assists INT NOT NULL,
    steals INT NOT NULL,
    blocks INT NOT NULL,
    fouls INT CHECK (fouls <= 6),
    turnovers INT NOT NULL,
    minutes_played FLOAT CHECK (minutes_played BETWEEN 0 AND 48.0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
