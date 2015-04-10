package com.example.jeffrey.fivekings;

import android.app.Activity;
import android.util.Log;
import android.widget.RelativeLayout;

import java.util.ArrayList;

/**
 * Created by Jeffrey on 3/22/2015.
 * 3/22/2015    Encapsulates list of players for Game
 * 3/24/2015    Moved next Dealer to initRound() (so we can handle deleting the dealer)
 TODO:A Maybe manage PlayerLayout as well?
 */
class PlayerList extends ArrayList<Player> {
    static enum PlayerType {HUMAN, BASIC_COMPUTER, EXPERT_COMPUTER}

    private Player dealer;
    private Player currentPlayer;
    private Player playerWentOut;

    PlayerList() {
        dealer = null;
        currentPlayer = null;
        playerWentOut = null;
    }

    final void addStandardPlayers() {
        this.addPlayer("You", PlayerList.PlayerType.HUMAN);
        this.addPlayer("Computer", PlayerList.PlayerType.EXPERT_COMPUTER);
    }

    //Called on first game and then if you want another game
    final void initGame() {
        this.dealer = this.get(0);
        for (Player player : this) player.initGame();
    }

    final void initRound() {
        currentPlayer=this.dealer;
        playerWentOut=null;
        this.dealer = getNextPlayer(currentPlayer);   //set this here so that if we delete the dealer we can advance the dealer
    }

    void addPlayer (final String name, final PlayerType playerType) {
        switch (playerType) {
            case HUMAN:
                this.add(new HumanPlayer(name));
                break;
            case BASIC_COMPUTER:
                this.add(new ComputerPlayer(name));
                break;
            case EXPERT_COMPUTER:
            default:
                this.add(new StrategyComputerPlayer(name));
                break;
        }
    }

    Player getPlayer(final int iPlayer) {
        return this.get(iPlayer);
    }

    void deletePlayer(final int iDeletedPlayer, Activity activity) {
        //need to advance to next player before we remove it
        if (this.get(iDeletedPlayer) == currentPlayer) currentPlayer = getNextPlayer(this.currentPlayer);
        if (this.get(iDeletedPlayer) == playerWentOut) playerWentOut = null;
        if (this.get(iDeletedPlayer) == dealer) dealer = getNextPlayer(dealer);

        //remove the PlayerLayout
        final RelativeLayout fullScreenContent = (RelativeLayout) activity.findViewById(R.id.fullscreen_content);
        final PlayerMiniHandLayout deletedPlayerMiniHandLayout = this.getPlayer(iDeletedPlayer).getMiniHandLayout();
        //this one won't be deleted in the loop because we deleted it from the player list
        if (null != deletedPlayerMiniHandLayout) fullScreenContent.removeView(deletedPlayerMiniHandLayout);
        this.remove(iDeletedPlayer);

        relayoutPlayerMiniHands(activity);
    }

    void updatePlayer(final String name, final boolean isHuman, final int iPlayer) {
        final Player oldPlayer=this.get(iPlayer);
        final Player newPlayer;
        if (isHuman != this.get(iPlayer).isHuman()) {
            //if we're changing Human <-> Computer, then we need to create a new one as a copy
            newPlayer = isHuman ? new HumanPlayer(oldPlayer) : new ComputerPlayer(oldPlayer);
            this.set(iPlayer, newPlayer);
            if (oldPlayer == currentPlayer) currentPlayer = newPlayer;
            if (oldPlayer == playerWentOut) playerWentOut = newPlayer;
            if (oldPlayer == dealer) dealer = newPlayer;
        }
        else newPlayer=oldPlayer;
        //and now update the name (which is the only other thing that can be changed in the dialog)
        newPlayer.updateName(name);
    }

    Player rotatePlayer() {
        this.currentPlayer.setTurnState(Player.TurnState.NOT_MY_TURN);
        this.currentPlayer = getNextPlayer(this.currentPlayer);
        this.currentPlayer.setTurnState(Player.TurnState.PREPARE_TURN);
        return this.currentPlayer;
    }

