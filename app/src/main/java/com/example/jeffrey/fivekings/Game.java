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
 *  3/12/2015   v0.4.01:
 *              Make all method parameters final
 *              Merged endHumanTurn into endTurn
 *              Effects of subclassing Player --> ComputerPlayer, HumanPlayer
 *              Changed getNextPlayer() to always return current+1
 *              When updatePlayer or deletePlayer clicked in the middle of a round, we have to adjust
 *              currentPlayer etc.
 *              - Introduce enum PileDecision and Player.MeldMethod
 * 3/16/2015    Removed USE_DRAW_PILE, USE_DISCARD_PILE
 *
 */
public class Game {
    static final String APP_TAG = BuildConfig.VERSION_NAME;
    static final int MAX_PLAYERS=10;
    static final int MAX_CARDS=14; //Round of Kings + picked up card
    //to eventually move into a Settings dialog
    static final boolean SHOW_ALL_CARDS=true;
    private static final int PERMUTATION_THRESHOLD=750; //if longer than 0.75s for each player we switch to heuristic approach
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
    private Hand.MeldMethod method = Hand.MeldMethod.PERMUTATIONS;
    private GameState gameState=null;

    Game(final Context fkActivity) {
        this.deck = Deck.getInstance(true);
        this.players = new ArrayList<Player>(){{
            add(new EvaluationComputerPlayer("Evaluation"));
            add(new StrategyComputerPlayer("Strategy 1"));
            add(new StrategyComputerPlayer("Strategy 2"));
            add(new HumanPlayer("You"));
        }};
        init();
    }

    //can call this to have another game with same players and deck
    final boolean init() {
        deck.shuffle();
        this.dealer = players.get(0);
        for (Player player : this.players) player.initGame();
        this.roundOf = Rank.getLowestRank();
        this.method= Hand.MeldMethod.PERMUTATIONS;
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
        for (Player curPlayer : players) curPlayer.initAndDealNewHand(drawAndDiscardPiles.drawPile, this.roundOf, this.method);
        //turn up next card onto discard pile - note that the *top* card is actually the last element in the list
        drawAndDiscardPiles.dealToDiscard();

        this.playerWentOut = null;
        this.currentPlayer = dealer; //TURN_START now rotates player to current+1
        this.gameState = GameState.TURN_START;
        this.drawnCard=null;
    }//end initRound

    void takeHumanTurn(final String turnInfoFormat, final StringBuilder turnInfo, final PileDecision drawOrDiscardPile) {
        logTurn();

        drawnCard = (drawOrDiscardPile==PileDecision.DISCARD_PILE) ?  drawAndDiscardPiles.discardPile.deal() : drawAndDiscardPiles.drawPile.deal();
        ((HumanPlayer)currentPlayer).addAndEvaluate((playerWentOut != null), drawnCard);
        turnInfo.append(String.format(turnInfoFormat, currentPlayer.getName(), drawnCard.getCardString(),
                (drawOrDiscardPile==PileDecision.DISCARD_PILE) ? "Discard" : "Draw"));

        Log.d(APP_TAG, turnInfo.toString());
       //don't meld, because it was done as part of evaluation
       this.gameState = GameState.END_HUMAN_TURN;
    }

    PileDecision takeComputerTurn(final String turnInfoFormat, final StringBuilder turnInfo) {
        logTurn();

        //improve hand by picking up from Discard pile or from Draw pile - use pickFromDiscardPile to decide
        //Pick from drawPile unless you can meld/match from discardPile

        //if Auto/Computer, then we test whether discard improves score/valuation
        final long playerStartTime = System.currentTimeMillis();
        final PileDecision pickFrom = ((EvaluationComputerPlayer)currentPlayer).tryDiscardOrDrawPile(this.method, (playerWentOut != null),
                drawAndDiscardPiles.discardPile.peekNext(),drawAndDiscardPiles.drawPile.peekNext());
        //now actually deal the card
        if (pickFrom == PileDecision.DISCARD_PILE) {
            this.drawnCard = drawAndDiscardPiles.discardPile.deal();
            turnInfo.append(String.format(turnInfoFormat, currentPlayer.getName(), drawnCard.getCardString(),
                    "Discard", currentPlayer.getHandDiscard().getCardString()));
        }else { //DRAW_PILE
            //if we decided to not use the Discard pile, then we need to find discard and best hand with drawPile card
            this.drawnCard = drawAndDiscardPiles.drawPile.deal();
            turnInfo.append(String.format(turnInfoFormat, currentPlayer.getName(), drawnCard.getCardString(),
                    "Draw", currentPlayer.getHandDiscard().getCardString()));
        }
        final long playerStopTime = System.currentTimeMillis();
        if (this.method == Hand.MeldMethod.PERMUTATIONS)
            this.method = ((playerStopTime - playerStartTime) < PERMUTATION_THRESHOLD) ? Hand.MeldMethod.PERMUTATIONS : Hand.MeldMethod.HEURISTICS;

        Log.d(APP_TAG, turnInfo.toString());
        //Moved actual discard into endTurn so we can do animation at same time
        return pickFrom;
    }

