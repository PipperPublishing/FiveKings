package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 1/22/2015.
 * 2/2/2015 Made member variables final (initialized once only)
 * 2/3/2015: Pulled out Wild Card values into Card
 */
//TODO:C Better design would pull Jokers up into Card, then eliminate JOKER rank
//This is ugly because there is a simple 5 x Rank of cards, but then there are six Jokers
enum Rank {
    THREE(3,"3"),FOUR(4,"4"),FIVE(5,"5"),SIX(6,"6"),SEVEN(7,"7"),EIGHT(8,"8"),NINE(9,"9"),
    TEN(10,"10"),JACK(11,"J"),QUEEN(12,"Q"),KING(13,"K"),JOKER(50,"Jok");

    private final int rankValue;
    private final String rankString;


    Rank(int rankValue, String rankString){
        this.rankValue = rankValue;
        this.rankString = rankString;
    }
    Rank getNext() {
        if (this == JOKER) return null; //should never get called in this way
        else return values()[this.ordinal()+1];
    }
    int getRankValue() {
        return rankValue;
    }
    String getRankString() {
        return rankString;
    }
}
