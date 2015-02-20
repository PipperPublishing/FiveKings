package com.example.jeffrey.fivekings;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.util.Comparator;

/**
 * Created by Jeffrey on 1/22/2015.
 * 2/2/2015 Changed cards to be final - no setters because you cannot change values
 * 2/3/2015 Added null suit constructor (for Jokers)
 * 2/4/2015 Moved Joker handling up here and out of Rank enum
 * 2/14/2015 Added bmp's for cards; resources are hard-coded to card rank and suit
 * 2/19/2015    Removed bitmap
 */
//TODO:B Intermediate and final scoring should be moved out of Card probably
//TODO:B Create a subclass for Jokers which doesn't have rank and suit
class Card {
    //can put these into resource files
    static final int INTERMEDIATE_WILD_CARD_VALUE=1;
    static final int FINAL_WILD_CARD_VALUE =20;
    static final int FINAL_JOKER_VALUE=50;
    static final String JOKER_STRING="Joker";

    //static array of mapping from cards to resource IDs
    //For now, stars are blue diamonds
    //array of [Suits][Ranks]
    static final int[][] sBitmapResource = {
            {R.drawable.s3, R.drawable.s4, R.drawable.s5, R.drawable.s6, R.drawable.s7, R.drawable.s8, R.drawable.s9, R.drawable.s10, R.drawable.sj, R.drawable.sq, R.drawable.sk},
            {R.drawable.h3, R.drawable.h4, R.drawable.h5, R.drawable.h6, R.drawable.h7, R.drawable.h8, R.drawable.h9, R.drawable.h10, R.drawable.hj, R.drawable.hq, R.drawable.hk},
            {R.drawable.d3, R.drawable.d4, R.drawable.d5, R.drawable.d6, R.drawable.d7, R.drawable.d8, R.drawable.d9, R.drawable.d10, R.drawable.dj, R.drawable.dq, R.drawable.dk},
            {R.drawable.c3, R.drawable.c4, R.drawable.c5, R.drawable.c6, R.drawable.c7, R.drawable.c8, R.drawable.c9, R.drawable.c10, R.drawable.cj, R.drawable.cq, R.drawable.ck},
            {R.drawable.st3, R.drawable.st4, R.drawable.st5, R.drawable.st6, R.drawable.st7, R.drawable.st8, R.drawable.st9, R.drawable.st10, R.drawable.stj, R.drawable.stq, R.drawable.stk}
    };

    //sorting for sequences (within the same suit)
    static final Comparator<Card> cardComparatorSuitFirst = new Comparator<Card>() {
        @Override
        public int compare(Card card1, Card card2) {
            if (card1.isJoker && card2.isJoker) return 0;
            if (card1.isJoker) return card2.getRankValue();
            if (card2.isJoker) return -card1.getRankValue();
            int suitCmp = card1.suit.compareTo(card2.suit);
            if (suitCmp != 0) return suitCmp;
            return card1.rank.compareTo(card2.rank);
        }
    };
    //sorting for rank melds (so we meld higher value cards first)
    static final Comparator<Card> cardComparatorRankFirstDesc = new Comparator<Card>() {
        @Override
        public int compare(Card card1, Card card2) {
            if (card1.isJoker && card2.isJoker) return 0;
            if (card1.isJoker) return card2.getRankValue();
            if (card2.isJoker) return -card1.getRankValue();
            int rankCmp = -card1.rank.compareTo(card2.rank);
            if (rankCmp != 0) return rankCmp;
            return card1.suit.compareTo(card2.suit);
        }
    };

    static final Comparator<Card> cardComparatorRankFirstAsc = new Comparator<Card>() {
        @Override
        public int compare(Card card1, Card card2) {
            if (card1.isJoker && card2.isJoker) return 0;
            if (card1.isJoker) return -card2.getRankValue();
            if (card2.isJoker) return card1.getRankValue();
            int rankCmp = card1.rank.compareTo(card2.rank);
            if (rankCmp != 0) return rankCmp;
            return card1.suit.compareTo(card2.suit);
        }
    };

    private final Suit suit;
    private final Rank rank;
    private final String cardString;
    private final int cardValue;
    private final boolean isJoker;
    private final Drawable drawable;

    Card(Suit suit, Rank rank, Context context) {
        this.suit = suit;
        this.rank = rank;
        this.cardString = rank.getRankString() + suit.getSuitString();
        this.cardValue = rank.getRankValue();
        this.isJoker = false;
        this.drawable = context.getResources().getDrawable(sBitmapResource[suit.getOrdinal()][this.getRank().getOrdinal()]);
    }

    //no arguments constructor is for Jokers
    Card(Context context) {
        this.isJoker = true;
        this.suit = null;
        this.rank = null;
        this.cardString = JOKER_STRING;
        this.cardValue = FINAL_JOKER_VALUE;
        this.drawable = context.getResources().getDrawable(R.drawable.joker1);
    }

    //true if a rank wildcard or a Joker; false if not (or null)
    boolean isWildCard(Rank wildCardRank){
        return ((wildCardRank != null) && (isJoker || (rank == wildCardRank)));
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

    //should not get called with Jokers
    int getRankValue() {
        if (null == rank) throw new RuntimeException("getRankValue: called with rank==null");
        return rank.getRankValue();
    }

    String getCardString() {return this.cardString;}

    boolean isSameRank(Card card) { return this.rank == card.rank; }
    boolean isSameRank(Rank rank) { return this.rank == rank;}

    boolean isSameSuit(Card card) {return this.suit == card.suit;}
    boolean isSameSuit(Suit suit) {return this.suit == suit;}

    //TODO:C use compare method instead
    int getRankDifference(Card card) {return this.getRankValue() - card.getRankValue();}

    int getRankDifference(Rank rank) {return this.getRankValue() - rank.getRankValue();}


    Drawable getDrawable() {
        return drawable;
    }


    Rank getRank() {
        return this.rank;
    }
    Suit getSuit() {
        return this.suit;
    }
}
