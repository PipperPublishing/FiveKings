package com.example.jeffrey.fivekings;

import android.app.Activity;
import android.util.Log;

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
 *              Rename endCurrentPlayerTurn to checkEndRound and remove getNextPlayer() (was showing human cards prematurely)
 *              Added rotatePlayer (acts on this.player) and getNextPlayer (if called with null, then act on this.player)
 *  3/6/2015    Added back an endCurrentPlayerTurn to hold the discard, so animation now correctly shows the next card if you draw from the Discard Pile
 *  3/11/2015   Call updateRoundScore in endCurrentPlayerTurn so round scores update after each final turn
 *  3/12/2015   v0.4.01:
 *              Make all method parameters final
 *              Merged endHumanTurn into endCurrentPlayerTurn
 *              Effects of subclassing Player --> ComputerPlayer, HumanPlayer
 *              Changed getNextPlayer() to always return current+1
 *              When updatePlayer or deletePlayer clicked in the middle of a round, we have to adjust
 *              currentPlayer etc.
 *              - Introduce enum PileDecision and Player.MeldMethod
 * 3/16/2015    Removed USE_DRAW_PILE, USE_DISCARD_PILE
 * 3/22/2015    Change takeComputerTurn and takeHumanTurn to Player methods
 * 3/31/2015    Replace logRoundScores with checkEndRound + other logic
 *              Added delegate methods, especially to playerList
 * 4/1/2015     Removed ROUND_END assignment
 *              Added
 *
 */
public class Game {
    static final String APP_TAG = BuildConfig.VERSION_NAME;
    static final int MAX_PLAYERS=10;
    static final int MAX_CARDS=14; //Round of Kings + picked up card
    // in the Menu Settings dialog
    private boolean showComputerCards =false;
    private boolean animateDealing=true;

    private final Deck deck;
    //List of players sorted into correct relative position
    private final PlayerList players;

    private Rank roundOf;
    private long roundStartTime, roundStopTime;

    private GameState gameState;

    static enum PileDecision {DISCARD_PILE, DRAW_PILE}


    Game() {
        this.deck = Deck.getInstance(true);
        this.players = new PlayerList();
        this.players.addStandardPlayers();
        this.gameState = GameState.NEW_GAME;
        roundStartTime = 0;
        roundStopTime = 0;
    }

    //can call this to have another game with same players and deck
    final boolean init() {
        deck.shuffle();
        this.players.initGame();
        this.roundOf = Rank.getLowestRank();
        this.gameState = GameState.ROUND_START;

        return true;
    }

    final void initRound() {
        Log.i(APP_TAG, "------------");
        Log.i(APP_TAG, "Round of " + roundOf.getString() + "'s:");
        roundStartTime = System.currentTimeMillis();

        //shuffle the deck- possibly should also be part of DrawAndDiscardPiles
        deck.shuffle();
        //creates the draw and discard piles and copies the deck to the drawPile (by adding the cards)
        deck.initDrawAndDiscardPile();
        //deal cards for each player - we just ignore who the "dealer" is, since the deck is guaranteed random
        for (Player curPlayer : players) curPlayer.initAndDealNewHand(deck.drawPile, this.roundOf);
        //turn up next card onto discard pile - note that the *top* card is actually the last element in the list
        deck.dealToDiscard();

        this.players.initRound();

        this.gameState = GameState.TURN_START;
    }//end initRound

    final Rank checkEndRound() {
        //don't advance to next player - instead do that when player is clicked
        //we've come back around to the player who went out
        if (players.nextPlayerWentOut()) {
            roundStopTime = System.currentTimeMillis();
            Log.d(Game.APP_TAG, String.format("Elapsed time = %.2f seconds", (roundStopTime - roundStartTime) / 1000.0));

            players.logRoundScores();

            roundOf = roundOf.getNext();
            this.gameState = roundOf == null ? GameState.GAME_END : GameState.ROUND_START;
        }
        else this.gameState = GameState.TURN_START;
        return roundOf;
    }


    // Handler when Draw or Discard pile clicked
    void clickedDrawOrDiscard(final FiveKings fKActivity, final Game.PileDecision drawOrDiscardPile) {
        setGameState(GameState.HUMAN_PICKED_CARD);
        fKActivity.disableDrawDiscardClick();
        getCurrentPlayer().takeTurn(fKActivity, getCurrentPlayer().getMiniHandLayout(), drawOrDiscardPile, deck, isFinalTurn());
    }

    Card peekDiscardPileCard() {
        //Possibly null (after just picking up)
         return deck.discardPile.peekNext();
    }

    /*----------------------------------------------*/
    /* Helper methods so we don't get players list */
    /*----------------------------------------------*/
    void logFinalScores() {
        this.players.logFinalScores();
    }

    void setHandDiscard(final Card discard) {
        players.getCurrentPlayer().setHandDiscard(discard);
    }

    void addPlayer(final String playerName, final boolean isHuman) {
        this.players.addPlayer(playerName, isHuman ? PlayerList.PlayerType.HUMAN : PlayerList.PlayerType.EXPERT_COMPUTER);
    }
    void updatePlayer(final String playerName, final boolean isHuman, final int iPlayer) {
        this.players.updatePlayer(playerName, isHuman, iPlayer);
    }

    void deletePlayer(final int iPlayerToDelete, final Activity activity) {
        this.players.deletePlayer(iPlayerToDelete, activity);
    }

    void relayoutPlayerMiniHands(final Activity a) {
        this.players.relayoutPlayerMiniHands(a);
    }
    void removePlayerMiniHands(final Activity a) { this.players.removePlayerMiniHands(a);}
    void resetPlayerMiniHandsToRoundStart() {
        players.resetPlayerMiniHandsRoundStart();
        players.updatePlayerMiniHands(false);
    }

    void updatePlayerMiniHands(final boolean isRoundStart) {
        this.players.updatePlayerMiniHands(isRoundStart);
    }

    String getNextPlayerName() {
        return this.players.getNextPlayer().getName();
    }

    Player getPlayerByIndex(final int iPlayer) {
        return players.get(iPlayer);
    }

    Player getPlayerWentOut() {
        return players.getPlayerWentOut();
    }

    Player getCurrentPlayer() {
        return players.getCurrentPlayer();
    }

    Player getNextPlayer() { return players.getNextPlayer();}

    boolean isFinalTurn() { return players.getPlayerWentOut() != null;}

    Card getDrawnCard() {
        return getCurrentPlayer().getDrawnCard();
    }

    void rotatePlayer() {
        this.players.rotatePlayer();
    }

    int numPlayers() { return this.players.size();}

    Player endTurn() {
        return players.endCurrentPlayerTurn(deck);
    }


    /* GETTERS and SETTERS */
    Rank getRoundOf() {
        return roundOf;
    }

    GameState getGameState() {
        return gameState;
    }

    void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    Deck getDeck() {
        return deck;
    }

    boolean isShowComputerCards() {
        return showComputerCards;
    }

    void setShowComputerCards(boolean showComputerCards) {
        this.showComputerCards = showComputerCards;
    }

    boolean isAnimateDealing() {
        return animateDealing;
    }

    void setAnimateDealing(boolean animateDealing) {
        this.animateDealing = animateDealing;
    }
}