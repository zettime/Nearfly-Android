package de.pbma.nearflyexample.scenarios.ScoreBoardNotepad;

import androidx.annotation.Nullable;

public class Player {
    /*private LinkedList<String> playerScore = new LinkedList<>();
    private LinkedList<String> playerId = new LinkedList<>();
    private LinkedList<String> playerNumber = new LinkedList<>();
    private LinkedList<String> playerEntersTime = new LinkedList<>();*/
    public String username;
    public long entersTime;
    public int score=0;
    public int number;
    public int totalTurns = 0;
    public int keepAliveCounter = 4;

    public Player(String username, long entersTime) {
        this.username = username;
        this.entersTime = entersTime;
    }

    public String toString(){
        return username + "--score: " + score + "--totalTurns: " + totalTurns
                + "--entersTime: " + entersTime + "\n";
    }

    public void resetPlayerStates(){
        score=0;
        totalTurns=0;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof Player))
            return false;

        Player other= (Player) obj;

        return username.equals(other.username);
         /*&& score == other.score
                && playerEntersTime == other.playerEntersTime)*/
    }
}
