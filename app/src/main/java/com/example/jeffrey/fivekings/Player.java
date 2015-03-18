package com.example.jeffrey.fivekings;

import android.content.Context;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Jeffrey on 1/22/2015.
* 2/3/2015 If DiscardPile reduces score, then use it, otherwise use drawPile
 * 2/4/2015 Remove drawPileCard from useDiscardPile decision (may later add back % decisions) and changes to separate meld/score with drawCard
 * 2/4/2015 Split discard/draw, meld&score, and discard into separate steps to make it easier to include human players
 * 2/15/2015 Sort unmelded cards for easier viewing
 * 2/15/2015 Initialize isFirstTurn and then check in useDiscardPile to see if we need to do initial melding
 * 2/17/2015 Don't use iPosition for now
 * 2/17/2015    Added isHuman to control when you can click on piles
 *              Added checkHandSize() to make sure we have the right number of cards
 * 2/24/2015    Move Hand to be inner class
 *              Eliminated meldAndEvaluateAsIs, evaluateIfFirst by melding when we deal the hand
 * 2/26/2015    getUnMelded returns melds and singles unrolled into one CardList for display
 * 2/27/2015    Added addToMeld and makeNewMeld; include checking for dropping back onto itself
 * 2/28/2015    Added Meld definition to record valuation (tells us whether it's a valid meld)
 * 3/3/2015     Added static method to check if a CardList is a valid meld
 * 3/4/2015     Removed meldAndEvaluate (replaced with checkMeldsAndEvaluate)
 * 3/7/2015     Zero round score in initGame so that display shows correctly
 *              and split updateRoundScore out so it can be called in endTurn
 * 3/11/2015    Moved updatePlayerLayout to PlayerLayout as "update"
 * 3/12/2015    v0.4.01:
 *              Change method parameters to final
 * 3/13/2015    Moved calculateValue to be a Player method so it can be overridden and brought cardScore in here from Card
 * 3/14/2015    meldUsingHeuristics: When melding singles need 2 wildcards to make a meld from a single (don't keep partials with a wildcard)
 *              Removed iPosition; maintaining this during deletions or adds would be painful
 * 3/15/2015    meldUsingHeuristics needs to add wildcards into singles if they aren't melded away
 * 3/16/2015    Convert to Meld using Hand; also create a PlayerHand which has roundOf and discard added
 *              and relevant methods
 * TODO:A : Need to eliminate partial melds of wildcards in Permutations
 * TODO:A Move roundScore to PlayerHand
 */
abstract class Player implements HandComparator {
    private String name;
    //dealer rotates every round, but iPosition says where this player sits relative to others
    private int roundScore;
    private int cumulativeScore;
    protected PlayerHand hand;

    private PlayerLayout playerLayout; //representation on-screen including score etc

    Player(final String name) {
        this.name = name;
        initGame();
    }

    //Copy constructor
    Player(final Player player) {
        this.name = player.name;
        this.roundScore = player.roundScore;
        this.cumulativeScore = player.cumulativeScore;
        this.hand = player.hand;
        this.playerLayout = player.playerLayout;
    }

    final boolean initGame() {
        this.cumulativeScore = 0;
        this.roundScore = 0; //only zero this here because we display all scores at the start of a new game
        this.hand=null;
        this.playerLayout=null;
        return true;
    }

    boolean initAndDealNewHand(DrawAndDiscardPiles.DrawPile drawPile,Rank roundOf, Hand.MeldMethod method) {
        this.roundScore = 0;
        this.hand = new PlayerHand(drawPile, roundOf);
        return true;
    }

    @Deprecated
    static final Comparator<Player> playerComparatorByScoreDesc = new Comparator<Player>() {
        @Override
        public int compare(final Player lhs, final Player rhs) {
            return lhs.cumulativeScore - rhs.cumulativeScore;
        }
    };


    //default implementation does nothing
    int checkMeldsAndEvaluate(boolean isFinalRound) {return -1;}

    abstract Card discardFromHand(final Card cardToDiscard);

    protected void checkHandSize() throws RuntimeException{
        if (!hand.checkSize()) throw new RuntimeException(Game.APP_TAG + "checkHandSize: Hand length is too short/long");
    }


    final PlayerLayout addPlayerLayout(final Context c, final int iPlayer, final int numPlayers)  {
        this.playerLayout = new PlayerLayout(c, this.name, iPlayer, numPlayers);
        return this.playerLayout;
    }

    final void removePlayerLayout() {
        this.playerLayout = null;
    }

    //Player GETTERS and SETTERS
    final int updateRoundScore() {
        roundScore = hand.getValueOrScore(true);
        return roundScore;
    }

    final void updateName(final String updatedName) {
        this.name = updatedName;
        this.playerLayout.updateName(updatedName);
    }

    final int addToCumulativeScore() {
        cumulativeScore += roundScore;
        return cumulativeScore;
    }

    final String getName() {
        return name;
    }

    final Card getHandDiscard() { return hand.getDiscard();}

    final String getMeldedString(final boolean withBraces){
        StringBuilder mMelds = new StringBuilder("Melds ");
        if (withBraces) mMelds.append("{");
        mMelds.append(Hand.getString(hand.melds));
        if (withBraces) mMelds.append("} ");
        return mMelds.toString();
    }

    final String getPartialAndSingles(final boolean withBraces) {
        String unMelded = Hand.getString(hand.partialMelds);
        String singles = hand.singles.getString();
        StringBuilder partialAndSingles = new StringBuilder();
        if (!unMelded.isEmpty()) {
            partialAndSingles.append("Potential melds ");
            if (withBraces) partialAndSingles.append("{");
            partialAndSingles.append(unMelded);
            if (withBraces) partialAndSingles.append("} ");
        }
        if (!singles.isEmpty()) {
            partialAndSingles.append("Unmelded");
            if (withBraces) partialAndSingles.append("{");
            partialAndSingles.append(singles);
            if (withBraces) partialAndSingles.append("} ");
        }
        return partialAndSingles.toString();
    }
   final int getHandValueOrScore(final boolean isFinalScore) {
        return hand.getValueOrScore(isFinalScore);
   }

   final int getRoundScore() {
        return roundScore;
    }

   final PlayerLayout getPlayerLayout() {
        return playerLayout;
    }

   final ArrayList<CardList> getHandMelded() {
        ArrayList<CardList> combined = new ArrayList<>();
        combined.addAll(hand.getMelded());
        return combined;
   }


    //TODO:A: Not unrolling these right now (Human doesn't see this)
    //because otherwise we don't know what to add back to
    //have to eliminate "combined"
   final ArrayList<CardList> getHandUnMelded() {
        ArrayList<CardList> combined = new ArrayList<>();
        combined.addAll(hand.getUnMelded());
        combined.add(hand.getSingles());
        return combined;
   }


   final CardList getHandSingles() {
        return this.hand.getSingles();
    }

   final int getCumulativeScore() {
        return cumulativeScore;
    }

   abstract boolean isHuman();

   final boolean isOut() {
        return ((hand != null) && (hand.getValueOrScore(true) == 0));
    }

   void setHandDiscard(Card discard) {hand.setDiscard(discard);}

    /*-------------------------------------------------------------
    /* INNER CLASS: PlayerHand - use of Hand specific to Player */
    /*-----------------------------------------------------------*/
   protected class PlayerHand extends Hand {
        private Card discard; //discard associated with this hand

        PlayerHand(final Rank roundOf) {
            super(roundOf, Player.this);
            discard = null;
        }

        //deal and return hand
        private PlayerHand(final DrawAndDiscardPiles.DrawPile drawPile, final Rank roundOf) {
            this(roundOf);
            if (drawPile != null) {
                cards = drawPile.deal(roundOf.getRankValue());
                singles = new Meld(this.playerHandComparator, cards); //only for human really
            }
        }

        //minimal constructor - copies and sets the cards (used in trying out different discards)
        //note that it doesn't copy other values which are not relevant
        protected PlayerHand(final Rank roundOf, final CardList cards, final Card discard) {
            this(roundOf);
            this.cards = cards;
            this.discard = discard;
        }

        private boolean checkSize() {
            return roundOf.getRankValue() == cards.size();
        }

        final Card getDiscard() {
            return this.discard;
        }

        protected void setDiscard(final Card discard) {
            if (null == discard)throw new RuntimeException("setDiscard: discard == null");
            this.discard = discard;
        }

    }//end Inner Class PlayerHand

    /* STATIC METHODS FOR PlayerLayout */
    static void updatePlayerHands(final List<Player> players, final Player currentPlayer) {
        for (Player player: players) {
            if (player.getPlayerLayout() == null) continue;
            player.getPlayerLayout().update(currentPlayer == player, player.isOut(),
                    player.getRoundScore(), player.getCumulativeScore());
        }
    }

    static void resetAndUpdatePlayerHands(final List<Player> players, final Player currentPlayer) {
        for (Player player : players) {
            player.getPlayerLayout().setPlayedInFinalRound(false);
            player.getPlayerLayout().setGreyedOut(true);
        }
        updatePlayerHands(players, currentPlayer);
    }

   public boolean isFirstBetterThanSecond(final Hand testHand, final Hand bestHand, final boolean isFinalRound) {
        return testHand.getValueOrScore(isFinalRound) <= bestHand.getValueOrScore(isFinalRound);
   }


}
