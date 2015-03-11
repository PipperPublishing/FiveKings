package com.example.jeffrey.fivekings;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeffrey on 1/22/2015.
 *  * 2/2/2015 Test how long each v1 (using permutations) is taking and switch to heuristic approach once over the threshold
 *  2/3/2015    Push draw-pile/discard-pile decision into Player so that we can replace with human - no longer peeks at DrawPile
 *  2/4/2015    Do discard vs draw decision and meld/score/discard as separate steps
 *  2/10/2015   Separate into play, nextRound, takeComputerTurn to interact properly with UI
 *  2/16/2015   Use BuildConfig rather than hardcoding app name
 *  2/17/2015   Change hardcoding of players' names and add more
 *  2/17/2015   Add player.init() loop to Game.init() (zero's scores)
 *  2/17/2015   Add takeHumanTurn to separate picking and discarding
 *  2/17/2015   Made deck a singleton
 *  2/19/2015   Start with Computer and You as hard-coded players; you can add more or change names
 *  2/20/2015   Moved reDeal to new DrawAndDiscardPiles to encapsulate it
 *  2/24/2015   Separated logic for Human/Computer more clearly in takeTurn
 *  2/26/2015   Hide computer cards except in last round when it goes out
 *  3/5/2015    Remove setting usePermutations in takeHumanTurn (is only relevant to Computer turn)
 *              Simplify takeHumanTurn and make drawnCard more like takeComputerTurn; simplify takeComputerTurn
 *              Rename endTurn to checkEndRound and remove getNextPlayer() (was showing human cards prematurely)
 *              Added rotatePlayer (acts on this.player) and getNextPlayer (if called with null, then act on this.player)
 *  3/6/2015    Added back an endTurn to hold the discard, so animation now correctly shows the next card if you draw from the Discard Pile
 *  3/11/2015   Call updateRoundScore in endTurn so round scores update after each final turn

 *
 */
public class Game {
    private static int PERMUTATION_THRESHOLD=750; //if longer than 0.75s for each player we switch to heuristic approach
    static final boolean USE_DRAW_PILE=false;
    static final boolean USE_DISCARD_PILE=true;
    static final String APP_TAG = BuildConfig.VERSION_NAME;
    static final int MAX_PLAYERS=10;
    static final int MAX_CARDS=14; //Round of Kings + picked up card

    //to eventually move into a Settings dialog
    static final boolean SHOW_ALL_CARDS=false;

    private final Deck deck;
    //List of players sorted into correct relative position
    private final List<Player> players;

    private Rank roundOf;
    private Player dealer;
    private Player currentPlayer;
    private Player playerWentOut;
    private Card drawnCard;
    private long roundStartTime, roundStopTime;

    private DrawAndDiscardPiles drawAndDiscardPiles;
    private boolean usePermutations=true;
    private GameState gameState=null;


    Game(Context fkActivity) {
        this.deck = Deck.getInstance(true, fkActivity);
        this.players = new ArrayList<Player>(){{
            add(new Player("Computer", false)); add(new Player("You", true));
        }};
        init();
    }


    //can call this to have another game with same players and deck
    boolean init() {
        deck.shuffle();
        this.dealer = players.get(0);
        for (Player player : this.players) player.initGame();
        this.roundOf = Rank.getLowestRank();
        this.usePermutations=true;
        this.gameState = GameState.ROUND_START;
        return true;
    }

    void initRound() {
        Log.i(APP_TAG, "------------");
        Log.i(APP_TAG, "Round of " + roundOf.getString() + "'s:");
        roundStartTime = System.currentTimeMillis();

        //shuffle the deck- possibly should also be part of DrawAndDiscardPiles
        deck.shuffle();
        //creates the draw and discard piles and copies the deck to the drawPile (by adding the cards)
        drawAndDiscardPiles = new DrawAndDiscardPiles(deck);
        //deal cards for each player - we just ignore who the "dealer" is, since the deck is guaranteed random
        for (Player curPlayer : players) curPlayer.initAndDealNewHand(drawAndDiscardPiles.drawPile, roundOf, this.usePermutations);
        //turn up next card onto discard pile - note that the *top* card is actually the last element in the list
        drawAndDiscardPiles.dealToDiscard();

        this.playerWentOut = null;
        this.currentPlayer = dealer; //TURN_START now sets player=getNextPlayer(player)
        this.gameState = GameState.TURN_START;
        this.drawnCard=null;
    }//end initRound

    void takeHumanTurn(String turnInfoFormat, StringBuilder turnInfo, boolean pickFromDiscardPile) {
        logTurn();

        drawnCard = pickFromDiscardPile ?  drawAndDiscardPiles.discardPile.deal() : drawAndDiscardPiles.drawPile.deal();
        currentPlayer.addAndEvaluate(usePermutations, (playerWentOut != null), drawnCard);
        turnInfo.append(String.format(turnInfoFormat, currentPlayer.getName(), drawnCard.getCardString(),
                pickFromDiscardPile ? "Discard" : "Draw"));

       //don't meld, because it was done as part of evaluation
       this.gameState = GameState.END_HUMAN_TURN;
    }

    boolean takeComputerTurn(String turnInfoFormat, StringBuilder turnInfo) {
        logTurn();

        //improve hand by picking up from Discard pile or from Draw pile - use pickFromDiscardPile to decide
        //Pick from drawPile unless you can meld/match from discardPile

        //if Auto/Computer, then we test whether discard improves score/valuation
        final long playerStartTime = System.currentTimeMillis();
        final boolean pickFromDiscardPile = currentPlayer.findBestHand(usePermutations, (playerWentOut != null), drawAndDiscardPiles.discardPile.peekNext());
        drawnCard = pickFromDiscardPile ?  drawAndDiscardPiles.discardPile.deal() : drawAndDiscardPiles.drawPile.deal();
        //if we decided to not use the Discard pile, then we need to find discard and best hand with drawPile card
        if (!pickFromDiscardPile) currentPlayer.findBestHand(usePermutations, (playerWentOut != null), drawnCard);
        turnInfo.append(String.format(turnInfoFormat, currentPlayer.getName(), drawnCard.getCardString(),
                pickFromDiscardPile ? "Discard":"Draw", currentPlayer.getDiscard().getCardString()));
        final long playerStopTime = System.currentTimeMillis();
        usePermutations = usePermutations && ((playerStopTime - playerStartTime) < PERMUTATION_THRESHOLD);

        //Moved actual discard into endTurn so we can do animation at same time
        return pickFromDiscardPile;
    }

 private void logTurn() {

     //Use final scoring (wild cards at full value) on last-licks turn (when a player has gone out)
     if (usePermutations)
         Log.d(Game.APP_TAG, "Player " + currentPlayer.getName() + ": Using Permutations");
     else Log.d(Game.APP_TAG, "Player " + currentPlayer.getName() + ": Using Heuristics");
     String sValuationOrScore = (null == playerWentOut) ? "Valuation=" : "Score=";
     Log.d(APP_TAG, "before...... " + currentPlayer.getMeldedString(true) + currentPlayer.getPartialAndSingles(true) + " "
             + sValuationOrScore + currentPlayer.getHandValueOrScore(null != playerWentOut));
 }

    //TODO: A merge with endTurn and checkEndRound
    Player endHumanTurn(Card discard) {
        currentPlayer.setBestDiscard(discard);     //not previously set until we dragged the discard
        return endTurn();
    }

    Player endTurn() {
        //remove discard from player's hand and add to discardPile
        drawAndDiscardPiles.discardPile.add(this.currentPlayer.discardFromHand(this.currentPlayer.getDiscard()));
        if (this.currentPlayer.isHuman()) currentPlayer.checkMeldsAndEvaluate(playerWentOut != null);

        String sValuationOrScore = (null == playerWentOut) ? "Valuation=" : "Score=";
        Log.d(APP_TAG, "after...... " + currentPlayer.getMeldedString(true) + currentPlayer.getPartialAndSingles(true) + " " + sValuationOrScore + currentPlayer.getHandValueOrScore(null != playerWentOut));

        if ((playerWentOut == null) && currentPlayer.isOut()) playerWentOut = currentPlayer;
        if (playerWentOut != null) currentPlayer.updateRoundScore();
        return playerWentOut;
    }

    void checkEndRound() {
        //we've come back around to the player who went out
        if (getNextPlayer(currentPlayer) == playerWentOut) this.gameState = GameState.ROUND_END;
        else this.gameState = GameState.TURN_START;
    }

    Rank endRound() {
        if (playerWentOut == null) throw new RuntimeException("Error - playerWentOut is null");
        roundStopTime = System.currentTimeMillis();
        Log.d(APP_TAG, String.format("Elapsed time = %.2f seconds", (roundStopTime - roundStartTime)/1000.0));
        Log.i(APP_TAG, "Player "+playerWentOut.getName()+" went out");
        Log.i(APP_TAG, "        Current scores:");
        for (Player player : players){
            //add round score (previously updated in endTurn) to this players cumulative Score
            player.addToCumulativeScore();
            Log.i(APP_TAG, "Player " + player.getName() + ": " + player.getMeldedString(true) + player.getPartialAndSingles(true) + ". Cumulative score=" + player.getCumulativeScore());
        }

        //and rotate the dealer
        dealer = getNextPlayer(dealer);

        if (dealer == null) throw new RuntimeException("Error in " + roundOf.getString() + " round" );

        roundOf = roundOf.getNext();
        currentPlayer = null;
        if (null == roundOf) this.gameState = GameState.GAME_END;
        else this.gameState = GameState.ROUND_START;
        return roundOf;
    }


    void logFinalScores() {
        for (Player player : players) {
            Log.i(APP_TAG, player.getName() + "'s final score is " + player.getCumulativeScore());
        }
    }


    Card getDiscardPileCard() {
        //Possibly null (after just picking up)
         return drawAndDiscardPiles.discardPile.peekNext();
    }


    /* UTILITIES FOR LIST OF PLAYERS */
    //TODO:B would make more sense to extend List and add these methods - would allow us to encapsulte player/next player
    //int parameter returns that player
    Player getPlayer(int iPlayer) {
        return players.get(iPlayer);
    }

    int getCurrentPlayerIndex() {
        return players.indexOf(this.currentPlayer);
    }

    void addPlayer (String name, boolean isHuman) {
        this.players.add(new Player(name, isHuman));
    }
    void updatePlayer(String name, boolean isHuman, int iPlayer) {
        this.players.get(iPlayer).update(name, isHuman);
    }

    Player rotatePlayer() {
        this.currentPlayer = getNextPlayer(this.currentPlayer);
        return this.currentPlayer;
    }
    //If called with null, act on this.currentPlayer
    Player getNextPlayer(Player player){
        if (players.isEmpty()) return null;
        final int currentPlayerIndex = players.indexOf(player==null ? this.currentPlayer : player);
        if (currentPlayerIndex == -1) return null;

        //if this is the "last" player, return the "first" one
        if (currentPlayerIndex == players.size()-1) return players.get(0);
        else return players.get(currentPlayerIndex+1);
    }



    /* GETTERS and SETTERS */
    Player getDealer() {
        return dealer;
    }

    Player getPlayerWentOut() {
        return playerWentOut;
    }

    Player getCurrentPlayer() {
        return currentPlayer;
    }

    Rank getRoundOf() {
        return roundOf;
    }

    Card getDrawnCard() {
        return drawnCard;
    }

    GameState getGameState() {
        return gameState;
    }

    List<Player> getPlayers() {
        return players;
    }

    void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

}