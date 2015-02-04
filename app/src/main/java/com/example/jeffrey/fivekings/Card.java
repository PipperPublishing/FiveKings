package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 1/22/2015.
 * 2/2/2015 Changed cards to be final - no setters because you cannot change values
 * 2/3/2015 Added null suit constructor (for Jokers)
 */
//TODO:C Better design would pull Jokers up into Card completely
class Card {
    static final int FINAL_WILD_CARD_VALUE =20;
    static final int INTERMEDIATE_WILD_CARD_VALUE=1;

    private final Suit suit;
    private final Rank rank;

    Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }
    //currently used for Jokers
    Card(Rank rank) {
        this.suit = null;
        this.rank = rank;
    }

    //returns false if wildCardRank is null
    boolean isWildCard(Rank wildCardRank){
        return ((wildCardRank != null) && ((rank == Rank.JOKER) || (rank == wildCardRank)));
    }

    int getRankValue() {
        return rank.getRankValue();
    }

    //by default, rank wildCards (not Jokers) have value INTERMEDIATE_WILDCARD_VALUE so that there is incentive to meld them
    //but not incentive to discard them
    //In final scoring they have full value (50)
    int getScore(Rank wildCardRank, boolean isFinal) {
        //if final, then rank wildCard=20, otherwise Rank value (includes Joker=50)
        if (isFinal) return (isSameRank(wildCardRank) ? FINAL_WILD_CARD_VALUE : this.getRankValue());
        //if intermediate, then any wildcard=1 (including Jokers), otherwise Rank value
        else return (isWildCard(wildCardRank) ? INTERMEDIATE_WILD_CARD_VALUE : this.getRankValue());
    }

    String getCardString() {return getRankString()+getSuitString();}
    private String getRankString() { return rank.getRankString(); }
    private String getSuitString() {
        if (null == suit) return "";
        else return suit.getSuitString();
    }

    boolean isSameRank(Card card) { return this.rank == card.rank; }
    boolean isSameRank(Rank rank) { return this.rank==rank;}

    boolean isSameSuit(Card card) {return this.getSuit() == card.getSuit();}
    boolean isSameSuit(Suit suit) {return this.getSuit() == suit;}

    int getRankDifference(Card card) {return card.getRankValue() - this.getRankValue();}

    Rank getRank() {
        return rank;
    }
    Suit getSuit() {
        return suit;
    }
}
