/*
 * Copyright Jeffrey Pugh (pipper.publishing@gmail.com) (c) 2015. All rights reserved.
 */

package com.pipperpublishing.fivekings.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pipperpublishing.fivekings.Player;
import com.pipperpublishing.fivekings.R;

/**
 * Created by Jeffrey on 3/8/2015.
 * 3/8/2015     Holds views for card back, name, round score, cum score
 *              problem of identical id's?
 * 3/9/2015     Solved clipping problem by adding playerLayouts to fullscreen_content
 *              Dealing now pads out to borders of Draw and Discard Pile
 * 3/10/2015    Call from Player to update border and scores
 *              onClickListener on name (or anywhere in PlayerLayout) lets you edit name
 * 3/11/2015    Reverse order of dealt hands so it goes clockwise
 * 3/11/2015    Add playedInFinalTurn flag - turned on after we score this hand
 *              Turn score red if it's "big"
 * 3/11/2015    v0.3.02 Add Player card to left (should be aligned to border instead of being translated)
 * 3/25/2015    Change onClick to play player and onLongClick to edit
 * 3/26/2015    Record which player linked this PlayerLayout. Not sure this two-way linkage is correct
 *              but it makes calling player methods much less clunky
 * 3/31/2015    Change name to PlayerMiniHandLayout
 *              Added yellow or red border to high-scoring hands
 *              On first click, show cards - on second play
 * 4/4/2015     If !showCards, then don't force a second click
 * 4/6/2015     Removed updateCumulativeScore from update, so it can be updated after the roundScore animation completes
    6/4/2015    Scale mini hands and draw/discard pile so that they fit in top 1/3 of screen
   6/9/2015     Scale mini hands based on actual size of DiscardPile
 9/1/2015       Save Custom View state as per http://stackoverflow.com/questions/3542333/how-to-prevent-custom-views-from-losing-state-across-screen-orientation-change
 9/21/2015      Not currently using Current View state save./restore (recreating from scratch)
 10/8/2015      Change miniHand layout to move them all to a separate miniHand strip - hopefully this will also make it easier to do a slide-out drawer
                (so no Y translation any more)
                Use Horizontal Linear Layout with equal weights
 10/10/2015     Store and update the player that this miniHand refers to (otherwise when we update a player, the miniHand is
                left pointing to the old player)
 10/17/2015     Remove special two-touches for human-to-human; instead just auto-hide current player
 */
public class PlayerMiniHandLayout extends RelativeLayout{
    //in API 17+ we could use View.generateViewId
    private static final int NAME_VIEW_ID=1001;
    private static final int CUMULATIVE_SCORE_VIEW_ID=1002;
    private static final int ROUND_SCORE_VIEW_ID=1003;
    private static final int CARD_VIEW_ID=1004;

    private static final float MINI_HAND_SCALING = 0.6f; //size of min-hand relative to DrawPile
    static final float SPACER_WIDTH = 10f;
    private static final float Y_PADDING = 30f;
    private static final int RED_SCORE = 40;
    private static final int YELLOW_SCORE = 20;

    //include here the views that we want to update easily
    private Player player;  //the reverse connection to the player so we know who is clicked
    private final CardView cardView;
    private final TextView roundScoreView;
    private final TextView cumScoreView;
    private final TextView nameView;

    private boolean playedInFinalTurn;

