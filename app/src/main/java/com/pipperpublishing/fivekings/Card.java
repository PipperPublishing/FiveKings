package com.pipperpublishing.fivekings;

import java.util.Comparator;

/**
 * Created by Jeffrey on 1/22/2015.
 * 2/2/2015 Changed cards to be final - no setters because you cannot change values
 * 2/3/2015 Added null suit constructor (for Jokers)
 * 2/4/2015 Moved Joker handling up here and out of Rank enum
 * 2/14/2015 Added bmp's for cards; resources are hard-coded to card rank and suit
 * 2/19/2015    Removed bitmap
 * 2/23/2015    Correctly interpret Joker String (have to explicitly get from resource)
 * 2/26/2015    Move drawables to CardView; changed Joker back to hardcoded string
 * 3/13/2015    Moved cardScore to Player.calculateCardScore (because it involves Final vs not Final)
 * 3/13/2015    Subclassed Jokers
 */
class Card {
    private static final int WILD_CARD_VALUE =20;

    //sorting for sequences (within the same suit)
    static final Comparator<Card> cardComparatorSuitFirst = new Comparator<Card>() {
        @Override
        public int compare(Card card1, Card card2) {
            if (card1.isJoker() && card2.isJoker()) return 0;
            if (card1.isJoker()) return card2.getRankValue();
            if (card2.isJoker()) return -card1.getRankValue();
            int suitCmp = card1.suit.compareTo(card2.suit);
            if (suitCmp != 0) return suitCmp;
            return card1.rank.compareTo(card2.rank);
        }
    };
    //sorting for rank melds (so we meld higher value cards first)
    static final Comparator<Card> cardComparatorRankFirstDesc = new Comparator<Card>() {
        @Override
        public int compare(Card card1, Card card2) {
            if (card1.isJoker() && card2.isJoker()) return 0;
            if (card1.isJoker()) return card2.getRankValue();
            if (card2.isJoker()) return -card1.getRankValue();
            int rankCmp = -card1.rank.compareTo(card2.rank);
            if (rankCmp != 0) return rankCmp;
            return card1.suit.compareTo(card2.suit);
        }
    };

    static final Comparator<Card> cardComparatorRankFirstAsc = new Comparator<Card>() {
        @Override
        public int compare(Card card1, Card card2) {
            if (card1.isJoker() && card2.isJoker()) return 0;
            if (card1.isJoker()) return -card2.getRankValue();
            if (card2.isJoker()) return card1.getRankValue();
            int rankCmp = card1.rank.compareTo(card2.rank);
            if (rankCmp != 0) return rankCmp;
            return card1.suit.compareTo(card2.suit);
        }
    };

    private final Suit suit;
    private final Rank rank;
    protected String cardString;
    protected int cardValue;

    Card(final Suit suit, final Rank rank) {
        this.suit = suit;
        this.rank = rank;
        if ((rank != null) && (suit != null)) {
            this.cardString = rank.getString() + suit.getString();
            this.cardValue = rank.getRankValue();
        }
    }


    //true if a rank wildcard ; false if not (or null)
    //Jokers handled in subclass
    boolean isWildCard(final Rank wildCardRank){
        return ((wildCardRank != null) && (rank == wildCardRank));
    }

    int getRankValue() {
        if (null == rank) throw new RuntimeException("getRankValue: called with rank==null");
        return rank.getRankValue();
    }

    final boolean isSameRank(final Card card) { return this.rank == card.rank; }
    final boolean isSameRank(final Rank rank) { return this.rank == rank;}

    final boolean isSameSuit(final Suit suit) {return this.suit == suit;}

    //TODO:C use compare method instead
    final int getRankDifference(final Card card) {return this.getRankValue() - card.getRankValue();}

    final int getRankDifference(final Rank rank) {return this.getRankValue() - rank.getRankValue();}

    /* GETTERS and SETTERS */
    boolean isJoker() {
        return false;
    }

    final String getCardString() {return this.cardString;}

    final Rank getRank() {
        return this.rank;
    }
    final Suit getSuit() {
        return this.suit;
    }

    int getCardValue(final Rank wildCardRank) {
        if (this.isWildCard(wildCardRank)) return WILD_CARD_VALUE;
        else return cardValue;
    }


}
