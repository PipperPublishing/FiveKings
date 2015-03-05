package com.example.jeffrey.fivekings;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Jeffrey on 2/18/2015.
 * References the Card that this ImageView shows so that we can know which one was clicked
 * 2/26/2015    Move drawable for Card to CardView so we don't need context
 * 3/3/2015     Set INTRINSIC_WIDTH and INTRINSIC_HEIGHT as soon as we start building CardViews
 * 3/4/2015     card can be null in which case we set null ImageDrawable as well
 */
class CardView extends ImageView {
    private Card card; //TODO:A would like to make this final again
    private final int viewIndex; //index in layout

    static final int sBitmapCardBack = R.drawable.b2fv;
    static int INTRINSIC_WIDTH = -1;
    static int INTRINSIC_HEIGHT = -1;

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

    CardView(Context c, Card card, int viewIndex) {
        super(c);
        checkSetIntrinsic(c);
        this.viewIndex = viewIndex;
        this.card = card;
        if (card == null) this.setImageDrawable(null);
        else if (card.isJoker()) setImageDrawable(c.getResources().getDrawable(R.drawable.joker1));
        else setImageDrawable(c.getResources().getDrawable(sBitmapResource[card.getSuit().getOrdinal()][card.getRank().getOrdinal()]));
    }

    //for card back and any other non-face-card which doesn't need viewIndex or card
    CardView(Context c, int resource) {
        super(c);
        checkSetIntrinsic(c);
        this.card = null;
        setImageDrawable(c.getResources().getDrawable(resource));
        this.viewIndex = -1;
    }

    public CardView(Context c, AttributeSet attributeSet) {
        super(c, attributeSet);
        checkSetIntrinsic(c);
        this.card = null;
        this.viewIndex = -1;
    }

    //Copy constructor
    CardView(Context c, CardView cv) {
        this(c, cv.card, -1);
    }

    void checkSetIntrinsic(Context c) {
        if(INTRINSIC_WIDTH==-1)
        {
            INTRINSIC_WIDTH = c.getResources().getDrawable(R.drawable.joker1).getIntrinsicWidth();
            INTRINSIC_HEIGHT = c.getResources().getDrawable(R.drawable.joker1).getIntrinsicHeight();
        }
    }

    //TODO:B this is a hack because we are changing that card
    void setCard(Context c, Card card) {
        this.card = card;

        if (card == null) this.setImageDrawable(null);
        else if (card.isJoker()) setImageDrawable(c.getResources().getDrawable(R.drawable.joker1));
        else setImageDrawable(c.getResources().getDrawable(sBitmapResource[card.getSuit().getOrdinal()][card.getRank().getOrdinal()]));
    }

    Card getCard() {
        return card;
    }

    int getViewIndex() {
        return viewIndex;
    }


}