    //Constructor - also lays out the Player layout
    public PlayerMiniHandLayout(final Context c, final Player initialPlayer, final int iPlayer, final int numPlayers) {
        super(c);
        final FiveKings fKActivity = (FiveKings)c;
        final LinearLayout.LayoutParams handLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        handLp.weight = 1;
        handLp.gravity = CENTER_VERTICAL;
        this.setLayoutParams(handLp);

        if (numPlayers <= 1) throw new RuntimeException("You must have at least two players");

        this.player = initialPlayer;

        /*
        double angle = 180 * iPlayer/(numPlayers-1);
        //Translate mini-hand center to clear DiscardPile/DrawPile + allowance for Spacer and some extra
        float xMargin = fKActivity.getDrawPileWidth()+ 0.5f * MINI_HAND_SCALING* fKActivity.getDrawPileWidth() + 2*SPACER_WIDTH;
        float yMargin = fKActivity.getDrawPileHeight() + Y_PADDING;
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
        */

        this.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View tv) {
                ((FiveKings) c).showEditPlayer(PlayerMiniHandLayout.this.player.getName(), PlayerMiniHandLayout.this.player.isHuman(), iPlayer);
                return true;
            }
        });

        this.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View tv) {
                //if this is the next player, and the current player has finished its turn, move to this player
                if ((PlayerMiniHandLayout.this.player == fKActivity.getmGame().getNextPlayer()) && (fKActivity.getmGame().getCurrentPlayer().getTurnState() == Player.TurnState.NOT_MY_TURN)) {
                    fKActivity.getmGame().rotatePlayer(); //also sets PREPARE_TURN
                    if (player.getTurnState() == Player.TurnState.PREPARE_TURN) player.prepareTurn(fKActivity);
                    //prepareTurn calls takeTurn if it's computer player and we're not showing cards
                }
                //second time we click on the current player, check that we're ready to play (PLAY_TURN)
                else if ((player == fKActivity.getmGame().getCurrentPlayer()) && (player.getTurnState() == Player.TurnState.PLAY_TURN)) {
                    cardView.clearAnimation();
                    player.takeTurn(fKActivity, null, fKActivity.getmGame().isFinalTurn());
                }
                else fKActivity.setShowHint(null, FiveKings.HandleHint.SHOW_HINT , true);
            }
        });


        this.cardView = new CardView(c, CardView.sBlueBitmapCardBack);
        LayoutParams cardLp = new LayoutParams((int)(fKActivity.getDrawPileWidth()* MINI_HAND_SCALING), (int)(fKActivity.getDrawPileHeight()* MINI_HAND_SCALING));
        cardLp.addRule(ALIGN_PARENT_TOP);
        cardLp.addRule(CENTER_HORIZONTAL);
        cardView.setLayoutParams(cardLp);
        cardView.setId(CARD_VIEW_ID);
        this.addView(cardView);

        this.nameView = new TextView(c);
        nameView.setId(NAME_VIEW_ID);
        nameView.setTextAppearance(c, R.style.TextAppearance_AppCompat_Small);
        LayoutParams nameLp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameLp.addRule(BELOW, CARD_VIEW_ID);
        nameLp.addRule(CENTER_HORIZONTAL);
        nameView.setLayoutParams(nameLp);
        this.addView(nameView);
        nameView.setTextColor(getResources().getColor(android.R.color.white));
        nameView.setText(player.getName());

        this.cumScoreView = new TextView(c);
        cumScoreView.setId(CUMULATIVE_SCORE_VIEW_ID);
        cumScoreView.setTextAppearance(c, R.style.TextAppearance_AppCompat_Small);
        cumScoreView.setTextColor(getResources().getColor(android.R.color.white));
        LayoutParams cumScoreLp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cumScoreLp.setMargins(0, 0, 0, 10);
        cumScoreLp.addRule(ALIGN_BOTTOM, CARD_VIEW_ID);
        cumScoreLp.addRule(CENTER_HORIZONTAL);
        cumScoreView.setLayoutParams(cumScoreLp);
        this.addView(cumScoreView);
        cumScoreView.setVisibility(INVISIBLE); //to start
        updateCumulativeScore(player.getCumulativeScore());

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

        reset(); //greys out and resets border
    }//end constructor for player mini-hands

    //constructor for [+] hand that when clicked gives you addPlayer dialog
    @Deprecated
    PlayerMiniHandLayout(final Context c) {
        super(c);
        final RelativeLayout.LayoutParams handLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        handLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        handLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        this.setLayoutParams(handLp);

        this.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View tv) {
                FiveKings fkActivity = (FiveKings)c;
                fkActivity.showAddPlayers();
            }
        });

        this.cardView = new CardView(c, CardView.sBlueBitmapCardBack);
        LayoutParams cardLp = new LayoutParams((int)(CardView.INTRINSIC_WIDTH* MINI_HAND_SCALING), (int)(CardView.INTRINSIC_HEIGHT* MINI_HAND_SCALING));
        cardLp.addRule(ALIGN_PARENT_TOP);
        cardLp.addRule(CENTER_HORIZONTAL);
        cardView.setLayoutParams(cardLp);
        cardView.setId(CARD_VIEW_ID);
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
        nameView.setTypeface(null, Typeface.ITALIC);
        nameView.setText("Add");

        roundScoreView=null;
        cumScoreView=null;

        reset();
    }

    /* UTILITIES FOR UPDATING SCORES ETC. */
    final public void reset() {
        //set the border depending on the player state
        resetToPlain();
        setPlayedInFinalTurn(false);
        setGreyedOut(true);
    }


    final public void update(final boolean isCurrent, final boolean isOut, final int roundScore, final int cumulativeScore) {
        showOrHideCurrentBorder(isCurrent);
        //playedInFinalTurn flag is set when you play in final round, and controls green border and round Score display
        //without this check, hands would show green border after being dealt a melded hand
        updateRoundScore(roundScore, isOut && playedInFinalTurn);
        // <0 means we're waiting for roundScore animation to update cumulativeScore
        if (cumulativeScore >= 0) updateCumulativeScore(cumulativeScore);
        invalidate();
    }


    final boolean updateCumulativeScore(final int cumulativeScore) {
        cumScoreView.setText(Integer.toString(cumulativeScore));
        cumScoreView.setVisibility(VISIBLE);
        return true;
    }

    //not shown until the final turns - set border to yellow or red
    private boolean updateRoundScore(final int roundScore, boolean showAsOut) {
        roundScoreView.setText("+"+Integer.toString(roundScore));
        if (showAsOut) {
            this.cardView.setBackgroundDrawable(getResources().getDrawable(R.drawable.no_pad_solid_green_border));
            this.roundScoreView.setTextColor(Color.GREEN);
            this.nameView.setTextColor(Color.GREEN);
        } else {
            // leave the border alone (also shows who is the current player)
            this.roundScoreView.setTextColor(getResources().getColor(android.R.color.white));
            this.nameView.setTextColor(getResources().getColor(android.R.color.white));
        }

        if (roundScore >= YELLOW_SCORE) {
            roundScoreView.setTextColor(Color.YELLOW);
            this.cardView.setBackgroundDrawable(getResources().getDrawable(R.drawable.no_pad_solid_yellow_border));
        }
        if (roundScore >= RED_SCORE) {
            roundScoreView.setTextColor(Color.RED);
            this.cardView.setBackgroundDrawable(getResources().getDrawable(R.drawable.no_pad_solid_red_border));
        }
        roundScoreView.setVisibility(this.playedInFinalTurn ? VISIBLE : INVISIBLE);
        return true;
    }
    public void setPlayedInFinalTurn(boolean set) {
        this.playedInFinalTurn = set;
    }

    final public void updateName(final String newName) {
        nameView.setText(newName);
    }

    final public void updatePlayer(final Player updatedPlayer) {
        this.player = updatedPlayer;
    }

    //grey out Card because we're looking at it
    final public void setGreyedOut(final boolean set) {
        if (set) this.cardView.setAlpha(FiveKings.ALMOST_TRANSPARENT_ALPHA);
        else this.cardView.setAlpha(1.0f);
    }


    //outline with white border, or solid if you're out
    private void showOrHideCurrentBorder(final boolean showCurrentBorder) {
        this.cardView.setBackgroundDrawable(showCurrentBorder ? getResources().getDrawable(R.drawable.nopad_white_border) : null);
    }

    private void resetToPlain() {
        this.cardView.setBackgroundDrawable(null);
        this.roundScoreView.setTextColor(getResources().getColor(android.R.color.white));
        this.nameView.setTextColor(getResources().getColor(android.R.color.white));
    }

    //animation of the mini Hand
    public void startAnimateMiniHand(final Animation animation) {
        cardView.startAnimation(animation);
    }
    public void clearAnimatedMiniHand() {
        cardView.clearAnimation();
    }

    /* GETTERS and SETTERS */
    TextView getRoundScoreView() {
        return roundScoreView;
    }

    public CardView getCardView() {
        return cardView;
    }

    /* Override onSaveInstanceState and extend BaseSavedState */

    @Override
    protected Parcelable onSaveInstanceState() {
        //Allows parent classes to save state
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        // And add the fields we want to save
        ss.cardView = this.cardView;
        ss.roundScoreView = this.roundScoreView;
        ss.cumScoreView = this.cumScoreView;
        ss.nameView = this.nameView;
        ss.playedInFinalTurn = this.playedInFinalTurn;

        return ss;
    }

    static class SavedState extends BaseSavedState {
        private  CardView cardView;
        private  TextView roundScoreView;
        private  TextView cumScoreView;
        private  TextView nameView;

        private boolean playedInFinalTurn;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.cardView = (CardView) in.readValue(CardView.class.getClassLoader());
            this.roundScoreView = (TextView) in.readValue(TextView.class.getClassLoader());
            this.cumScoreView = (TextView) in.readValue(TextView.class.getClassLoader());
            this.nameView = (TextView) in.readValue(TextView.class.getClassLoader());
            this.playedInFinalTurn = in.readByte() != 0x00;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(cardView);
            out.writeValue(roundScoreView);
            out.writeValue(cumScoreView);
            out.writeValue(nameView);
            out.writeByte((byte) (playedInFinalTurn ? 0x01 : 0x00));
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

    }
}
