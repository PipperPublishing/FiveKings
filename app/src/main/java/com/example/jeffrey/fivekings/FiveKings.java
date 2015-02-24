package com.example.jeffrey.fivekings;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

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
    2/16/2015   Changed LayeredDrawables to FrameLayouts so they can be clicked
    2/17/2015   Fixed end-of-round button to be Show scores. Last button now says [New Game] and calls Game constructor
    2/17/2015   turnInfo line now uses parameterized string resource
    2/17/2015   Converted StringBuffer to StringBuilder throughout
    2/21/2015   Creating basic elements of Draw-and-Drop listener (for discarding)
    2/23/2015   Trigger drag on simple click of a card
                Set DiscardPile clickable or not (disabled means you can't drop on it)
 */

public class FiveKings extends Activity {
    static final float CARD_OFFSET =25.0f;
    static final float ADDITIONAL_MELD_OFFSET = 20.0f;
    static final float CARD_WIDTH = 50.0f;


    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = false;

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
    private TextView mLastRoundScores;
    private TextView mCurrentRound;
    private TableLayout mScoreDetail;
    private View mScoreDetailView;
    private TextView mGameInfoLine;
    private TableRow[] mScoreDetailRow = new TableRow[Game.MAX_PLAYERS];
    private TextView[] mPlayerNametv = new TextView[Game.MAX_PLAYERS];
    private TextView[] mPlayerScoretv = new TextView[Game.MAX_PLAYERS];
    private TextView[] mPlayerCumScoretv = new TextView[Game.MAX_PLAYERS];
    private CheckBox[] mPlayerIsHuman = new CheckBox[Game.MAX_PLAYERS];
    private TextView[] mPlayerIndex = new TextView[Game.MAX_PLAYERS];
    private TextView mInfoLine;
    private ImageButton mDiscardButton;
    private ImageButton mDrawPileButton;
    private RelativeLayout mCurrentCards;
    private RelativeLayout mCurrentMelds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_five_kings);


        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);
/*
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
*/
/*
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
        });*/

        //set up the playGameClicked handler for the Button
        //set the OnClickListener for the button - for some reason this doesn't reliably work from XML
        mPlayButton = (Button)findViewById(R.id.Play);
/*
        //stops UI from hiding during button access
        mPlayButton.setOnTouchListener(mDelayHideTouchListener);
*/

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                playGameClicked(v);
            }
        });
        mPlayButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                //TODO:B Use string resource here
                Toast.makeText(getApplicationContext(), "Starting new game", Toast.LENGTH_SHORT).show();
                mPlayButton.setText(getText(R.string.newGame));
                mGame.setGameState(GameState.NEW_GAME);
                playGameClicked(v);
                return true;
            }
        });

        final ImageButton drawPileButton = (ImageButton)findViewById(R.id.drawPile);
        drawPileButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clickedDrawPile(v);
            }
        });

        final ImageButton discardPileButton = (ImageButton)findViewById(R.id.discardPile);
        discardPileButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clickedDiscardPile(v);
            }
        });

        //Listen for clicks on the melds or cards
    }//end onCreate

/*
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    *//*
*/
/**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     *//*
*/
/*
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

    */
