package com.example.jeffrey.fivekings;

import java.util.Comparator;

/**
 * Created by Jeffrey on 1/22/2015.
* 2/3/2015 If DiscardPile reduces score, then use it, otherwise use drawPile
 * 2/4/2015 Remove drawPileCard from useDiscardPile decision (may later add back % decisions) and changes to separate meld/score with drawCard
 * 2/4/2015 Split discard/draw, meld&score, and discard into separate steps to make it easier to include human players
*/
class Player {
    private String name;
    //dealer rotates every round, but relativePosition says where this player sits relative to others
    private int relativePosition;
    private int roundScore;
    private int cumulativeScore;
    private Hand hand;
    private boolean useDiscardPile=false;


    Player(String name, int relativePosition) {
        this.name = name;
        this.relativePosition = relativePosition;
        init();
    }

    boolean init() {
        cumulativeScore = 0;
        initRound();
        return true;
    }
    boolean initRound() {
        hand = new Hand();
        roundScore = 0;
        return true;
    }

    static final Comparator<Player> playerComparatorByScoreDesc = new Comparator<Player>() {
        @Override
        public int compare(Player lhs, Player rhs) {
            return lhs.cumulativeScore - rhs.cumulativeScore;
        }
    };

    boolean useDiscardPile(Rank roundOf, boolean usePermutations, boolean isFinalScore, Card discardPileCard) {
        //if DiscardPile lowers current evaluation, then use it - otherwise DrawPile (so we no longer peek)
        //this will also avoid loops where we do not draw because it doesn't improve hand
        //TODO:A First evaluation of the round is faulty, because we haven't actually melded and scored
        //the hand we picked up
        int beforeEvaluation = hand.getHandValueOrScore(isFinalScore);
        int afterEvaluation = hand.meldAndEvaluate(roundOf, usePermutations, isFinalScore, discardPileCard);
        //TODO:C eventually should be able to track what is left in drawpile
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
        return cardToDiscard;
    }

    boolean addCardToHand(Card card) {
        if (card == null) return false;
        hand.add(card);
        return true;
    }

    boolean initAndDealNewHand(CardList cards, int numberToDeal) {
        initRound();
        return hand.dealNew(cards,numberToDeal);
    }

    String getName() {
        return name;
    }

    String getMeldedString(){
        return "Melds{"+hand.getMeldedString()+"} ";
    }

    String getPartialAndSingles() {
        String unMelded = hand.getUnMeldedString();
        String singles = hand.getSingles();
        StringBuffer partialAndSingles = new StringBuffer();
        if (!unMelded.isEmpty()) partialAndSingles.append("Potential melds{"+unMelded+"} ");
        if (!singles.isEmpty()) partialAndSingles.append("Unmelded{"+singles+"}");
        return partialAndSingles.toString();
    }
    Card getDiscard() {
        return hand.getLastDiscard();
    }

    int getHandValueOrScore(boolean isFinalScore) {
        return hand.getHandValueOrScore(isFinalScore);
    }

    public int getRoundScore() {
        return roundScore;
    }

    void addToCumulativeScore() {
        roundScore = hand.getHandValueOrScore(true);

        cumulativeScore += roundScore;
    }

    int getCumulativeScore() {
        return cumulativeScore;
    }

}
