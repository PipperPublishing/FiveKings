package com.example.jeffrey.fivekings;

import java.util.ArrayList;

/**
 * Created by Jeffrey on 1/24/2015.
 * 2/5/2015 No longer used (intermediate results are stored in Hand member variables)
 */
//Hack to contain evaluation result and card to discard
class EvaluationWrapper {
    private Card cardToDiscard;
    private ArrayList<CardList> bestMelds = new ArrayList<>();
    private ArrayList<CardList> bestUnMelded = new ArrayList<>();
    private int score;

    EvaluationWrapper() {
        this.cardToDiscard = null;
        this.bestMelds = new ArrayList<>();
        this.bestUnMelded = new ArrayList<>();
        this.score = 0;
    }

    //this is a shallow copy with all the dangers of that
    EvaluationWrapper(EvaluationWrapper wrapper) {
        this.cardToDiscard = wrapper.cardToDiscard;
        this.bestMelds = wrapper.bestMelds;
        this.bestUnMelded = wrapper.bestUnMelded;
        this.score = wrapper.score;
    }

    void copy(EvaluationWrapper wrapper) {
        this.cardToDiscard = wrapper.cardToDiscard;
        this.bestMelds = wrapper.bestMelds;
        this.bestUnMelded = wrapper.bestUnMelded;
        this.score = wrapper.score;
    }

    //v2 uses separated lists of melds
    void setBestMelds(ArrayList<CardList> bestMelds) {
        this.bestMelds = new ArrayList<>(bestMelds);
    }
    //v1 uses one list of melds
    void setBestMelds(CardList bestMelds) {
        this.bestMelds.clear();
        this.bestMelds.add(bestMelds);
    }

    void setBestUnMelded(ArrayList<CardList> bestUnMelded) {
        this.bestUnMelded = new ArrayList<>(bestUnMelded);
    }
    void setBestUnMelded(CardList bestUnMelded) {
        this.bestUnMelded.clear();
        this.bestUnMelded.add(bestUnMelded);
    }

    ArrayList<CardList> getBestMelds() {
        return bestMelds;
    }

    ArrayList<CardList> getBestUnMelded() {
        return bestUnMelded;
    }

    Card getCardToDiscard() {
        return cardToDiscard;
    }

    void setCardToDiscard(Card cardToDiscard) {
        this.cardToDiscard = cardToDiscard;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
