package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 1/22/2015.
 * 2/2/2015 Changed cards to be final - no setters because you cannot change values
 * 2/3/2015 Added null suit constructor (for Jokers)
 * 2/4/2015 Moved Joker handling up here and out of Rank enum
 */
//TODO:B Intermediate and final scoring should be moved out of Card probably
    //TODO:B Create a subclass for Jokers which doesn't have rank and suit
class Card {
    //can put these into resource files
    static final int INTERMEDIATE_WILD_CARD_VALUE=1;
    static final int FINAL_WILD_CARD_VALUE =20;
    static final int FINAL_JOKER_VALUE=50;
    static final String JOKER_STRING="Jok";

    private final Suit suit;
    private final Rank rank;
    private final String cardString;
    private final int cardValue;
    private final boolean isJoker;

    Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
        this.cardString = rank.getRankString() + suit.getSuitString();
        this.cardValue = rank.getRankValue();
        this.isJoker = false;
    }
    //no arguments constructor is for Jokers
    Card() {
        this.isJoker = true;
        this.suit = null;
        this.rank = null;
        this.cardString = JOKER_STRING;
        this.cardValue = FINAL_JOKER_VALUE;
    }

    //true if a rank wildcard or a Joker; false if not (or null)
    boolean isWildCard(Rank wildCardRank){
        return ((wildCardRank != null) && (isJoker || (rank == wildCardRank)));
    }

    //should not get called with Jokers
    int getRankValue() {
        if (null == rank) throw new RuntimeException("getRankValue: called with rank==null");
        return rank.getRankValue();
    }

    //by default, wildcards have value INTERMEDIATE_WILDCARD_VALUE so that there is incentive to meld them
    //but not incentive to discard them
    //In final scoring they have full value (20 for rank wildcards, and 50 for Jokers)
    int getScore(Rank wildCardRank, boolean isFinal) {
        int cardScore=0;
        //if final, then rank wildCard=20, Joker=50, otherwise Rank value
        if (isFinal) {
            if (!isJoker && isWildCard(wildCardRank)) cardScore = FINAL_WILD_CARD_VALUE;
            else cardScore = this.cardValue; //includes final Joker value
        }
        //if intermediate, then any wildcard=1 (including Jokers), otherwise Rank value
        else cardScore = (isWildCard(wildCardRank) ? INTERMEDIATE_WILD_CARD_VALUE : this.cardValue);
        return cardScore;
    }

    String getCardString() {return this.cardString;}

    boolean isSameRank(Card card) { return this.rank == card.rank; }
    boolean isSameRank(Rank rank) { return this.rank == rank;}

    boolean isSameSuit(Card card) {return this.suit == card.suit;}
    boolean isSameSuit(Suit suit) {return this.suit == suit;}

    int getRankDifference(Card card) {return card.cardValue - this.cardValue;}

    Rank getRank() {
        return this.rank;
    }
    Suit getSuit() {
        return this.suit;
    }
}
