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
 *  2/10/2015   Separate into play, nextRound, takeAutoTurn to interact properly with UI
 *  2/16/2015   Use BuildConfig rather than hardcoding app name
 *  2/17/2015   Change hardcoding of players' names and add more
 *  2/17/2015   Add player.init() loop to Game.init() (zero's scores)
 *  2/17/2015   Add takeHumanTurn to separate picking and discarding
 *  2/17/2015   Made deck a singleton
 *  2/19/2015   Start with Computer and You as hard-coded players; you can add more or change names
 */
public class Game {
    private static final boolean DEBUG=false;
    private static final boolean IS_FINAL_SCORE=true;
    private static final boolean IS_NOT_FINAL_SCORE=false;
    private static int PERMUTATION_THRESHOLD=500; //if longer than 0.5s for each player we switch to heuristic approach
    static final boolean USE_DRAW_PILE=false;
    static final boolean USE_DISCARD_PILE=true;
    static final String APP_TAG = BuildConfig.VERSION_NAME;
    static final int MAX_PLAYERS=12;
    static final int MAX_CARDS=14; //Round of Kings + picked up card

    private Deck deck;
    private Context context;

    //List of players sorted into correct relative position
    private List<Player> players;
    private Rank roundOf;
    private Player dealer;
    private Player player; //current player
    private Player playerWentOut;
    private long roundStartTime, roundStopTime;

    private DiscardPile discardPile;
    private DrawPile drawPile;
    private boolean usePermutations=true;
    private GameState gameState=null;


    Game(Context fkActivity) {
        this.context = fkActivity;
        this.deck = Deck.getInstance(true, context);
        this.players = new ArrayList<Player>(){{
            add(new Player("Computer", false)); add(new Player("You", true));
        }};
        init();
    }


    //can call this to have another game with same players and deck
    boolean init() {
        deck.shuffle();
        this.dealer = players.get(0);
        for (Player player : this.players) player.init();
        this.roundOf = Rank.values()[0];
        this.usePermutations=true;
        this.gameState = GameState.ROUND_START;
        return true;
    }

    void initRound() {
        Log.i(APP_TAG, "------------");
        Log.i(APP_TAG, "Round of " + roundOf.getRankString() + "'s:");
        roundStartTime = System.currentTimeMillis();
        //shuffle the deck
        deck.shuffle();
        discardPile = new DiscardPile();
        //we don't copy the Deck set, but we do add all the CardList objects so we can manipulate it without
        //then having to reassemble the deck
        drawPile = new DrawPile(deck);

        //deal cards for each player - we just ignore who the "dealer" is, since the deck is guaranteed random
        for (Player curPlayer : players)
            curPlayer.initAndDealNewHand(drawPile, roundOf);

        //turn up next card onto discard pile - note that the *top* card is actually the last element in the list
        discardPile.add(drawPile.deal());
        this.playerWentOut = null;
        this.player = getNextPlayer(dealer);
        this.gameState = GameState.TURN_START;
    }//end initRound

    Player takeHumanTurn(String turnInfoFormat, StringBuilder turnInfo, boolean useDiscardPile) {
        long playerStartTime, playerStopTime;

        //improve hand by picking up from Discard pile or from Draw pile - use useDiscardPile to decide
        //Pick from drawPile unless you can meld/match from discardPile
        //discardPile never runs out, but drawPile may need to be refreshed
        if (null == drawPile.peekNext()) reDeal();

        //Use final scoring (wild cards at full value) on last-licks turn (when a player has gone out)
        if (usePermutations)
            Log.d(Game.APP_TAG, "Player " + player.getName() + ": Using Permutations");
        else Log.d(Game.APP_TAG, "Player " + player.getName() + ": Using Heuristics");
        String sValuationOrScore = (null == playerWentOut) ? "Valuation=" : "Score=";
        player.evaluateIfFirstTurn(roundOf, usePermutations, (playerWentOut != null));
        Log.d(APP_TAG, "before...... " + player.getMeldedString(true) + player.getPartialAndSingles(true) + " "
                + sValuationOrScore + player.getHandValueOrScore(null != playerWentOut));

        playerStartTime = System.currentTimeMillis();
        //Now meld and score (discard is a separate UI call)
        //FIX-NEXT Clean this up to not make redundant calls (two different calls in Player - subclass Player to Human and not)
        if (useDiscardPile) {
            player.useDiscardPile(roundOf, usePermutations, (playerWentOut != null), discardPile.peekNext());
            player.meldAndEvaluate(roundOf, usePermutations, (playerWentOut != null), discardPile.peekNext());
            turnInfo.append(String.format(turnInfoFormat,player.getName(),discardPile.peekNext().getCardString(),"Discard"));
            Log.d(Game.APP_TAG, turnInfo.toString());
            player.addCardToHand(discardPile.deal());
        } else {
            player.meldAndEvaluate(roundOf, usePermutations, (playerWentOut != null), drawPile.peekNext());
            turnInfo.append(String.format(turnInfoFormat,player.getName(),drawPile.peekNext().getCardString(),"Draw"));
            Log.d(Game.APP_TAG, turnInfo.toString());
            player.addCardToHand(drawPile.deal());
        }
        playerStopTime = System.currentTimeMillis();
        usePermutations = DEBUG || (usePermutations && ((playerStopTime - playerStartTime) < PERMUTATION_THRESHOLD));
        player.meldAndEvaluateAsIs(this.roundOf, this.usePermutations, playerWentOut != null);
        this.gameState = GameState.END_HUMAN_TURN;
        return null;
    }

    Player endHumanTurn(Card discard) {
        String sValuationOrScore = (null == playerWentOut) ? "Valuation=" : "Score=";

        discardPile.add(player.discardFromHand(discard));
        player.meldAndEvaluateAsIs(this.roundOf, this.usePermutations, playerWentOut != null);
        Log.d(APP_TAG, "You discarded the " + discard.getCardString());
        Log.d(APP_TAG, "after...... " + player.getMeldedString(true) + player.getPartialAndSingles(true) + " " + sValuationOrScore + player.getHandValueOrScore(null != playerWentOut));

        if ((playerWentOut == null) && player.isOut(roundOf)) playerWentOut = player;
        return playerWentOut;
    }

    Player takeAutoTurn(String turnInfoFormat, StringBuilder turnInfo) {
        final boolean useDiscardPile;
        long playerStartTime, playerStopTime;

        //improve hand by picking up from Discard pile or from Draw pile - use useDiscardPile to decide
        //Pick from drawPile unless you can meld/match from discardPile
        //discardPile never runs out, but drawPile may need to be refreshed
        if (null == drawPile.peekNext()) reDeal();

        //Use final scoring (wild cards at full value) on last-licks turn (when a player has gone out)
        if (usePermutations)
            Log.d(Game.APP_TAG, "Player " + player.getName() + ": Using Permutations");
        else Log.d(Game.APP_TAG, "Player " + player.getName() + ": Using Heuristics");
        String sValuationOrScore = (null == playerWentOut) ? "Valuation=" : "Score=";
        player.evaluateIfFirstTurn(roundOf, usePermutations, (playerWentOut != null));
        Log.d(APP_TAG, "before...... " + player.getMeldedString(true) + player.getPartialAndSingles(true) + " "
                + sValuationOrScore + player.getHandValueOrScore(null != playerWentOut));
        playerStartTime = System.currentTimeMillis();
        useDiscardPile = player.useDiscardPile(roundOf, usePermutations, (playerWentOut != null), discardPile.peekNext());

        //Now meld, score, and decide on discard - if it's an automated player and the Discard Pile we'll just get back the existing results
        if (useDiscardPile) {
            player.meldAndEvaluate(roundOf, usePermutations, (playerWentOut != null), discardPile.peekNext());
            turnInfo.append(String.format(turnInfoFormat,player.getName(),discardPile.peekNext().getCardString(),"Discard",player.getDiscard().getCardString()));
            Log.d(Game.APP_TAG, turnInfo.toString());
            player.addCardToHand(discardPile.deal());
        } else {
            player.meldAndEvaluate(roundOf, usePermutations, (playerWentOut != null), drawPile.peekNext());
            turnInfo.append(String.format(turnInfoFormat,player.getName(),drawPile.peekNext().getCardString(),"Draw",player.getDiscard().getCardString()));
            Log.d(Game.APP_TAG, turnInfo.toString());
            player.addCardToHand(drawPile.deal());
        }
        discardPile.add(player.discardFromHand(player.getDiscard()));

        playerStopTime = System.currentTimeMillis();
        usePermutations = DEBUG || (usePermutations && ((playerStopTime - playerStartTime) < PERMUTATION_THRESHOLD));
        Log.d(APP_TAG, "after...... " + player.getMeldedString(true) + player.getPartialAndSingles(true) + " " + sValuationOrScore + player.getHandValueOrScore(null != playerWentOut));

        if ((playerWentOut == null) && player.isOut(roundOf)) playerWentOut = player;
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

        if (dealer == null) throw new RuntimeException("Error in " + roundOf.getRankString() + " round" );

        roundOf = roundOf.getNext();
        player = null;
        if (null == roundOf) this.gameState = GameState.GAME_END;
        else this.gameState = GameState.ROUND_START;
        return roundOf;
    }


    boolean isRoundOver() {
        return player==playerWentOut;
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

    private void reDeal() {
        Log.d(APP_TAG, "Redealing Discard Pile");
        drawPile = new DrawPile(discardPile);
        drawPile.shuffle();
        discardPile.clear();
        discardPile.add(drawPile.deal());
    }

    Drawable getDiscardPileDrawable() {
        //This is the only time when DiscardPile should be empty - when we've just picked up
        if (discardPile.peekNext() == null) return null;
        else return discardPile.peekNext().getDrawable();
    }

    Player getDealer() {
        return dealer;
    }

    Player getPlayer() {
        return player;
    }

    void setPlayer(Player player) {
        this.player = player;
    }

    Rank getRoundOf() {
        return roundOf;
    }

    Context getContext() {
        return context;
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

    void addPlayer (String name, boolean isHuman) {
        this.players.add(new Player(name, isHuman));
    }
    void updatePlayer(String name, boolean isHuman, int iPlayer) {
        this.players.get(iPlayer).update(name, isHuman);
    }

}