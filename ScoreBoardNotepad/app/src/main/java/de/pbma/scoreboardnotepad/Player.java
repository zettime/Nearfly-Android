package de.pbma.scoreboardnotepad;

import java.util.LinkedList;

public class Player {
    /*private LinkedList<String> playerScore = new LinkedList<>();
    private LinkedList<String> playerId = new LinkedList<>();
    private LinkedList<String> playerNumber = new LinkedList<>();
    private LinkedList<String> playerEntersTime = new LinkedList<>();*/
    public String username;
    public long playerEntersTime;
    public int score;

    public Player(String username, long playerEntersTime) {
        this.username = username;
        this.playerEntersTime = playerEntersTime;
    }
}
