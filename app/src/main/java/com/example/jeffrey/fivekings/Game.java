package com.example.jeffrey.fivekings;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
 */
public class Game {
    private static int PERMUTATION_THRESHOLD=1000; //if longer than 1s for each player we switch to heuristic approach
    static final boolean USE_DRAW_PILE=false;
    static final boolean USE_DISCARD_PILE=true;
    static final String APP_TAG = BuildConfig.VERSION_NAME;
    static final int MAX_PLAYERS=10;
    static final int MAX_CARDS=14; //Round of Kings + picked up card

    //to eventually move into a Settings dialog
    static final boolean SHOW_ALL_CARDS=false;

    private Deck deck;

    //List of players sorted into correct relative position
    private List<Player> players;
    private Rank roundOf;
    private Player dealer;
    private Player player; //current player
    private Player playerWentOut;
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
        this.player = getNextPlayer(dealer);
        this.gameState = GameState.TURN_START;
    }//end initRound

    void takeHumanTurn(String turnInfoFormat, StringBuilder turnInfo, boolean useDiscardPile) {
        logTurn();

        Card drawnCard=null;
        long playerStartTime = System.currentTimeMillis();
        if (useDiscardPile) {
            drawnCard = drawAndDiscardPiles.discardPile.peekNext();
            player.addAndEvaluate(usePermutations, (playerWentOut != null), drawAndDiscardPiles.discardPile.deal());
            turnInfo.append(String.format(turnInfoFormat, player.getName(), drawnCard.getCardString(), "Discard"));
        } else {
            drawnCard = drawAndDiscardPiles.drawPile.peekNext();
            player.addAndEvaluate(usePermutations, (playerWentOut != null), drawAndDiscardPiles.drawPile.deal());
            turnInfo.append(String.format(turnInfoFormat, player.getName(), drawnCard.getCardString(), "Draw"));
        }
        long playerStopTime = System.currentTimeMillis();
        usePermutations = usePermutations && ((playerStopTime - playerStartTime) < PERMUTATION_THRESHOLD);

       //don't meld, because it was done as part of evaluation
       this.gameState = GameState.END_HUMAN_TURN;
    }

    Player takeComputerTurn(String turnInfoFormat, StringBuilder turnInfo) {
        logTurn();

        //improve hand by picking up from Discard pile or from Draw pile - use useDiscardPile to decide
        //Pick from drawPile unless you can meld/match from discardPile

        Card drawnCard=null;
        //if Auto/Computer, then we test whether discard improves score/valuation
        long playerStartTime = System.currentTimeMillis();
        boolean useDiscardPile = player.findBestHand(usePermutations, (playerWentOut != null), drawAndDiscardPiles.discardPile.peekNext());
        if (useDiscardPile) {
            drawnCard = drawAndDiscardPiles.discardPile.deal(); //note use of deal() here
            //already melded and have discard
            turnInfo.append(String.format(turnInfoFormat, player.getName(), drawnCard.getCardString(), "Discard", player.getDiscard().getCardString()));

        }else {
            drawnCard = drawAndDiscardPiles.drawPile.peekNext();
            player.findBestHand(usePermutations, (playerWentOut != null), drawAndDiscardPiles.drawPile.deal());
            turnInfo.append(String.format(turnInfoFormat, player.getName(), drawnCard.getCardString(), "Draw", player.getDiscard().getCardString()));
        }
        long playerStopTime = System.currentTimeMillis();
        usePermutations = usePermutations && ((playerStopTime - playerStartTime) < PERMUTATION_THRESHOLD);


        //this actually does discard for humans; for computer it's already been done in findBestHand
        drawAndDiscardPiles.discardPile.add(player.discardFromHand(player.getDiscard()));
        String sValuationOrScore = (null == playerWentOut) ? "Valuation=" : "Score=";
        Log.d(APP_TAG, "after...... " + player.getMeldedString(true) + player.getPartialAndSingles(true) + " " + sValuationOrScore + player.getHandValueOrScore(null != playerWentOut));

        if ((playerWentOut == null) && player.isOut()) playerWentOut = player;
        return playerWentOut;
    }

 private void logTurn() {

     //Use final scoring (wild cards at full value) on last-licks turn (when a player has gone out)
     if (usePermutations)
         Log.d(Game.APP_TAG, "Player " + player.getName() + ": Using Permutations");
     else Log.d(Game.APP_TAG, "Player " + player.getName() + ": Using Heuristics");
     String sValuationOrScore = (null == playerWentOut) ? "Valuation=" : "Score=";
     Log.d(APP_TAG, "before...... " + player.getMeldedString(true) + player.getPartialAndSingles(true) + " "
             + sValuationOrScore + player.getHandValueOrScore(null != playerWentOut));
 }

    Player endHumanTurn(Card discard) {
        String sValuationOrScore = (null == playerWentOut) ? "Valuation=" : "Score=";

        //Now discard and meld again
        drawAndDiscardPiles.discardPile.add(player.discardFromHand(discard));
        //But we do need to evaluate whether the melds are all done
        player.checkMeldsAndEvaluate(playerWentOut != null);
        Log.d(APP_TAG, "You discarded the " + discard.getCardString());
        Log.d(APP_TAG, "after...... " + player.getMeldedString(true) + player.getPartialAndSingles(true) + " " + sValuationOrScore + player.getHandValueOrScore(null != playerWentOut));

        if ((playerWentOut == null) && player.isOut()) playerWentOut = player;
        return playerWentOut;
    }
    boolean endTurn() {
        player = getNextPlayer(player);
        //we've come back around to the player who went out
        if (player == playerWentOut) this.gameState = GameState.ROUND_END;
        else this.gameState = GameState.TURN_START;
        return (this.gameState == GameState.TURN_START);
    }

    Rank endRound() {
        if (playerWentOut == null) throw new RuntimeException("Error - playerWentOut is null");
        roundStopTime = System.currentTimeMillis();
        Log.d(APP_TAG, String.format("Elapsed time = %.2f seconds", (roundStopTime - roundStartTime)/1000.0));
        Log.i(APP_TAG, "Player "+playerWentOut.getName()+" went out");
        Log.i(APP_TAG, "        Current scores:");
        for (Player player : players){
            //add ending score to this players cumulative Score
            //Pass IS_FINAL_SCORE to make sure we count unmelded Jokers and wild cards
            player.addToCumulativeScore();
            Log.i(APP_TAG, "Player " + player.getName() + ": " + player.getMeldedString(true) + player.getPartialAndSingles(true) + ". Cumulative score=" + player.getCumulativeScore());
        }

        //and rotate the dealer
        dealer = getNextPlayer(dealer);

        if (dealer == null) throw new RuntimeException("Error in " + roundOf.getString() + " round" );

        roundOf = roundOf.getNext();
        player = null;
        if (null == roundOf) this.gameState = GameState.GAME_END;
        else this.gameState = GameState.ROUND_START;
        return roundOf;
    }


    Player getNextPlayer(Player currentPlayer){
        if ((currentPlayer == null) || (players.isEmpty())) return null;
        final int currentPlayerIndex = players.indexOf(currentPlayer);
        if (currentPlayerIndex == -1) return null;

        //if this is the "last" player, return the "first" one
        if (currentPlayerIndex == players.size()-1) return players.get(0);
        else return players.get(currentPlayerIndex+1);
    }

    void logFinalScores() {
        for (Player player : players) {
            Log.i(APP_TAG, player.getName() + "'s final score is " + player.getCumulativeScore());
        }
    }


    Drawable getDiscardPileDrawable(Context c) {
        //This is the only time when DiscardPile should be empty - when we've just picked up
        if (drawAndDiscardPiles.discardPile.peekNext() == null) return null;
        else {
            CardView cv = new CardView(c,drawAndDiscardPiles.discardPile.peekNext(),0 );
            return cv.getDrawable();
        }
    }

    void addPlayer (String name, boolean isHuman) {
        this.players.add(new Player(name, isHuman));
    }
    void updatePlayer(String name, boolean isHuman, int iPlayer) {
        this.players.get(iPlayer).update(name, isHuman);
    }


    /* GETTERS and SETTERS */
    Player getDealer() {
        return dealer;
    }

    Player getPlayerWentOut() {
        return playerWentOut;
    }

    Player getPlayer() {
        return player;
    }

    Rank getRoundOf() {
        return roundOf;
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