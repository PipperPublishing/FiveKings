package com.example.jeffrey.fivekings;

import android.content.Context;
import android.widget.ImageView;

/**
 * Created by Jeffrey on 2/18/2015.
 * References the Card that this ImageView shows so that we can know which one was clicked
 */
class CardView extends ImageView {
    private final Card card;
    private final int viewIndex; //index in layout

    CardView(Context c, Card card, int viewIndex) {
        super(c);
        this.card = card;
        this.viewIndex = viewIndex;

    }

    Card getCard() {
        return card;
    }

    int getViewIndex() {
        return viewIndex;
    }


}