 private void logTurn() {

     //Use final scoring (wild cards at full value) on last-licks turn (when a player has gone out)
     if (this.method == Hand.MeldMethod.PERMUTATIONS)
         Log.d(Game.APP_TAG, "Player " + currentPlayer.getName() + ": Using Permutations");
     else Log.d(Game.APP_TAG, "Player " + currentPlayer.getName() + ": Using Heuristics");
     String sValuationOrScore = (null == playerWentOut) ? "Valuation=" : "Score=";
     Log.d(APP_TAG, "before...... " + currentPlayer.getMeldedString(true) + currentPlayer.getPartialAndSingles(true) + " "
             + sValuationOrScore + currentPlayer.getHandValueOrScore(null != playerWentOut));
 }

    Player endTurn() {
        //remove discard from player's hand and add to discardPile
        drawAndDiscardPiles.discardPile.add(this.currentPlayer.discardFromHand(this.currentPlayer.getHandDiscard()));
        //only HumanPlayer does anything
        currentPlayer.checkMeldsAndEvaluate(playerWentOut != null);

        String sValuationOrScore = (null == playerWentOut) ? "Valuation=" : "Score=";
        Log.d(APP_TAG, "after...... " + currentPlayer.getMeldedString(true) + currentPlayer.getPartialAndSingles(true) + " " + sValuationOrScore + currentPlayer.getHandValueOrScore(null != playerWentOut));

        if ((playerWentOut == null) && currentPlayer.isOut()) playerWentOut = currentPlayer;
        if (playerWentOut != null) currentPlayer.updateRoundScore();
        return playerWentOut;
    }

    void checkEndRound() {
        //we've come back around to the player who went out
        if (getNextPlayer() == playerWentOut) this.gameState = GameState.ROUND_END;
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
        dealer = getNextPlayer();

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
    //TODO:A would make more sense to extend List and add these methods - would allow us to encapsulte player/next player
    //and also currentPlayer, dealer etc. which may change right now
    //int parameter returns that player
    Player getPlayer(final int iPlayer) {
        return players.get(iPlayer);
    }

    int getCurrentPlayerIndex() {
        return players.indexOf(this.currentPlayer);
    }

    void addPlayer (final String name, final boolean isHuman) {
        this.players.add(isHuman ? new HumanPlayer(name) : new EvaluationComputerPlayer(name));
    }

    void deletePlayer(final int iPlayer) {
        //need to advance to next player before we remove it
        if (this.players.get(iPlayer) == currentPlayer) currentPlayer = getNextPlayer();
        if (this.players.get(iPlayer) == playerWentOut) playerWentOut = null;
        this.players.remove(iPlayer);
    }

    void updatePlayer(final String name, final boolean isHuman, final int iPlayer) {
        final Player oldPlayer=this.players.get(iPlayer);
        final Player newPlayer;
        if (isHuman != this.players.get(iPlayer).isHuman()) {
            //if we're changing Human <-> Computer, then we need to create a new one as a copy
            newPlayer = isHuman ? new HumanPlayer(oldPlayer) : new EvaluationComputerPlayer(oldPlayer);
            this.players.set(iPlayer,newPlayer);
            if (oldPlayer == currentPlayer) currentPlayer = newPlayer;
            if (oldPlayer == playerWentOut) playerWentOut = newPlayer;
            if (oldPlayer == dealer) dealer = newPlayer;
        }
        else newPlayer=oldPlayer;
        //and now update the name (which is the only other thing that can be changed in the dialog)
        newPlayer.updateName(name);
    }

    Player rotatePlayer() {
        this.currentPlayer = getNextPlayer();
        return this.currentPlayer;
    }

    //by default, gets current+1
    Player getNextPlayer(){
        final Player nextPlayer;
        if (players.isEmpty()) nextPlayer=null;
        else {
            final int currentPlayerIndex = players.indexOf(this.currentPlayer);
            if (currentPlayerIndex == -1) nextPlayer = null;
            //if this is the "last" player, return the "first" one
            else if (currentPlayerIndex == players.size() - 1) nextPlayer = players.get(0);
            else nextPlayer =  players.get(currentPlayerIndex + 1);
        }
        return nextPlayer;
    }

    /* GETTERS and SETTERS */
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

    void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    List<Player> getPlayers() {
        return players;
    }

    enum PileDecision {DISCARD_PILE, DRAW_PILE}

}