    boolean hideHandFromPrevious(Player thisPlayer) {
        //true if previous player is Human and so is this
        boolean hideCurrentHandFromPrevious=false;
        for (Player player : this) {
            if (getNextPlayer(player) == thisPlayer) {
                hideCurrentHandFromPrevious = player.isHuman() && thisPlayer.isHuman();
            }
        }
        return hideCurrentHandFromPrevious;
    }

    Player getNextPlayer() {
        return getNextPlayer(this.currentPlayer);
    }

    Player getNextPlayer(final Player player){
        final Player nextPlayer;
        if (this.isEmpty()) nextPlayer=null;
        else {
            final int playerIndex = this.indexOf(player);
            if (playerIndex == -1) nextPlayer = null;
                //if this is the "last" player, return the "first" one
            else if (playerIndex == this.size() - 1) nextPlayer = this.get(0);
            else nextPlayer =  this.get(playerIndex + 1);
        }
        return nextPlayer;
    }

    Player getWinner() {
        Player winningPlayer = this.get(0);

        for (Player player : this) {
            if (player.getCumulativeScore() <= winningPlayer.getCumulativeScore()) {
                winningPlayer = player;
            }
        }
        return winningPlayer;
    }

    boolean nextPlayerWentOut() {
        return (getNextPlayer(currentPlayer) == playerWentOut);
    }

    Player endCurrentPlayerTurn(final Deck deck) {
        this.playerWentOut = this.getCurrentPlayer().endTurn(this.getPlayerWentOut(), deck);
        return this.playerWentOut;
    }


    void logRoundScores() {
        if (playerWentOut == null) throw new RuntimeException("Error - playerWentOut is null");
        Log.i(Game.APP_TAG, "Player "+playerWentOut.getName()+" went out");
        Log.i(Game.APP_TAG, "        Current scores:");
        for (Player player : this){
            //add to cumulative score is done in animateRoundScore()
            Log.i(Game.APP_TAG, "Player " + player.getName() + ": " + player.getMeldedString(true) + player.getPartialAndSingles(true) + ". Cumulative score=" + player.getCumulativeScore());
        }

        if (this.dealer == null) throw new RuntimeException("logRoundScores: dealer is null" );
        this.currentPlayer = null;

    }


    void logFinalScores() {
        for (Player player : this) {
            Log.i(Game.APP_TAG, player.getName() + "'s final score is " + player.getCumulativeScore());
        }
    }


    /* GETTERS and SETTERS */

    Player getCurrentPlayer() {
        return currentPlayer;
    }

    Player getPlayerWentOut() {
        return playerWentOut;
    }

    /* Update PlayerLayout */
    void resetPlayerMiniHandsRoundStart() {
        for (Player player : this) player.resetPlayerMiniHand();
    }

    void updatePlayerMiniHands() {
        for (Player player : this) player.updatePlayerMiniHand(this.currentPlayer == player, true);
    }

    void removePlayerMiniHands(Activity a) {
        final RelativeLayout fullScreenContent = (RelativeLayout) a.findViewById(R.id.fullscreen_content);
        for (int iPlayer = 0; iPlayer < this.size(); iPlayer++) {
            PlayerMiniHandLayout playerMiniHandLayout = this.get(iPlayer).getMiniHandLayout();
            fullScreenContent.removeView(playerMiniHandLayout);
            this.get(iPlayer).removePlayerMiniHand();
        }
    }

    //re-layout when we've changed number of players by adding/deleting
    void relayoutPlayerMiniHands(Activity a) {
        removePlayerMiniHands(a);
        final RelativeLayout fullScreenContent = (RelativeLayout) a.findViewById(R.id.fullscreen_content);
        for (int iPlayer = 0; iPlayer < this.size(); iPlayer++) {
            fullScreenContent.addView(this.get(iPlayer).addPlayerMiniHandLayout(a, iPlayer, this.size()));
        }
    }
}
