package com.example.jeffrey.fivekings.util;

import android.view.animation.AlphaAnimation;

/**
 * Created by Jeffrey on 3/4/2015.
 */
public class Utilities {
    //Animation that fades the cards instantly
    public final static AlphaAnimation instantFade(float from, float to) {
        AlphaAnimation alphaFade = new AlphaAnimation(from, to);
        alphaFade.setDuration(0); //instant
        alphaFade.setFillAfter(true); //persist after animation ends
        return alphaFade;
    }
}