/**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     *//*

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
*/

 /*   EVENT HANDLERS*/
    //Event handler for [Save] in add player dialog
 public void addEditPlayerClicked(String playerName, boolean isHuman, boolean addingFlag, int iPlayer) {
     if (addingFlag) mGame.addPlayer(playerName, isHuman);
     else mGame.updatePlayer(playerName, isHuman, iPlayer);
     showPlayerScores(mGame.getPlayers());
 }

    //the Event handler for button presses on [Play]
    private void playGameClicked(View v){
        enablePlayDisableDrawDiscard(true);
        if (null== mGame) {
            mGame = new Game(this); //also sets gameState=ROUND_START
            mLastRoundScores = (TextView)findViewById(R.id.after_round);
            mCurrentRound = (TextView)findViewById(R.id.current_round);
            mGameInfoLine = (TextView) findViewById(R.id.game_info_line);
            mGameInfoLine.setTypeface(null, Typeface.BOLD_ITALIC);
            mInfoLine = (TextView) findViewById(R.id.info_line);
            mInfoLine.setTypeface(null, Typeface.ITALIC);
            mDiscardButton = (ImageButton)findViewById(R.id.discardPile);
            mDiscardButton.setClickable(false);
            mDrawPileButton = (ImageButton)findViewById(R.id.drawPile);
            mDrawPileButton.setEnabled(false);
            mCurrentMelds = (RelativeLayout) findViewById(R.id.current_melds);
            mCurrentCards = (RelativeLayout) findViewById(R.id.current_cards);
            this.mCurrentRound.setText(resFormat(R.string.current_round,mGame.getRoundOf().getString()));
            this.mPlayButton.setText(resFormat(R.string.nextRound, mGame.getRoundOf().getString()));
            showPlayerScores(mGame.getPlayers());

            showAddPlayers();
        }

        //User pressed [New Game] button
        else if (GameState.NEW_GAME == mGame.getGameState()) {
            mGame.init();
            mGameInfoLine.setText(getText(R.string.blank));
            mInfoLine.setText(getText(R.string.blank));
            this.mPlayButton.setText(resFormat(R.string.nextRound, mGame.getRoundOf().getString()));
            mCurrentCards.removeAllViews();
            mCurrentMelds.removeAllViews();
            showPlayerScores(mGame.getPlayers());
        }

        //starting a round
        else if (GameState.ROUND_START == mGame.getGameState()) {
            mGame.initRound();
            this.mCurrentRound.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
            mGameInfoLine.setText(String.format(getText(R.string.deals).toString(), mGame.getDealer().getName()));
            mInfoLine.setText(getText(R.string.blank));
            this.mPlayButton.setText(resFormat(R.string.nextPlayer, mGame.getPlayer().getName()));
            mCurrentCards.removeAllViews();
            mCurrentMelds.removeAllViews();
            syncDiscardMeldsCards(null);
        }

        else if (GameState.TAKE_COMPUTER_TURN == mGame.getGameState()) {
            StringBuilder turnInfo = new StringBuilder(100);
            Player playerWentOut=null;

            turnInfo.setLength(0);
            playerWentOut = mGame.takeAutoTurn(getText(R.string.computerTurnInfo).toString(), turnInfo);

            if (playerWentOut != null) mGameInfoLine.setText(String.format(getText(R.string.wentOut).toString(),playerWentOut.getName()));
            syncDiscardMeldsCards(turnInfo);
            //returns false if we've reached the player who went out again
            if (mGame.endTurn()) this.mPlayButton.setText(resFormat(R.string.nextPlayer, mGame.getPlayer().getName()));
            else this.mPlayButton.setText(getText(R.string.showScores));
        }

        else if (GameState.TURN_START == mGame.getGameState()) {
            syncDiscardMeldsCards(null);

            if (mGame.getPlayer().isHuman()) {
                mInfoLine.setText(resFormat(R.string.yourCardsAndMelds, mGame.getPlayer().getName()));
                enablePlayDisableDrawDiscard(false);
                mGame.setGameState(GameState.TAKE_HUMAN_TURN);
            }
            else {
                mInfoLine.setText(resFormat(R.string.computerCardsAndMelds, mGame.getPlayer().getName()));
                this.mPlayButton.setText(resFormat(R.string.takePlayerTurn, mGame.getPlayer().getName()));
                mGame.setGameState(GameState.TAKE_COMPUTER_TURN);
            }
        }//end TURN_START

        else if (GameState.ROUND_END == mGame.getGameState()) {
            mLastRoundScores.setText(resFormat(R.string.scores_after, mGame.getRoundOf().getString()));
            mGame.endRound();
            mGameInfoLine.setText(getText(R.string.displayScores));
            showPlayerScores(mGame.getPlayers());
            if (GameState.ROUND_START == mGame.getGameState()) this.mPlayButton.setText(resFormat(R.string.nextRound,mGame.getRoundOf().getString()));
        }

        else if (GameState.GAME_END == mGame.getGameState()) {
            this.mPlayButton.setText(getText(R.string.newGame));
            mGame.logFinalScores();
            mGame.setGameState(GameState.NEW_GAME);
        }
    }

    //Event handler for clicks on Discard Pile
    private void clickedDiscardPile(View v) {
        drawOrDiscard(Game.USE_DISCARD_PILE);
    }

    //Event handler for clicks on Draw Pile
    private void clickedDrawPile(View v) {
        drawOrDiscard(Game.USE_DRAW_PILE);
    }

    private void drawOrDiscard(boolean useDiscardPile) {
        StringBuilder turnInfo = new StringBuilder(100);

        turnInfo.setLength(0);
        mGame.takeHumanTurn(getText(R.string.humanTurnInfo).toString(), turnInfo, useDiscardPile); //also sets GameState to END_HUMAN_TURN
        enablePlayDisableDrawDiscard(false, false); //disable drawing again from the Discard Pile
        syncDiscardMeldsCards(turnInfo);
    }


