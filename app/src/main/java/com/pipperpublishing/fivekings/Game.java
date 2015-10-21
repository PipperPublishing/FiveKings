package com.pipperpublishing.fivekings;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.animation.Animation;

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
    8/30/2015   Made Game parcelable as first step towards saving state
 9/3/2015       Switch parceling gameState to toString/valueOf
 10/1/2015      checkEndRound sets to ROUND_END to make it easier to restart in the middle
 10/3/2015      Moved Meld-and-discard hint out of disableDrawDiscardClick into calling method
 10/12/2015     Moved default player setup to Game() constructor
 10/16/2015     Removed discard from player step into endTurnCheckRound so no longer have to pass deck
 10/18/2015     checkEndRound now sets TURN_END which clicking on a new player will change to TURN_START
 10/20/2015     Moved clickedDrawOrDiscard to FiveKings with other event handlers
                Moved isShowComputerCards and isAnimateDealing to FiveKings
 *
 */
public class Game implements Parcelable{
    static final String APP_TAG = BuildConfig.VERSION_NAME;
    static final int MAX_PLAYERS = 10;
    static final int MAX_CARDS = 14; //Round of Kings + picked up card
    // in the Menu Settings dialog
    private boolean showComputerCards = false;
    private boolean animateDealing = true;

    private final Deck deck;
    //List of players sorted into correct relative position
    private final PlayerList players;

    private Rank roundOf;
    private long roundStartTime, roundStopTime;

    private GameState gameState;

    enum PileDecision {DISCARD_PILE, DRAW_PILE}


    Game(final FiveKings fKActivity) {
        this.deck = Deck.getInstance(true);
        this.players = new PlayerList();
        this.players.addPlayer(fKActivity.getString(R.string.defaultComputerPlayer), PlayerList.PlayerType.EXPERT_COMPUTER);
        this.players.addPlayer(fKActivity.getString(R.string.defaultHumanPlayer), PlayerList.PlayerType.HUMAN);
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

        //shuffle the deck
        deck.shuffle();
        //creates the draw and discard piles and copies the deck to the drawPile (by adding the cards)
        deck.initDrawAndDiscardPile();
        //deal cards for each player - we just ignore who the "dealer" is, since the deck is guaranteed random
        for (Player curPlayer : players) curPlayer.initAndDealNewHand(deck, this.roundOf);
        //turn up next card onto discard pile - note that the *top* card is actually the last element in the list
        deck.dealToDiscard();

        this.players.initRound();

    }//end initRound

    final Rank checkEndRound() {
        //don't advance to next player - instead do that when player is clicked
        //we've come back around to the player who went out
        if (players.nextPlayerWentOut()) {
            roundStopTime = System.currentTimeMillis();
            Log.d(Game.APP_TAG, String.format("Elapsed time = %.2f seconds", (roundStopTime - roundStartTime) / 1000.0));

            players.logRoundScores();

            roundOf = roundOf.getNext();
            this.gameState = roundOf == null ? GameState.GAME_END : GameState.ROUND_END;
        } else this.gameState = GameState.TURN_END ;
        return roundOf;
    }

    final Card peekDiscardPileCard() {
        //Possibly null (after just picking up)
        return deck.peekFromDiscardPile();
    }

    /*----------------------------------------------*/
    /* Helper methods so we don't get players list */
    /*----------------------------------------------*/
    final void logFinalScores() {
        this.players.logFinalScores();
    }

    final void setHandDiscard(final Card discard) {
        players.getCurrentPlayer().setHandDiscard(discard);
    }

    final void addPlayer(final String playerName, final PlayerList.PlayerType playerType, final FiveKings fKActivity) {
        if ((getRoundOf() != null) && (getRoundOf() != Rank.getLowestRank())) {
            Log.e(Game.APP_TAG, "Can't add players after Round of 3's");
        } else {
            this.players.addPlayer(playerName, playerType);
            relayoutPlayerMiniHands(fKActivity);
        }
    }

    final void updatePlayer(final String playerName, final boolean isHuman, final int iPlayer) {
        this.players.updatePlayer(playerName, isHuman, iPlayer);
        updatePlayerMiniHands();
    }

    final void deletePlayer(final int iPlayerToDelete, final Activity activity) {
        this.players.deletePlayer(iPlayerToDelete, activity);
    }

    final void relayoutPlayerMiniHands(final Activity a) {
        this.players.relayoutPlayerMiniHands(a);
    }

    final void removePlayerMiniHands(final Activity a) {
        this.players.removePlayerMiniHands(a);
    }

    final void resetPlayerMiniHandsToRoundStart() {
        players.resetPlayerMiniHandsRoundStart();
        players.updatePlayerMiniHands();
    }

    final void updatePlayerMiniHands() {
        this.players.updatePlayerMiniHands();
    }

    void setMiniHandsSolid() {this.players.setMiniHandsSolid();}

    Player getPlayerByIndex(final int iPlayer) {
        return players.get(iPlayer);
    }

    Player getPlayerWentOut() {
        return players.getPlayerWentOut();
    }

    Player getCurrentPlayer() {
        return players.getCurrentPlayer();
    }

    Player getNextPlayer() {
        return players.getNextPlayer();
    }

    Player getWinner() {
        return players.getWinner();
    }

    boolean isFinalTurn() {
        return players.getPlayerWentOut() != null;
    }

    void rotatePlayer() {
        this.gameState = GameState.TURN_START;
        this.players.rotatePlayer();
    }

    int numPlayers() {
        return this.players.size();
    }

    void animatePlayerMiniHand(final Player setAnimatedPlayerHand, final Animation bounceAnimation) {
        //starts animation on the appropriate hand and passes back special hint if Human->Human
        this.players.setAnimated(setAnimatedPlayerHand, bounceAnimation);
    }

    boolean currentAndNextAreHuman() {
        return players.getCurrentPlayer().isHuman() && players.getNextPlayer().isHuman();
    }

    /* Starting and ending turns */
    void findBestHandStart() {
        getNextPlayer().findBestHandStart(isFinalTurn(), peekDiscardPileCard());
    }

    Player endCurrentPlayerTurn() {
        return players.endCurrentPlayerTurn();
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


    /* IMPLEMENTATION OF PARCELABLE */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeByte((byte)(showComputerCards ? 1:0));
        out.writeByte((byte)(animateDealing ? 1:0));

        out.writeLong(roundStartTime);
        out.writeLong(roundStopTime);

        out.writeValue(deck);
        out.writeValue(players);
        out.writeValue(roundOf);
        out.writeString(gameState.toString());
    }

    public static final Parcelable.Creator<Game> CREATOR
            = new Parcelable.Creator<Game>() {
            @Override
            public Game createFromParcel(Parcel in) {
                return new Game(in);
            }

            @Override
            public Game[] newArray(int size) {
                return new Game[size];
            }
        };

    //recreate object from parcel
    private Game(Parcel in) {
        showComputerCards = in.readByte() != 0;
        animateDealing = in.readByte() != 0;

        roundStartTime = in.readLong();
        roundStopTime = in.readLong();

        deck = (Deck) in.readValue(Deck.class.getClassLoader());
        players = (PlayerList) in.readValue(PlayerList.class.getClassLoader());
        roundOf = (Rank) in.readValue(Rank.class.getClassLoader());
        gameState = GameState.valueOf(in.readString());
    }
}