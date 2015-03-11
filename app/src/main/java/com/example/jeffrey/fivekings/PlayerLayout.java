package com.example.jeffrey.fivekings;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Jeffrey on 3/8/2015.
 * 3/8/2015     Holds views for card back, name, round score, cum score
 *              problem of identical id's?
 * 3/9/2015     Solved clipping problem by adding playerLayouts to fullscreen_content
 *              Dealing now pads out to borders of Draw and Discard Pile
 * 3/10/2015    Call from Player to update border and scores
 *              onClickListener on name (or anywhere in PlayerLayout) lets you edit name
 * 3/11/2015    Reverse order of dealt hands so it goes clockwise

 */
class PlayerLayout extends RelativeLayout{
    //in API 17+ we could use View.generateViewId
    private static final int NAME_VIEW_ID=1001;
    private static final int CUMULATIVE_SCORE_VIEW_ID=1002;
    private static final int ROUND_SCORE_VIEW_ID=1003;
    private static final int CARD_VIEW_ID=1004;

    static final float DECK_SCALING = 0.75f;
    static final float CARD_SCALING = 0.5f; //reduced size of drawpile + 1/2 size of placed card
    private static final float X_PADDING = 50f;
    private static final float Y_PADDING = 0f;

    private final int iPlayer;
    //include here the views that we want to update easily
    private final CardView cardView;
    private final TextView roundScoreView;
    private final TextView cumScoreView;
    private final TextView nameView;

    //Constructor - also lays out the Player layout
    PlayerLayout(final Context c, final String playerName, final int iPlayer, final int numPlayers) {
        super(c);
        this.iPlayer = iPlayer;

        final RelativeLayout.LayoutParams handLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        handLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        handLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        this.setLayoutParams(handLp);

        double angle = 180 * iPlayer/(numPlayers-1);
        float xMargin = CardView.INTRINSIC_WIDTH*(DECK_SCALING + CARD_SCALING/2)+ X_PADDING;
        float yMargin = CardView.INTRINSIC_HEIGHT*(DECK_SCALING + CARD_SCALING/2) + Y_PADDING;
        float TranslationX;
        float TranslationY;
        //Adjust to clear the edges of the Drawpile and DiscardPile
        if (angle < 60) {
            TranslationX = xMargin;
            TranslationY = (float) (TranslationX * Math.tan(Math.toRadians(angle)));
        }
        else if (angle > 120) {
            TranslationX = -xMargin;
            TranslationY = (float) (TranslationX * Math.tan(Math.toRadians(angle)));
        }
        else {
            TranslationY = yMargin;
            TranslationX = (float) (TranslationY / Math.tan(Math.toRadians(angle)));
        }
        this.setTranslationX(TranslationX);
        this.setTranslationY(TranslationY);

        this.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View tv) {
                FiveKings fkActivity = (FiveKings)tv.getContext();
                Player player = fkActivity.getmGame().getPlayer(iPlayer);
                fkActivity.showEditPlayer(player.getName(), player.isHuman(),iPlayer);
            }
        });


        this.cardView = new CardView(c, CardView.sBitmapCardBack);
        LayoutParams cardLp = new LayoutParams((int)(CardView.INTRINSIC_WIDTH*0.5f), (int)(CardView.INTRINSIC_HEIGHT*0.5f));
        cardLp.addRule(ALIGN_PARENT_TOP);
        cardLp.addRule(CENTER_HORIZONTAL);
        cardView.setLayoutParams(cardLp);
        cardView.setId(CARD_VIEW_ID);
        this.setGreyedOut(true); //until the first card is dealt
        this.addView(cardView);

        this.nameView = new TextView(getContext());
        nameView.setId(NAME_VIEW_ID);
        nameView.setTextAppearance(getContext(), R.style.TextAppearance_AppCompat_Small);
        LayoutParams nameLp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameLp.addRule(BELOW, CARD_VIEW_ID);
        nameLp.addRule(CENTER_HORIZONTAL);
        nameView.setLayoutParams(nameLp);
        this.addView(nameView);
        nameView.setTextColor(getResources().getColor(android.R.color.white));
        nameView.setText(playerName);

        this.cumScoreView = new TextView(c);
        cumScoreView.setId(CUMULATIVE_SCORE_VIEW_ID);
        cumScoreView.setTextAppearance(c, R.style.TextAppearance_AppCompat_Small);
        cumScoreView.setTextColor(getResources().getColor(android.R.color.white));
        LayoutParams cumScoreLp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cumScoreLp.setMargins(0,0,0,10);
        cumScoreLp.addRule(ALIGN_BOTTOM, CARD_VIEW_ID);
        cumScoreLp.addRule(CENTER_HORIZONTAL);
        cumScoreView.setLayoutParams(cumScoreLp);
        this.addView(cumScoreView);
        cumScoreView.setVisibility(INVISIBLE); //to start
        updateCumulativeScore(0);

        this.roundScoreView = new TextView(c);
        roundScoreView.setId(ROUND_SCORE_VIEW_ID);
        roundScoreView.setTextAppearance(c, R.style.TextAppearance_AppCompat_Small);
        roundScoreView.setTypeface(null, Typeface.ITALIC);
        roundScoreView.setTextColor(getResources().getColor(android.R.color.white));
        LayoutParams roundScoreLp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        roundScoreLp.setMargins(0, 10, 0, 0);
        roundScoreLp.addRule(CENTER_HORIZONTAL);
        roundScoreLp.addRule(ALIGN_TOP, CARD_VIEW_ID);
        roundScoreView.setLayoutParams(roundScoreLp);
        this.addView(roundScoreView);
        this.showRoundScore(0,false);
    }

    boolean updateCumulativeScore(int cumulativeScore) {
        cumScoreView.setText(Integer.toString(cumulativeScore));
        cumScoreView.setVisibility(VISIBLE);
        return true;
    }

    //not shown until the end of the round
    boolean showRoundScore(int roundScore, boolean isVisible) {
        roundScoreView.setText("+"+Integer.toString(roundScore));
        roundScoreView.setVisibility(isVisible ? VISIBLE : INVISIBLE);
        return true;
    }

    void updateName(String newName) {
        nameView.setText(newName);
    }

    //grey out Card because we're looking at it
    void setGreyedOut(boolean set) {
        if (set) this.cardView.setAlpha(0.1f);
        else this.cardView.setAlpha(1.0f);
    }


    //outline with dashed border, or solid if you're out
    void showAsCurrent() {
        this.cardView.setBackgroundDrawable(getResources().getDrawable(R.drawable.nopad_dashed_border));
        this.roundScoreView.setTextColor(getResources().getColor(android.R.color.white));
        this.nameView.setTextColor(getResources().getColor(android.R.color.white));
    }
    void showAsOut() {
        this.cardView.setBackgroundDrawable(getResources().getDrawable(R.drawable.no_pad_solid_green_border));
        this.roundScoreView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        this.nameView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
    }
    void resetToPlain() {
        this.cardView.setBackgroundDrawable(null);
        this.roundScoreView.setTextColor(getResources().getColor(android.R.color.white));
        this.nameView.setTextColor(getResources().getColor(android.R.color.white));
    }

    /* GETTERS and SETTERS */

    TextView getRoundScoreView() {
        return roundScoreView;
    }

    CardView getCardView() {
        return cardView;
    }
}
