package com.example.jeffrey.fivekings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.jeffrey.fivekings.util.SystemUiHider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
/* HISTORY
    2/10/2015   Display each round every time Play button is pressed
    2/12/2015   Display each player's hands and top of Discard Pile
    2/15/2015   Added GameState to provide State after each button press
 */
//TODO:B Embed spaces in string resources

public class FiveKings extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;
    // Dynamic elements of the interface
    private Game mGame=null;
    private Button mPlayButton;
    private TextView mRoundNumber;
    private TableLayout mScoreDetail;
    private View mScoreDetailView;
    private TextView mGameInfoLine;
    private TableRow[] mScoreDetailRow = new TableRow[Game.MAX_PLAYERS];
    private TextView[] mPlayerNametv = new TextView[Game.MAX_PLAYERS];
    private TextView[] mPlayerScoretv = new TextView[Game.MAX_PLAYERS];
    private TextView[] mPlayerCumScoretv = new TextView[Game.MAX_PLAYERS];
    private TextView mInfoLine;
    private ImageButton mDiscardButton;
    private ImageButton mDrawPileButton;
    private ImageView mCurrentMeldsImage;
    private ImageView mCurrentCardsImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_five_kings);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        mPlayButton= (Button)findViewById(R.id.Play);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.Play).setOnTouchListener(mDelayHideTouchListener);

        //set up the playGame handler for the Button
        //set the OnClickListener for the button - for some reason this doesn't reliably work from XML
        final Button playButton = (Button)findViewById(R.id.Play);
        playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                playGame(v);
            }
        });

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    //the Event handler for button presses on [Play]
    void playGame(View v){
        Rank nextRound;
        Player playerWentOut=null;
        StringBuffer turnInfo = new StringBuffer(100);

        //Beginning of game
        //need to allow for mGame!=null but GameState reset to NEW_GAME (rerun)
        if (null== mGame) {
            mGame = new Game(this);
            mRoundNumber = (TextView)findViewById(R.id.round_number);
            mGameInfoLine = (TextView) findViewById(R.id.game_info_line);
            mGameInfoLine.setTypeface(null, Typeface.BOLD_ITALIC);
            mInfoLine = (TextView) findViewById(R.id.info_line);
            mInfoLine.setTypeface(null, Typeface.ITALIC);
            mDiscardButton = (ImageButton)findViewById(R.id.discardPile);
            mDrawPileButton = (ImageButton)findViewById(R.id.drawPile);
            mCurrentMeldsImage = (ImageView) findViewById(R.id.current_melds);
            mCurrentCardsImage = (ImageView) findViewById(R.id.current_cards);
            mPlayButton= (Button)findViewById(R.id.Play);
            mPlayButton.setText(getText(R.string.nextRound)+" "+mGame.getRoundOf().getRankString()+"'s");
            showPlayerScores(mGame.getPlayers());
        }

        //starting a round
        else if (GameState.ROUND_START == mGame.getGameState()) {
            mGame.initRound();
            mDiscardButton.setImageDrawable(mGame.getDiscardPileDrawable());
            mGameInfoLine.setText(getText(R.string.now_playing) + " " + mGame.getRoundOf().getRankString() + "'s");
            mInfoLine.setText(mGame.getDealer().getName() + " " + getText(R.string.deals));
            mPlayButton.setText(getText(R.string.nextPlayer) + " " + mGame.getPlayer().getName());
            mCurrentCardsImage.setImageDrawable(null);
            mCurrentMeldsImage.setImageDrawable(null);
        }

        else if (GameState.TAKE_TURN == mGame.getGameState()) {
            playerWentOut = mGame.takeTurn(turnInfo);
            if (playerWentOut != null) mGameInfoLine.setText(playerWentOut.getName() + " " + getText(R.string.wentOut));
            //Show card on discard pile (changes because of this player's play)
            mDiscardButton.setImageDrawable(mGame.getDiscardPileDrawable());
            showCards();
            mInfoLine.setText(turnInfo);
            if (mGame.endTurn()) mPlayButton.setText(getText(R.string.nextPlayer) + " " + mGame.getPlayer().getName());
        }

        else if (GameState.TURN_START == mGame.getGameState()) {
            //Show card on discard pile (need to display at beginning of round)
            mDiscardButton.setImageDrawable(mGame.getDiscardPileDrawable());
            showCards();

            mInfoLine.setText(mGame.getPlayer().getName()+"'s " + getText(R.string.cards));
            mPlayButton.setText(getText(R.string.takeTurn) + " " + mGame.getPlayer().getName());
            mGame.setGameState(GameState.TAKE_TURN);
        }

        else if (GameState.ROUND_END == mGame.getGameState()) {
            mRoundNumber.setText(getText(R.string.scores_after) +" " + mGame.getRoundOf().getRankString() + "'s");
            nextRound = mGame.endRound();
            mGameInfoLine.setText(getText(R.string.displayScores));
            showPlayerScores(mGame.getPlayers());
            if (null == nextRound) mPlayButton.setText(getText(R.string.Exit));
            else
                mPlayButton.setText(getText(R.string.nextRound) + " " + mGame.getRoundOf().getRankString() + "'s");
        }

        else if (GameState.GAME_END == mGame.getGameState()) {
            mGame.showFinalScores();
        }
    }

    //show melds and unmelded as StackedDrawables
    //TODO:A left most cards in stack are looking very stretched
    //TODO:A put a space between each meld
    private boolean showCards() {
        ArrayList<Drawable> meldedLayers = new ArrayList<>(Game.MAX_CARDS);
        for (CardList cardlist : mGame.getPlayer().getHandMelded()) {
            for (Card card : cardlist.getCards()) meldedLayers.add(card.getDrawable());
        }
        if (!meldedLayers.isEmpty()) {
            Drawable[] layers = new Drawable[meldedLayers.size()];
            for (int iDrawable = 0; iDrawable < meldedLayers.size(); iDrawable++) {
                layers[iDrawable] = meldedLayers.get(iDrawable);
            }
            LayerDrawable meldsDrawable = new LayerDrawable(layers);
            for (int iLayer = 0; iLayer < meldsDrawable.getNumberOfLayers(); iLayer++) {
                meldsDrawable.setLayerInset(iLayer, iLayer * Game.CARD_STACK_OFFSET, 0, 0, 0);
            }
            mCurrentMeldsImage.setImageDrawable(meldsDrawable);
        } else {//clear it
            mCurrentMeldsImage.setImageDrawable(null);
        }

        ArrayList<Drawable> unMeldedLayers = new ArrayList<>(Game.MAX_CARDS);
        for (CardList cardlist : mGame.getPlayer().getHandUnMelded()) {
            for (Card card : cardlist.getCards()) unMeldedLayers.add(card.getDrawable());
        }
        if (!unMeldedLayers.isEmpty()) {
            Drawable[] layers = new Drawable[unMeldedLayers.size()];
            for (int iDrawable = 0; iDrawable < unMeldedLayers.size(); iDrawable++) {
                layers[iDrawable] = unMeldedLayers.get(iDrawable);
            }
            LayerDrawable unMeldedDrawable = new LayerDrawable(layers);
            for (int iLayer = 0; iLayer < unMeldedDrawable.getNumberOfLayers(); iLayer++) {
                unMeldedDrawable.setLayerInset(iLayer, iLayer * Game.CARD_STACK_OFFSET, 0, 0, 0);
            }
            mCurrentCardsImage.setImageDrawable(unMeldedDrawable);
        } else {//clear it
            mCurrentCardsImage.setImageDrawable(null);
        }

        return true;
    }//end showCards


    //TODO:B Look at switching to ListView, especially since all Table Rows are the same
    void showPlayerScores(List<Player> players) {

        if (null == mScoreDetail) {
            mScoreDetail = (TableLayout) findViewById(R.id.scoreDetail);
            LayoutInflater  inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //provides the layout params for each row
            mScoreDetailView = inflater.inflate(R.layout.score_detail,null);
            TableRow mScoreDetailRowBase = (TableRow) mScoreDetailView.findViewById(R.id.score_detail_row);
            TextView mPlayerNametvBase = (TextView) mScoreDetailView.findViewById(R.id.player_name);
            TextView mPlayerScoretvBase = (TextView) mScoreDetailView.findViewById(R.id.round_score);
            TextView mPlayerCumScoretvBase = (TextView) mScoreDetailView.findViewById(R.id.cum_score);
            //set up the names and scores - then update each round
            for (int iPlayer=0; iPlayer<players.size(); iPlayer++) {
                mScoreDetailRow[iPlayer] = new TableRow(this);
                mPlayerNametv[iPlayer] = clone(mPlayerNametvBase);
                mPlayerScoretv[iPlayer] = clone(mPlayerScoretvBase);
                mPlayerCumScoretv[iPlayer] = clone(mPlayerCumScoretvBase);

                //add the detail row view into the table layout
                mScoreDetail.addView(mScoreDetailRow[iPlayer]);
                //add the element unless this is the default (template) row
                mScoreDetailRow[iPlayer].addView(mPlayerNametv[iPlayer], 0);
                mScoreDetailRow[iPlayer].addView(mPlayerScoretv[iPlayer], 1);
                mScoreDetailRow[iPlayer].addView(mPlayerCumScoretv[iPlayer], 2);
            }
        }//end if mScoreDetail not initialized

        List<Player> sortedPlayers = new ArrayList<Player>(players);
        Collections.sort(sortedPlayers, Player.playerComparatorByScoreDesc);
        for (int iPlayer=0; iPlayer<sortedPlayers.size(); iPlayer++) {
            mPlayerNametv[iPlayer].setText(String.valueOf(sortedPlayers.get(iPlayer).getName()));
            mPlayerScoretv[iPlayer].setText(String.valueOf(sortedPlayers.get(iPlayer).getRoundScore()));
            mPlayerCumScoretv[iPlayer].setText(String.valueOf(sortedPlayers.get(iPlayer).getCumulativeScore()));
        }//end for players
        mPlayerNametv[0].setTypeface(null, Typeface.BOLD);
        mPlayerCumScoretv[0].setTypeface(null, Typeface.BOLD);
    }//end showPlayerScores

    private TextView clone(TextView vFrom) {
        final TextView vTo = new TextView(this);
        cloneLayout(vFrom,vTo);
        return vTo;
    }



    static private void cloneLayout(TextView vFrom, TextView vTo) {
        vTo.setLayoutParams(vFrom.getLayoutParams());
        vTo.setPadding(vFrom.getPaddingLeft(),vFrom.getPaddingTop(),vFrom.getPaddingRight(),vFrom.getPaddingBottom());
    }
}
