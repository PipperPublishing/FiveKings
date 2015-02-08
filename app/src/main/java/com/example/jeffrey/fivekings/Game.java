package com.example.jeffrey.fivekings;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeffrey on 1/22/2015.
 *  * 2/2/2015 Test how long each v1 (using permutations) is taking and switch to heuristic approach once over the threshold
 *  2/3/2015    Push draw-pile/discard-pile decision into Player so that we can replace with human - no longer peeks at DrawPile
 *  2/4/2015    Do discard vs draw decision and meld/score/discard as separate steps
 */
public class Game {
    private static final boolean DEBUG=false;
    private static final boolean IS_FINAL_SCORE=true;
    private static final boolean IS_NOT_FINAL_SCORE=false;
    private static int PERMUTATION_THRESHOLD=200; //if longer than 0.5s for each player we switch to heuristic approach
    static final boolean USE_DRAW_PILE=false;
    static final boolean USE_DISCARD_PILE=true;
    static final String APP_TAG = "Five Kings:";

    private static Deck deck = new Deck(true);

    //List of players sorted into correct relative position
    private List<Player> players;

    private DiscardPile discardPile;
    private DrawPile drawPile;
    private Player dealer;


    Game() {
        //TODO: For testing, hardcode player positions
        this.players = new ArrayList<Player>(){{add(new Player("NORTH",0));add(new Player("EAST",1));
            add(new Player("SOUTH",2)); add(new Player("WEST",3));}};
        init();
    }

    //can call this to have another game with same players and deck
    boolean init() {
        deck.shuffle();
        dealer = players.get(0);
        return true;
    }

    void play() {
        EvaluationWrapper resultsWrapper= new EvaluationWrapper();
        long roundStartTime, roundStopTime;
        long playerStartTime, playerStopTime;
        boolean usePermutations=true;
        boolean useDiscardPile=USE_DISCARD_PILE;

        for (Rank roundOf : Rank.values()){
            Log.i(APP_TAG,"------------");
            Log.i(APP_TAG,"Round of "+roundOf.getRankString()+"'s:");
            roundStartTime = System.currentTimeMillis();
            //shuffle the deck
            deck.shuffle();
            discardPile = new DiscardPile();
            //we don't copy the Deck set, but we do add all the CardList objects so we can manipulate it without
            //then having to reassemble the deck
            drawPile = new DrawPile(deck);

            //deal cards for each player - we just ignore who the "dealer" is, since the deck is guaranteed random
            for (Player curPlayer : players)
                curPlayer.initAndDealNewHand(drawPile, roundOf.getRankValue());

            //turn up next card onto discard pile - note that the *top* card is actually the last element in the list
            discardPile.add(drawPile.deal());
            Player playerWentOut=null;

            for (Player player=getNextPlayer(dealer); player != playerWentOut ; player = getNextPlayer(player) ) {
                //improve hand by picking up from Discard pile or from Draw pile - use useDiscardPile to decide
                //TODO: v3 of strategy would look also at what other people had been picking up
                //Pick from drawPile unless you can meld/match from discardPile
                //discardPile never runs out, but drawPile may need to be refreshed
                if (null == drawPile.peekNext()) reDeal();

                //Use final scoring (wild cards at full value) on last-licks turn (when a player has gone out)
                if (usePermutations) Log.d(Game.APP_TAG,"Player "+player.getName() + ": Using Permutations");
                else Log.d(Game.APP_TAG,"Player "+player.getName() + ": Using Heuristics");
                String sValuationOrScore= (null==playerWentOut) ? "Valuation=" : "Score=";
                Log.d(APP_TAG, "before...... " + player.getMeldedString() + player.getPartialAndSingles() + " "
                        +sValuationOrScore + player.getHandValueOrScore(null!=playerWentOut));
                playerStartTime = System.currentTimeMillis();
                useDiscardPile = player.useDiscardPile(roundOf, usePermutations, (playerWentOut != null), discardPile.peekNext());

                //Now meld, score, and decide on discard - if it's an automated player and the Discard Pile we'll just get back the existing results
                if (useDiscardPile) {
                    player.meldAndEvaluate(roundOf, usePermutations, (playerWentOut != null), discardPile.peekNext());
                    Log.d(Game.APP_TAG,"Drawing from Discard pile = "+discardPile.peekNext().getCardString()+", Draw pile = ?; discarded the "+ player.getDiscard().getCardString());
                    player.addCardToHand(discardPile.deal());
                }
                else {
                    player.meldAndEvaluate(roundOf, usePermutations, (playerWentOut != null), drawPile.peekNext());
                    Log.d(Game.APP_TAG,"Discard pile = "+discardPile.peekNext().getCardString()+", Drawing from Draw pile = "+drawPile.peekNext().getCardString()
                            +"; discarded the "+player.getDiscard().getCardString());
                    player.addCardToHand(drawPile.deal());
                }
                discardPile.add( player.discardFromHand(player.getDiscard()));

                playerStopTime = System.currentTimeMillis();
                usePermutations = DEBUG || (usePermutations && ((playerStopTime-playerStartTime) < PERMUTATION_THRESHOLD));
                Log.d(APP_TAG, "after...... " + player.getMeldedString() + player.getPartialAndSingles() + " "+sValuationOrScore + player.getHandValueOrScore(null!=playerWentOut));
                if ((playerWentOut==null) && player.isOut(roundOf)) playerWentOut = player;
            }//end for Player (loop until somebody goes out)

            if (playerWentOut == null) throw new RuntimeException("Error - playerWentOut is null");
            roundStopTime = System.currentTimeMillis();
            Log.d(APP_TAG, String.format("Elapsed time = %.2f seconds", (roundStopTime - roundStartTime)/1000.0));
            Log.i(APP_TAG, "Player "+playerWentOut.getName()+" went out");
            Log.i(APP_TAG, "        Current scores:");
            for (Player player : players){
                //add ending score to this players cumulative Score
                //Pass IS_FINAL_SCORE to make sure we count unmelded Jokers and wild cards
                player.addToCumulativeScore();
                Log.i(APP_TAG, "Player " + player.getName() + ": " + player.getMeldedString() + player.getPartialAndSingles() + ". Cumulative score=" + player.getCumulativeScore());
            }

            //and rotate the dealer
            dealer = getNextPlayer(dealer);
            if (dealer == null) throw new RuntimeException("Error in " + roundOf.getRankString() + " round" );
        }//end of rounds

        //show final scores
        for (Player player : players) {
            Log.i(APP_TAG,player.getName()+"'s final score is "+ player.getCumulativeScore());
        }
    }

    private Player getNextPlayer(Player currentPlayer){
        if ((currentPlayer == null) || (players.isEmpty())) return null;
        final int currentPlayerIndex = players.indexOf(currentPlayer);
        if (currentPlayerIndex == -1) return null;

        //if this is the "last" player, return the "first" one
        if (currentPlayerIndex == players.size()-1) return players.get(0);
        else return players.get(currentPlayerIndex+1);
    }

    private void reDeal() {
        Log.d(APP_TAG, "Redealing Discard Pile");
        drawPile = new DrawPile(discardPile);
        drawPile.shuffle();
        discardPile.clear();
        discardPile.add(drawPile.deal());
    }
}