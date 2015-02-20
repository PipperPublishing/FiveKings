package com.example.jeffrey.fivekings;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by Jeffrey on 1/22/2015.
* 2/3/2015 If DiscardPile reduces score, then use it, otherwise use drawPile
 * 2/4/2015 Remove drawPileCard from useDiscardPile decision (may later add back % decisions) and changes to separate meld/score with drawCard
 * 2/4/2015 Split discard/draw, meld&score, and discard into separate steps to make it easier to include human players
 * 2/15/2015 Sort unmelded cards for easier viewing
 * 2/15/2015 Initialize isFirstTurn and then check in useDiscardPile to see if we need to do initial melding
 * 2/17/2015 Don't use relativePosition for now
 * 2/17/2015    Added isHuman to control when you can click on piles
 *              Added checkHandSize() to make sure we have the right number of cards
*/
class Player {
    private boolean isHuman; //interact allowing clicking of Draw and Discard piles
    private String name;
    //dealer rotates every round, but relativePosition says where this player sits relative to others
    private int relativePosition;
    private int roundScore;
    private int numCards;
    private boolean isFirstTurn;
    private int cumulativeScore;
    private Hand hand;
    private boolean useDiscardPile=false;


    Player(String name, boolean isHuman) {
        this.name = name;
        this.isHuman = isHuman;
        this.relativePosition = 0; //not meaningful for now
        init();
    }

    boolean init() {
        cumulativeScore = 0;
        initRound(Rank.getLowestRank());
        return true;
    }
    boolean initRound(Rank roundOf) {
        hand = new Hand();
        roundScore = 0;
        this.numCards = roundOf.getRankValue();
        isFirstTurn = true;
        return true;
    }

    boolean initAndDealNewHand(CardList cards,Rank roundOf) {
        initRound(roundOf);
        return hand.dealNew(cards,roundOf.getRankValue());
    }

    static final Comparator<Player> playerComparatorByScoreDesc = new Comparator<Player>() {
        @Override
        public int compare(Player lhs, Player rhs) {
            return lhs.cumulativeScore - rhs.cumulativeScore;
        }
    };


    void evaluateIfFirstTurn(Rank roundOf, boolean usePermutations, boolean isFinalScore) {
        //if first Turn, we haven't got a score yet
        if (isFirstTurn) meldAndEvaluateAsIs(roundOf, usePermutations, isFinalScore);
        isFirstTurn = false;
    }

    //FIX-NEXT: clean up this mess
    void meldAndEvaluateAsIs(Rank roundOf, boolean usePermutations, boolean isFinalScore) {
        //currently uses heuristics to meld
        hand.meldAndEvaluate(roundOf, usePermutations, isFinalScore);
    }

    boolean useDiscardPile(Rank roundOf, boolean usePermutations, boolean isFinalScore, Card discardPileCard) {
        //if DiscardPile lowers current evaluation, then use it - otherwise DrawPile (so we no longer peek)
        //this will also avoid loops where we do not draw because it doesn't improve hand
        int beforeEvaluation = hand.getHandValueOrScore(isFinalScore);
        int afterEvaluation = hand.meldAndEvaluate(roundOf, usePermutations, isFinalScore, discardPileCard);
        this.useDiscardPile = (afterEvaluation < beforeEvaluation);
        return useDiscardPile;
    }

    //callback to meld and evaluate or just return existing results
    void meldAndEvaluate(Rank roundOf, boolean usePermutations, boolean isFinalScore, Card addedCard) {
        //already evaluated what to do for discard if this is automated player
        if (!this.useDiscardPile) hand.meldAndEvaluate(roundOf, usePermutations, isFinalScore, addedCard);
    }


    boolean isOut(Rank wildCardRank) {
        return (hand.getHandValueOrScore(true) == 0);
    }

    Card discardFromHand(Card cardToDiscard) {
        if (cardToDiscard == null) return null;
        hand.discardFrom(cardToDiscard);
        checkHandSize();
        return cardToDiscard;
    }

    boolean addCardToHand(Card card) {
        if (card == null) return false;
        checkHandSize();
        hand.add(card);
        return true;
    }

    private void checkHandSize() throws RuntimeException{
        if (hand.getLength() != this.numCards) throw new RuntimeException(Game.APP_TAG + "checkHandSize: Hand length is too short/long");
    }

    void update(String updatedName, boolean isHuman) {
        this.name = updatedName;
        this.isHuman = isHuman;
    }

    String getName() {
        return name;
    }

    String getMeldedString(boolean withBraces){
        StringBuilder mMelds = new StringBuilder("Melds ");
        if (withBraces) mMelds.append("{");
        mMelds.append(hand.getMeldedString());
        if (withBraces) mMelds.append("} ");
        return mMelds.toString();
    }

    String getPartialAndSingles(boolean withBraces) {
        String unMelded = hand.getUnMeldedString();
        String singles = hand.getSinglesString();
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
    Card getDiscard() {
        return hand.getLastDiscard();
    }

    int getHandValueOrScore(boolean isFinalScore) {
        return hand.getHandValueOrScore(isFinalScore);
    }

    int getRoundScore() {
        return roundScore;
    }

    void addToCumulativeScore() {
        roundScore = hand.getHandValueOrScore(true);

        cumulativeScore += roundScore;
    }

    ArrayList<CardList> getHandMelded() {
        return hand.getMelded();
    }

    ArrayList<CardList> getHandUnMelded() {
        ArrayList<CardList> combined = new ArrayList<>(hand.getUnMelded());
        combined.add(hand.getSingles());
        return combined;
    }

    int getCumulativeScore() {
        return cumulativeScore;
    }

    boolean isHuman() {
        return isHuman;
    }
}
