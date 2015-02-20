package com.example.jeffrey.fivekings;

import android.content.Context;
import android.widget.ImageView;

/**
 * Created by Jeffrey on 2/18/2015.
 * References the Card that this ImageView shows so that we can know which one was clicked
 */
public class CardView extends ImageView {
    private final Card card;

    CardView(Context c, Card card) {
        super(c);
        this.card = card;
    }

    Card getCard() {
        return card;
    }
}
