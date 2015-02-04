package com.example.jeffrey.fivekings;

import java.util.ArrayList;

/**
 * Created by Jeffrey on 1/22/2015.
* 2/3/2015 If DiscardPile reduces score, then use it, otherwise use drawPile
*/
class Player {
    private String name;
    //dealer rotates every round, but relativePosition says where this player sits relative to others
    private int relativePosition;
    private int cumulativeScore;
    private Hand hand;


    Player(String name, int relativePosition) {
        this.name = name;
        this.relativePosition = relativePosition;
        init();
    }

    boolean init() {
        cumulativeScore = 0;
        hand = new Hand();
        return true;
    }


    boolean useDrawOrDiscard(Rank roundOf, boolean usePermutations, boolean isFinalScore, Card drawPileCard, Card discardPileCard, EvaluationWrapper wrapper) {
        //if DiscardPile lowers current score, then use it - otherwise DrawPile (so we no longer peek)
        //this will also avoid loops where we do not draw because it doesn't improve hand
        //TODO:C eventually should be able to track what is left in drawpile
        int score = hand.meldAndScore(roundOf,usePermutations ,isFinalScore, discardPileCard, wrapper);
        if (score < hand.getScore(roundOf, isFinalScore))
            return Game.USE_DISCARD_PILE;
        else {
            score = hand.meldAndScore(roundOf, usePermutations, isFinalScore, drawPileCard, wrapper);
            return Game.USE_DRAW_PILE;
        }
    }

    boolean isOut(Rank wildCardRank) {
        return (hand.getScore(wildCardRank, true) == 0);
    }

    boolean discardFromHand(Card cardToDiscard) {
        if (cardToDiscard == null) return false;
        else return (hand.discardFrom(cardToDiscard) != null);
    }

    boolean addCardToHand(Card card) {
        if (card == null) return false;
        hand.add(card);
        return true;
    }

    boolean dealNewHand(CardList cards, int numberToDeal) {
        return hand.dealNew(cards,numberToDeal);
    }

    String getName() {
        return name;
    }

    String getMeldedString(){ return hand.getMeldedString();}

    String getUnMeldedString() { return hand.getUnMeldedString();}

    void setHandMelds(ArrayList<CardList> melds, ArrayList<CardList> unMelded){
        if (melds != null) hand.setMelds(melds);
        if (unMelded != null) hand.setUnMelded(unMelded);
    }

    void addToCumulativeScore(Rank wildCardRank, boolean isFinalScore) {
        cumulativeScore += hand.getScore(wildCardRank, isFinalScore);
    }

    int getCumulativeScore() {
        return cumulativeScore;
    }

}