//Event Handler for clicks on cards or melds - called from DiscardPile drag listener
    boolean draggedCard(int iCardView) {
        CardView foundCardView=null;
        //find the view we coded with this index
        for (int iView = 0; iView < mCurrentCards.getChildCount(); iView++) {
            CardView cv = (CardView) mCurrentCards.getChildAt(iView);
            if (cv.getViewIndex() == iCardView) {
                foundCardView=cv;
                break;
            }
        }
        if (foundCardView == null) //less likely, but search melded cards too
            for (int iView = 0; iView < mCurrentMelds.getChildCount(); iView++) {
                CardView cv = (CardView) mCurrentMelds.getChildAt(iView);
                if (cv.getViewIndex() == iCardView) {
                    foundCardView=cv;
                    break;
                }
            }
        if (foundCardView == null) {
            Toast.makeText(this, "No matching card found", Toast.LENGTH_SHORT).show();
            Log.e(mGame.APP_TAG, "draggedCard: No matching card found");
            return false;
        }
        else Toast.makeText(this, foundCardView.getCard().getCardString() + " discarded", Toast.LENGTH_SHORT).show();

        if (GameState.END_HUMAN_TURN == mGame.getGameState()) {
            if (!mGame.getPlayer().isHuman()) throw new RuntimeException("draggedCard: player is not Human");
            Player playerWentOut = mGame.endHumanTurn(foundCardView.getCard());
            if (playerWentOut != null) mGameInfoLine.setText(String.format(getText(R.string.wentOut).toString(),playerWentOut.getName()));
            syncDiscardMeldsCards(null);

            //returns false if we've reached the player who went out again
            if (mGame.endTurn()) mPlayButton.setText(resFormat(R.string.nextPlayer, mGame.getPlayer().getName()));
            else mPlayButton.setText(getText(R.string.showScores));

            enablePlayDisableDrawDiscard(true);
        }
        return true;
    }

    /* COMMON METHODS FOR MANAGING UI ELEMENTS */
    //TODO:A: Also enable/disable dragging of discard when not human on discard step
    private void enablePlayDisableDrawDiscard(boolean enable) {
        if (this.mPlayButton != null) {
            this.mPlayButton.setEnabled(enable);
            this.mPlayButton.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        }
        if (this.mDrawPileButton != null) this.mDrawPileButton.setEnabled(!enable);
        if (this.mDiscardButton != null) this.mDiscardButton.setClickable(!enable);
    }
    private void enablePlayDisableDrawDiscard(boolean enablePlay, boolean enablePiles) {
        enablePlayDisableDrawDiscard(enablePlay);
        if (this.mDrawPileButton != null) this.mDrawPileButton.setEnabled(enablePiles);
        if (this.mDiscardButton != null) this.mDiscardButton.setClickable(enablePiles);
    }

    private void syncDiscardMeldsCards(StringBuilder turnInfo) {
        //Show card on discard pile (changes because of this player's play)
        mDiscardButton.setImageDrawable(mGame.getDiscardPileDrawable());
        mDiscardButton.setOnDragListener(new ImageButtonDragEventListener() );

        int iBase = showCards(mGame.getPlayer().getHandMelded(), mCurrentMelds,0);
        showCards(mGame.getPlayer().getHandUnMelded(), mCurrentCards, iBase);
        mInfoLine.setText(turnInfo);
    }

    //open Add Player dialog
    private void showAddPlayers() {
        //pop open the Players dialog for the names
        EnterPlayersDialogFragment epdf = EnterPlayersDialogFragment.newInstance(null, false, true, -1);
        epdf.show(getFragmentManager(),null);
    }

    private void showEditPlayer(String oldPlayerName, boolean oldIsHuman, int iPlayer) {
        EnterPlayersDialogFragment epdf = EnterPlayersDialogFragment.newInstance(oldPlayerName, oldIsHuman, false, iPlayer);
        epdf.show(getFragmentManager(),null);
    }

    //show melds and unmelded as ImageViews
    private int showCards(ArrayList<CardList> meldLists, RelativeLayout relativeLayout, int iViewBase) {
        ArrayList<CardView> cardLayers = new ArrayList<>(Game.MAX_CARDS);
        float xOffset=0f;
        int iView=iViewBase;
        relativeLayout.removeAllViews();
        for (CardList cardlist : meldLists) {
            for (Card card : cardlist.getCards()) {
                CardView cv = new CardView(this, card, iView );
                cv.setTag(iView); //allows us to pass the index into the dragData without dealing with messy object passing
                iView++;
                cv.setImageDrawable(card.getDrawable());
                cv.setTranslationX(xOffset);
                cv.bringToFront();
                cv.setClickable(false);
                cv.setOnDragListener(new CardViewDragEventListener() );
                cv.setOnLongClickListener(new View.OnLongClickListener() {
                    // Defines the one method for the interface, which is called when the View is long-clicked
                    public boolean onLongClick(View v) {
                        // Create a new ClipData using the tag as a label, the plain text MIME type, and
                        // the string containing the View index. This will create a new ClipDescription object within the
                        // ClipData, and set its MIME type entry to "text/plain"
                        ClipData dragData = ClipData.newPlainText("Discard", v.getTag().toString());
                        View.DragShadowBuilder myShadow = new CardViewDragShadowBuilder(v);

                        // Starts the drag
                        v.startDrag(dragData,  // the data to be dragged
                                myShadow,  // the drag shadow builder
                                null,      // no need to use local data
                                0          // flags (not currently used, set to 0)
                        );
                        return true;
                    }//end onClick
                });

                cardLayers.add(cv);
                xOffset += CARD_OFFSET;
            }
            xOffset += ADDITIONAL_MELD_OFFSET;
        }
        //bit of a hack: we now adjust everything by - 1/2*(width of stacked image) to center it
        // add CARD_WIDTH because the total width of the stacked image is sum(offsets) + CARD_WIDTH
        // subtract CARD_OFFSET and ADDITIONAL_MELD_OFFSET because we added these at the end of the loop (but they're not in the layout)
        xOffset = xOffset - CARD_OFFSET - ADDITIONAL_MELD_OFFSET + CARD_WIDTH;
        if (!cardLayers.isEmpty()) {
            for (CardView cv : cardLayers) {
                cv.setTranslationX(cv.getTranslationX()-0.5f*xOffset);
                relativeLayout.addView(cv);
            }
        }
        return iView; //used as the starting index for mCurrentMelds
    }//end showCards


    //TODO:A Look at switching to ListView, especially since all Table Rows are the same
    //Also would allow us to get rid of this hacky behavior of storing hidden fields
    //since the ListView would contain the data we needed
    private void showPlayerScores(List<Player> players) {

        if (null == mScoreDetail) {
            mScoreDetail = (TableLayout) findViewById(R.id.scoreDetail);
            LayoutInflater  inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //provides the layout params for each row
            mScoreDetailView = inflater.inflate(R.layout.score_detail,null);
            //actual rows are added dynamically below (allows for adding new players)
        }//end if mScoreDetail not initialized

        List<Player> sortedPlayers = new ArrayList<Player>(players);
        Collections.sort(sortedPlayers, Player.playerComparatorByScoreDesc);
        for (int iPlayer=0; iPlayer<sortedPlayers.size(); iPlayer++) {
            //dynamically add this row, including the initial hard-coded players
            if (null == mScoreDetailRow[iPlayer]) {
                mScoreDetailRow[iPlayer] = new TableRow(this);
                mPlayerNametv[iPlayer] = clone((TextView) mScoreDetailView.findViewById(R.id.player_name));
                mPlayerNametv[iPlayer].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View tv) {
                        //third argument says we are editing
                        //really ugly...
                        showEditPlayer(((TextView) tv).getText().toString(),
                                ((CheckBox) ((ViewGroup) tv.getParent()).getChildAt(3)).isChecked(),
                                Integer.parseInt(((TextView) ((ViewGroup) tv.getParent()).getChildAt(4)).getText().toString()));
                    }
                });

                mPlayerScoretv[iPlayer] = clone((TextView) mScoreDetailView.findViewById(R.id.round_score));
                mPlayerCumScoretv[iPlayer] = clone((TextView) mScoreDetailView.findViewById(R.id.cum_score));
                //invisible checkbox to remember whether this player is human or not
                mPlayerIsHuman[iPlayer] = new CheckBox(this);
                mPlayerIsHuman[iPlayer].setVisibility(View.GONE);
                //invisible array selector
                mPlayerIndex[iPlayer] = new TextView(this);
                mPlayerIndex[iPlayer].setVisibility(View.GONE);

                //add the detail row view into the table layout
                mScoreDetail.addView(mScoreDetailRow[iPlayer]);
                mScoreDetailRow[iPlayer].addView(mPlayerNametv[iPlayer], 0);
                mScoreDetailRow[iPlayer].addView(mPlayerScoretv[iPlayer], 1);
                mScoreDetailRow[iPlayer].addView(mPlayerCumScoretv[iPlayer], 2);
                mScoreDetailRow[iPlayer].addView(mPlayerIsHuman[iPlayer], 3);//index 3 is important for this invisible checkbox
                mScoreDetailRow[iPlayer].addView(mPlayerIndex[iPlayer],4);
            }
            mPlayerNametv[iPlayer].setText(String.valueOf(sortedPlayers.get(iPlayer).getName()));
            mPlayerScoretv[iPlayer].setText(String.valueOf(sortedPlayers.get(iPlayer).getRoundScore()));
            mPlayerCumScoretv[iPlayer].setText(String.valueOf(sortedPlayers.get(iPlayer).getCumulativeScore()));
            mPlayerIsHuman[iPlayer].setChecked(sortedPlayers.get(iPlayer).isHuman());
            mPlayerIndex[iPlayer].setText(String.valueOf(iPlayer));
        }//end for players
        //Bold the top (leading) player
        mPlayerNametv[0].setTypeface(null, Typeface.BOLD);
        mPlayerCumScoretv[0].setTypeface(null, Typeface.BOLD);
    }//end showPlayerScores

    /* SMALL UTILITY METHODS */
    private TextView clone(TextView vFrom) {
        final TextView vTo = new TextView(this);
        cloneLayout(vFrom, vTo);
        return vTo;
    }

    static private void cloneLayout(TextView vFrom, TextView vTo) {
        vTo.setLayoutParams(vFrom.getLayoutParams());
        vTo.setPadding(vFrom.getPaddingLeft(), vFrom.getPaddingTop(), vFrom.getPaddingRight(), vFrom.getPaddingBottom());
    }

    private String resFormat(int resource, String param) {
        return String.format(getText(resource).toString(),param);
    }

}
