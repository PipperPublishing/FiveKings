
/*
 * Copyright Jeffrey Pugh (pipper.publishing@gmail.com) (c) 2015. All rights reserved.
 */

package com.pipperpublishing.fivekings.view;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.pipperpublishing.fivekings.BuildConfig;
import com.pipperpublishing.fivekings.Card;
import com.pipperpublishing.fivekings.CardList;
import com.pipperpublishing.fivekings.Deck;
import com.pipperpublishing.fivekings.Game;
import com.pipperpublishing.fivekings.GameState;
import com.pipperpublishing.fivekings.HumanPlayer;
import com.pipperpublishing.fivekings.Meld;
import com.pipperpublishing.fivekings.Player;
import com.pipperpublishing.fivekings.PlayerList;
import com.pipperpublishing.fivekings.R;
import com.pipperpublishing.fivekings.Rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    2/26/2015   Change GameInfo and other on-screen text to Toasts
                Don't show computer cards except in final round after turn
                Removed Toast/display for current round (have lots of hints in UI already)
                Moved UI element initialization into onCreate
                Create nested melds (RelativeLayouts) inside mCurrent Melds - findViewByIndex now has an extra loop
    3/1/2015    Use onTouch to start Drag rather than OnLongClick
    3/3/2015    Put dashed line border around melds; use invisible background to preset correct width
                Note that Layouts wrap content around child views *without* Translation (Translation does not adjust parent view width)
    3/3/2015    Set CARD_OFFSET_RATIO to be 20% of INTRINSIC_WIDTH (preset in CardView)
    3/3/2015    Vibrate Discard and Draw Piles to show you are meant to draw - replace InfoLine with Toast on first few rounds
    3/3/2015    Animate pick from Draw or Discard Piles
                Comment out infoLine - add back in information as needed
                Enable/Disable Draw and Discard Piles just sets clickable or not
    3/5/2015    Improved animation to show cards picked for humans and faulty animation for Computer
    3/6/2015    Switch to using Animator for Computer Discard
    3/6/2015    First code for saving instance State when app gets Stopped - NOT WORKING
                On LongClick of Play button, pop up a confirmation dialog before starting a new game
    3/6/2015    0.2.10:
                Disable Play Button during Computer Animation so you can't click it twice
                Reset Scores header correctly to blank
                First version of dealing animation at the beginning of each round
                For now we eliminate it, but next step would be to label each hand and then scale it to 100% on each hand
    3/6/2015    Add dealt cards to piles section and label with name and scores
    3/7/2015    Create Relative Layout for Player Hands, including card back, name, round score, and cumulative score
    3/10/2015   Save PlayerLayout in Player class
                Fixed dealing animation by moving duration to individual objectAnimator steps
                Computer Draw animation goes from pile to mini-hand
                Convert animateHuman... to grey out PlayerHand while you look at it
    3/11/2015   Dealt hands were starting off too small (base 75% card size is now considered 1.0 for scaleX/Y)
                Don't show Computer card backs
    3/11/2015   Don't show roundScore until final round when you've had your final play
    3/11/2015   v0.3.02: Add Delete player option in Edit dialog
                Add a transparent card with a plus on it next to the final player
    3/12/2015   Impact of subclassing of Player->HumanPlayer and ComputerPlayer
    3/22/2015   Impact of encapsulating all player management in PlayerList
    3/31/2015   Removed mCurrentRound (showing it on the moved Play button)
                Moved clickedDrawOrDiscard to Game
                Relayout miniHands on add/delete of players
                Moved common code to endTurnCheckRound
                Replaced syncDisplay with updateHandsAndCards
    4/1/2015    Removed syncDisplay and ROUND_END
    4/2/2015    First cut at Settings menu
    4/3/2015    Settings: showComputerHands and animateDealing
                Move Game creation to onCreate; could also make it singleton
    4/5/2015    Start threaded findBestHand as soon as previous player (or round start) is done.
    4/6/2015    Implement more menu items, including Player, and remove Add Player on screen
    4/6/2015    Display hints if you click in the wrong place
    4/7/2015    Delete Deprecated methods
                explodeHands shows winner's hand spiraling out
    4/8/2015    explodeHand shows deck spiraling out randomly from deal position
    4/9/2015    Added enable/disableDragToDiscardPile to prevent drag and drop before you've picked
                Return AnimatorSet from animateDealing so you can cancel it with Play button
                Change showCards to disable dragging after Final Turn
    6/3/2015    showCards: Compute height of cards as 50% of RelativeLayout
    6/6/2015    Added CARD_SCALING (the ratio of the card height to that of the Meld Layout so that the cards can be arranged +/- 50% offset)
    6/11/2015   Change from DECK_SCALING to use actual scaled height of deck (that is dynamically controlled by the LinearLayout it is placed in)
    6/17/2015   Try adding Discard and DrawPile programmatically to solve weird translation problem
                Use setAdjustViewBounds and position relative to spacer
    8/25/2015   Read/set settings for ShowComputerHands and AnimateDealing
    8/26/2015   Show Help unless ShowHelp setting false; also have a checkbox in the dialog?
    8/29/2015   Implement onSave/onRestore Instance State to recreate correct layout AND correct round etc.
    8/30/2015   Eliminate showIntro and get it directly from settings
    9/2/2015    Get scaledCardHeight (assumed to be the same for mCurrentCards and mCurrentMelds) when window first is focused
                Add flag needToRefreshCardsMelds to ensure relayout on Orientation change etc.
                Add flag isAfterDealing to indicate whether we should show a card or not
                Redo animateHumanPickup and animateRoundScore to use actual positions on screen
    9/3/2015    We *should * override onSave/onRestore for each custom View to bring it back correctly (and handling layout changes there)
                Instead, we're refreshing the dynamic elements which makes this fragile
    9/17/2015   Record whether mini-hands were animated and recreate that way.
                Bug: If you change orientation when Human is playing, and Draw/Discard are jiggling, it will switch to jiggling the Computer hand
    9/21/2015   Separate routine for alpha animation of mini hands if you don't do dealing animation
                Move set/cancel miniHand animation to Game and other calls

    9/30/2015   Second attempt to handle restart using GameState and TurnState
    10/1/2015   Animate piles when appropriate
    10/2/2015   Make hand animation, pile animation, and solid cards dependent on GameState and TurnState
    10/3/2015      Moved Meld-and-discard hint out of disableDrawDiscardClick into calling method
    10/6/2015   Bug: Press [New Game] should cancel the card explosion animation and show the winning score
                Make explodeSet a class variable so that we can cancel it on New Game
                Cancel animation removes added cards
                Add human_from_discardpile.xml and human_from_drawpile to scale picked up cards to 75% rather than 0%
    10/8/2015   Moved miniHands to their own strip, so dealing and pickup/discard needed adjustment - use same technique as with animate roundScore
                animate roundScore from top of currentMelds instead of middle
                Animate dealing from Draw Pile already showing on screen
    10/9/2015   Don't show hints if showIntroduction is turned off
                Moved Intro and Player menu items into Action Bar if room
    10/10/2015  Centered ProgressBar (circle) underneath [Start] button
    10/12/2015  Add a dashed border to the Discard Pile
                Set discardPile to null during startRound so that blank shows during dealing
                Change getText..toString to getString calls on resource IDs
                Show card we pick up from DrawPile
    10/13/2015  Moved "toStartGameHint" after congrats dialog (was getting covered by explode animation?)
    10/15/2015  showCards was not offsetting the meld by xCardOffset
                Reduce X_MELD_OFFSET_RATIO to 0.1
                0.7.20: Animate card to discardPile at the end of dealing
    10/16/2015  Remove showDiscardPileCard to only the places it gets changed (after dealing, and discards)
                Add setShowHint to combine setmHint and showHint...
    10/17/2015  Automatically hide hand if Human to Human
    10/18/2015  More granular control of updateHandsAndCards on restart
                Change per player updateHandsAndCards to returning a showCards flag
                Added playButton set on TURN_END (for example, if you change orientation during dealing)
 10/20/2015     Moved clickedDrawOrDiscard here from Game
                Moved showComputerCards and animateDealing into FiveKings, use directly from preferences
                Change parameters of updateHandsAndCards to showCards and allowDragging
 11/4/2015      Set Drag here for a new meld to White explicitly (looked grey)
 11/6/2015      Set and pass highlightWildCardRank if NOVICE_MODE set (also save in savedInstanceState)
                Don't highlight in animateComputerPickupAndDiscard
 11/8/2015      Highlight wildcards if separate highlightWildcards option set
 11/9/2015      Parcelable implementation: cascade properly through sub/super classes
                Move View-type classes to their own package
                Parcelable: whereever we have an object reference to Card or Player, we need to
                NOT create a new Card/Player but get the reference to the list of Cards (Deck) or Players
                This is at least true of melds, partialMelds, and singles which are copies of Hand cards

                Replace stored deck with references to the singleton
 11/10/2015     Change "click" to "tap" and include round # in hint
                Clear game play area if you select [Yes] in New Game option, or finish old game
                Also Game.init() if you select [Yes]
                This also resets roundOf so you can add players
                clearAnimation before making piles invisible (seems to interfere otherwise)
 11/11/2015     Try using ShowcaseView to highlight portions of the UI
                Remove showing Intro box when you start game
 11/13/2015     Don't show hints while you're showing Tutorial screens
*/
public class FiveKings extends Activity {
    public static final String APP_TAG = BuildConfig.VERSION_NAME;

    static final String SETTINGS_NAME="SettingsFile";
    static final String SHOW_COMPUTER_HANDS_SETTING="showComputerHands";
    static final String ANIMATE_DEALING_SETTING="animateDealing";
    static final String NOVICE_MODE ="showIntro";
    static final String HIGHLIGHT_WILDCARDS="highlightWildcards";

    static int showingTutorial = 0;

    static final float CARD_OFFSET_RATIO = 0.2f;
    static final float CARD_SCALING = 0.6f; //in the meld area, the cards are 1.5 x CARD_SCALING + a margin for the border, to fit in the meld area
    static final float X_MELD_OFFSET_RATIO = 0.1f;
    static final float Y_MELD_OFFSET_RATIO = 0.25f; //each meld is +/-25% of card height from the center-line
    //static final int TOAST_X_OFFSET = 20;
    //static final int TOAST_Y_OFFSET = +600;
    static final Rank HELP_ROUNDS=Rank.FOUR;
    static final float ALMOST_TRANSPARENT_ALPHA=0.1f;
    static final float HALF_TRANSPARENT_ALPHA=0.5f;
    static final float THIRD_TRANSPARENT_ALPHA=0.3f;
    static final float INVISIBLE_ALPHA=0.0f;

    private static final int ANIMATION_2S =2000;
    private static final int ANIMATION_1S = 1000;
    private static final int ANIMATION_500MS =500;
    private static final int ANIMATION_100MS =100;

    private static final int DISCARDPILE_VIEW_ID=1000;
    private static final int DRAWPILE_VIEW_ID=1001;

    public enum HandleHint {SHOW_HINT, SET_HINT, SET_AND_SHOW_HINT}

    // Dynamic elements of the interface
    //TODO:A could probably merge Game up into FiveKings to simplify things
    private Game mGame=null;
    private float scaledCardHeight;
    private boolean needToRefreshCardsMelds =true;

    private ProgressBar spinner;
    private Button mPlayButton;
    private TableLayout mScoreDetail;
    private View mScoreDetailView;
    private Toast mHintToast;
    private String mHint;
    private TableRow[] mScoreDetailRow = new TableRow[Game.MAX_PLAYERS];
    private TextView[] mPlayerNametv = new TextView[Game.MAX_PLAYERS];
    private TextView[] mPlayerScoretv = new TextView[Game.MAX_PLAYERS];
    private TextView[] mPlayerCumScoretv = new TextView[Game.MAX_PLAYERS];
    private CheckBox[] mPlayerIsHuman = new CheckBox[Game.MAX_PLAYERS];
    private TextView[] mPlayerIndex = new TextView[Game.MAX_PLAYERS];
    private CardView mDiscardPile;
    private CardView mDrawPile;
    private RelativeLayout mCurrentCards;
    private RelativeLayout mCurrentMelds;
    private AnimatorSet dealAnimatorSet;
    private AnimatorSet explodeSet;

    private Animation shakeAnimation;
    private Rank highlightWildCardRank = null;
    private ShowcaseView lastShowcaseView=null;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    //Note that we merge onRestoreSavedInstanceState in here because of logic related to restoring Game state
    protected void onCreate(Bundle savedInstanceState) {
        //auto-magically recreates static elements (defined in the XML)
        super.onCreate(savedInstanceState);

        //invalidated by a re-layout
        needToRefreshCardsMelds = true;

        //This section does not differ whether it's a new instance or a resumed one
        setContentView(R.layout.activity_five_kings);

        //Initialize other dynamic elements of the UI (e.g. handlers) or get references for later manipulation

        //set up the playGameClicked handler for the Button
        //set the OnClickListener for the button - for some reason this doesn't reliably work from XML
        mPlayButton = (Button)findViewById(R.id.Play);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                playGameClicked();
            }
        });
        //If you longClick, can start a new Game but pops up a confirmation dialog
        mPlayButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                checkNewGame(); //pop up check dialog
                return true;
            }
        });

        mCurrentMelds = (RelativeLayout) findViewById(R.id.current_melds);
        mCurrentMelds.setOnDragListener(new CurrentMeldsLayoutDragListener());
        mCurrentCards = (RelativeLayout) findViewById(R.id.current_cards);

        spinner = (ProgressBar)findViewById(R.id.progressCircle);
        spinner.setVisibility(View.GONE);

        shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.hand_click_me);

        // This section differs whether this is being newly CREATED (Bundle is null) or RESUMED
        if (savedInstanceState == null) {
            dealAnimatorSet = null;
            mGame = new Game(this);
            layoutDrawAndDiscardPiles(); //needs deck created in Game
            showHideDrawAndDiscardPiles(false);
            mHint = getString(R.string.toStartGameHint);
            mHintToast = Toast.makeText(this,mHint, Toast.LENGTH_SHORT);
            mHintToast.show();        }
        else {
            Log.i(FiveKings.APP_TAG, "onCreate: Restoring State from Parcel");
            mGame = savedInstanceState.getParcelable("mGame");
            mHint = savedInstanceState.getString("mHint", getString(R.string.toStartGameHint));
            mPlayButton.setText(savedInstanceState.getString("playButtonText", (String) mPlayButton.getText()));
            highlightWildCardRank = savedInstanceState.getParcelable("highlightWildCardRank");
            mHintToast = Toast.makeText(this,mHint, Toast.LENGTH_SHORT);
            //Show/Hide state of Draw and Discard pile is whatever it was when saved
            layoutDrawAndDiscardPiles();
        }

        disableDrawDiscardClick();

        //Note we cannot actually refresh the card display here because the Views have not been measured yet
        // and so getHeight returns 0 (which we use for dynamic scaling of card heights)
    }//end onCreate

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // saves the View hierarchy state
        /* We *should * override onSave/onRestore for each custom View to bring it back correctly (and handling layout changes there)
        Instead, we're refreshing the dynamic elements which makes this fragile */

        super.onSaveInstanceState(savedInstanceState);
        Log.i(FiveKings.APP_TAG,"onSaveInstanceState: Saving State");
        //save Game info
        savedInstanceState.putParcelable("mGame", mGame);
        savedInstanceState.putString("mHint", mHint);
        savedInstanceState.putString("playButtonText", (String) mPlayButton.getText());
        savedInstanceState.putParcelable("highlightWildCardRank", highlightWildCardRank);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            //get the scaledCardHeight to fit the melds/cards 50% overlapping
            //set it here because we don't know height until the window is focused
            scaledCardHeight = CARD_SCALING * mCurrentCards.getHeight();

            //This logic is if we've resumed or changed orientation and need to re-layout
            if ((mGame != null) && (needToRefreshCardsMelds)) {
                Log.i(FiveKings.APP_TAG, "onWindowFocusChanged: Refreshing cards, melds, and display");
                mGame.relayoutPlayerMiniHands(this);

                //if we resume from a screen with the hands dealt, then we need to reset that
                //so that when you re-lay them out they set the card showing correctly
                switch (mGame.getGameState()) {
                    case NEW_GAME:
                        break;

                    case ROUND_READY_TO_DEAL:
                        this.mPlayButton.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
                        break;

                    case ROUND_START:
                        mPlayButton.setText(resFormat(R.string.nextRound, mGame.getRoundOf().getString()));
                        disableDragToDiscardPile();
                        // Animate draw/discard piles when appropriate
                        if ((mGame.getCurrentPlayer() != null)
                                && (mGame.getCurrentPlayer().getTurnState() == Player.TurnState.PLAY_TURN)) {
                            enableDrawDiscardClick();
                        }
                        mGame.setMiniHandsSolid();
                        break;

                    case TURN_END:
                        mPlayButton.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
                        //animates whichever one was already moving if this is NOT_MY_TURN phase
                        mGame.animatePlayerMiniHand(null, shakeAnimation);
                        setShowHint(getString(R.string.tapMovingHandHint), HandleHint.SET_AND_SHOW_HINT, false);
                        mGame.setMiniHandsSolid();
                        if ((mGame.getCurrentPlayer() == null) || mGame.currentAndNextAreHuman()) updateHandsAndCards(false, mGame.getCurrentPlayer().isHuman());
                        else  updateHandsAndCards(mGame.getCurrentPlayer().showCards(isShowComputerCards()), mGame.getCurrentPlayer().isHuman());
                        break;

                    case TURN_START:
                        mPlayButton.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
                        disableDragToDiscardPile();
                        // Animate draw/discard piles when appropriate
                        if ((mGame.getCurrentPlayer() != null) && (mGame.getCurrentPlayer().getTurnState() == Player.TurnState.PLAY_TURN)) {
                            enableDrawDiscardClick();
                        }
                        mGame.setMiniHandsSolid();
                        updateHandsAndCards(mGame.getCurrentPlayer() == null ? false : mGame.getCurrentPlayer().showCards(isShowComputerCards()), mGame.getCurrentPlayer().isHuman());
                        break;

                    case HUMAN_PICKED_CARD:
                    case HUMAN_READY_TO_DISCARD:
                        disableDrawDiscardClick();
                        enableDragToDiscardPile();
                        mGame.setMiniHandsSolid();
                        updateHandsAndCards(mGame.getCurrentPlayer().showCards(isShowComputerCards()), mGame.getCurrentPlayer().isHuman());
                        break;

                    default:
                        mGame.setMiniHandsSolid();
                }//end switch
                //If we were in the Tutorial, restart there
                switch (showingTutorial) {
                    case 1:
                        showIntro1();
                        break;
                    case 2:
                        showIntro2();
                        break;
                    case 3:
                        showIntro3();
                        break;
                    case 4:
                        showIntro4();
                        break;
                    case 5:
                        showIntro5();
                        break;
                    case 6:
                        showIntro6();
                        break;
                    case 7:
                        showIntro7();
                        break;
                    case 8:
                        showIntro8();
                        break;
                    case 9:
                        showIntro9();
                        break;
                    default:
                }


                needToRefreshCardsMelds = false;
            }
        }
    }//end method OnWindowFocusChanged

    /*------------------*/
    /*   EVENT HANDLERS */
    /*------------------*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, 0);
                SettingsDialogFragment settingsDialogFragment = SettingsDialogFragment.newInstance(isShowComputerCards(),isAnimateDealing(),
                        settings.getBoolean(NOVICE_MODE, true),settings.getBoolean(HIGHLIGHT_WILDCARDS, true) );
                settingsDialogFragment.show(getFragmentManager(), null);
                return true;
            case R.id.action_add_player:
                showAddPlayers();
                return true;
            case R.id.action_edit_player:
            case R.id.action_delete_player:
                Toast.makeText(this,R.string.edit_delete_hint,Toast.LENGTH_LONG).show();
                return false;
            case R.id.action_new_game:
                return checkNewGame();
            case R.id.action_help:
                IntroBox.show(FiveKings.this);
                return true;
            case R.id.action_about:
                AboutBox.show(FiveKings.this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void cleanupShowcaseView(ShowcaseView sv) {
        //release view memory to reduce OOM errors
        /* don't have a reference to these Listeners
        sv.getViewTreeObserver().removeOnPreDrawListener();
        if(Build.VERSION.SDK_INT>15){
            sv.getViewTreeObserver().removeOnGlobalLayoutListener();
        }
        else {
            sv.getViewTreeObserver().removeGlobalOnLayoutListener(globalLayout);
        }
        */
        //Builder and insertShowcase addView to this parent (the base view including status bar)
        ((ViewGroup) findViewById(android.R.id.content)).removeView(sv);
    }

    private void showIntro1() {
        showingTutorial = 1;
        this.lastShowcaseView = new ShowcaseView.Builder(this)
                .withNewStyleShowcase()
                .setTarget(new ViewTarget(mPlayButton))
                .setStyle(R.style.ShowcaseForFiveKingsNext)
                .setContentTitle(R.string.intro1_title)
                .setContentText(R.string.intro1_text)
                .blockAllTouches()
                .build();
        this.lastShowcaseView.setOnShowcaseEventListener(new OnShowcaseEventListener() {
            @Override
            public void onShowcaseViewHide(ShowcaseView showcaseView) {

            }

            @Override
            public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                clearPlayArea();
                startGame();
                startRound();
                dealRound();
                showIntro2();
            }

            @Override
            public void onShowcaseViewShow(ShowcaseView showcaseView) {

            }
        });
    }

    private void showIntro2() {
        showingTutorial=2;
        cleanupShowcaseView(this.lastShowcaseView);
        this.lastShowcaseView = new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(mGame.getNextPlayer().getMiniHandLayout()))
                .setStyle(R.style.ShowcaseForFiveKingsNext)
                .setContentTitle(R.string.intro2_title)
                .setContentText(R.string.intro2_text)
                .blockAllTouches()
                .build();
        this.lastShowcaseView
                .setOnShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        mGame.rotatePlayer(); //also sets PREPARE_TURN
                        mGame.getCurrentPlayer().prepareTurn(FiveKings.this);
                        showIntro3();
                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                    }
                });
    }
    private void showIntro3() {
        showingTutorial=3;
        cleanupShowcaseView(this.lastShowcaseView);
        this.lastShowcaseView = new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(mCurrentCards))
                .setStyle(R.style.ShowcaseForFiveKingsNext)
                .setContentTitle(R.string.intro3_title)
                .setContentText(R.string.intro3_text)
                .blockAllTouches()
                .build();
        this.lastShowcaseView
                .setOnShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        showIntro4();
                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                    }
                });
    }
    private void showIntro4() {
        showingTutorial = 4;
        cleanupShowcaseView(this.lastShowcaseView);
        this.lastShowcaseView = new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(mDiscardPile))
                .setStyle(R.style.ShowcaseForFiveKingsNext)
                .setContentTitle(R.string.intro4_title)
                .setContentText(R.string.intro4_text)
                .blockAllTouches()
                .build();
        this.lastShowcaseView
                .setOnShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        showIntro5();
                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                    }
                });
    }
    private void showIntro5() {
        showingTutorial=5;
        cleanupShowcaseView(this.lastShowcaseView);
        this.lastShowcaseView = new  ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(mDrawPile))
                .setStyle(R.style.ShowcaseForFiveKingsNext)
                .setContentTitle(R.string.intro5_title)
                .setContentText(R.string.intro5_text)
                .blockAllTouches()
                .build();
        this.lastShowcaseView
                .setOnShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        showIntro6();
                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                    }
                });
    }
    private void showIntro6() {
        showingTutorial=6;
        cleanupShowcaseView(this.lastShowcaseView);
        this.lastShowcaseView = new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(mCurrentMelds))
                .setStyle(R.style.ShowcaseForFiveKingsNext)
                .setContentTitle(R.string.intro6_title)
                .setContentText(R.string.intro6_text)
                .blockAllTouches()
                .build();
        this.lastShowcaseView
                .setOnShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        showIntro7();
                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                    }
                });
    }
    private void showIntro7() {
        showingTutorial=7;
        cleanupShowcaseView(this.lastShowcaseView);
        this.lastShowcaseView =
                new ShowcaseView.Builder(this)
                        .setTarget(new ViewTarget(mCurrentCards))
                        .setStyle(R.style.ShowcaseForFiveKingsNext)
                        .setContentTitle(R.string.intro7_title)
                        .setContentText(R.string.intro7_text)
                        .blockAllTouches()
                        .build();
        this.lastShowcaseView
                .setOnShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        showIntro8();
                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                    }
                });
    }


    private void showIntro8() {
        showingTutorial=8;
        cleanupShowcaseView(this.lastShowcaseView);
        this.lastShowcaseView =
        new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(mDiscardPile))
                .setStyle(R.style.ShowcaseForFiveKingsNext)
                .setContentTitle(R.string.intro8_title)
                .setContentText(R.string.intro8_text)
                .blockAllTouches()
                .build();
        this.lastShowcaseView
                .setOnShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        showIntro9();
                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                    }
                });
    }
    private void showIntro9() {
        showingTutorial=9;
        cleanupShowcaseView(this.lastShowcaseView);
        this.lastShowcaseView =
        new ShowcaseView.Builder(this)
                .setTarget(com.github.amlcurran.showcaseview.targets.Target.NONE)
                .setStyle(R.style.ShowcaseForFiveKingsLast)
                .setContentTitle(R.string.intro9_title)
                .setContentText(R.string.intro9_text)
                .blockAllTouches()
                .build();
        this.lastShowcaseView
                .setOnShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        showingTutorial=0;
                        cleanupShowcaseView(FiveKings.this.lastShowcaseView);
                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                    }
                });
    }
    //Called from settingsDialogFragment
    public void setSettings(final boolean showComputerCards, final boolean animateDealing, boolean tutorialMode, boolean highlightWildcards) {
        //store only in preferences
        SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(SHOW_COMPUTER_HANDS_SETTING, showComputerCards);
        editor.putBoolean(ANIMATE_DEALING_SETTING, animateDealing);
        editor.putBoolean(NOVICE_MODE, tutorialMode);
        editor.putBoolean(HIGHLIGHT_WILDCARDS, highlightWildcards);

        // Lazy Commit the edits!
        editor.apply();
    }

    //Event handler for [Save] in add player dialog
    public void addEditPlayerClicked(final String playerName, final boolean isHuman, final boolean addingFlag, final int iPlayerUpdate) {
         //if we add a player, we need to remove and re-add the hand layouts
        if (addingFlag) mGame.addPlayer(playerName, isHuman ? PlayerList.PlayerType.HUMAN : PlayerList.PlayerType.EXPERT_COMPUTER, this);
        else mGame.updatePlayer(playerName, isHuman, iPlayerUpdate);
    }

    public void deletePlayerClicked(final int iPlayerToDelete) {
        //pop up an alert to verify
        if ((mGame == null) || (mGame.getGameState() != GameState.NEW_GAME)) {
            setShowHint(getString(R.string.cantAddDelete), HandleHint.SHOW_HINT, true);
        }else if (mGame.numPlayers() <= 2) {
            setShowHint(getString(R.string.mustHaveTwoPlayer), HandleHint.SHOW_HINT, true);
        }else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.confirmDelete)
                    .setMessage(resFormat(R.string.areYouSureDelete, mGame.getPlayerByIndex(iPlayerToDelete).getName()))
                    .setPositiveButton(R.string.yesDelete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //have to be careful of order and save the layout to delete from the screen
                            mGame.deletePlayer(iPlayerToDelete, FiveKings.this);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    //the Event handler for button presses on [Play]
    private void playGameClicked(){
        //If you're dealing then cancel dealing
        if (dealAnimatorSet != null) dealAnimatorSet.cancel();
        //If you're exploding hands, then cancel and show the final result
        if (explodeSet != null) explodeSet.cancel();

        if (GameState.NEW_GAME == mGame.getGameState()) {
            if (getSharedPreferences(SETTINGS_NAME, 0).getBoolean(NOVICE_MODE, true)) {
                showIntro1();
            } else {
                clearPlayArea();
                startGame();
            }
        }
        //starting a round
        else if (GameState.ROUND_START == mGame.getGameState()) {
            startRound();
            dealRound();
        }

        else setShowHint(null, HandleHint.SHOW_HINT, true); //force show the current hint
    }

    //Event handler when Human clicks Draw or Discard Pile
    void clickedDrawOrDiscard(final Game.PileDecision drawOrDiscardPile) {
        mGame.setGameState(GameState.HUMAN_PICKED_CARD);
        disableDrawDiscardClick();
        setShowHint(R.string.meldAndDragDiscardHint, FiveKings.HandleHint.SET_AND_SHOW_HINT, false);
        //turn on ability to accept drag to DiscardPile
        enableDragToDiscardPile();
        mGame.getCurrentPlayer().takeTurn(this, drawOrDiscardPile, mGame.isFinalTurn());
    }



    private void startRound() {
        mCurrentCards.removeAllViews();
        mCurrentMelds.removeAllViews();
        mGame.resetPlayerMiniHandsToRoundStart();
        //can't show discardPile after initRound because cards have been dealt
        mDiscardPile.setCardAndImage(this, null, mGame.getRoundOf()); //TODO:A really should reflect the discardPile

        mGame.initRound(); //actually does the dealing, including the discard pile
        //if HIGHLIGHT_WILDCARDS set, then highlight wildcards in human players' hands
        highlightWildCardRank = getSharedPreferences(SETTINGS_NAME, 0).getBoolean(HIGHLIGHT_WILDCARDS, true) ? mGame.getRoundOf() : null;

        this.mPlayButton.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
        mGame.setGameState(GameState.ROUND_READY_TO_DEAL);
    }
    private void dealRound() {
        showHideDrawAndDiscardPiles(true);
        if (isAnimateDealing()) {
            this.dealAnimatorSet = animateDealing(mGame.getRoundOf().getRankValue(), mGame.numPlayers());
        }
        else {
            afterDealing();
            mGame.setMiniHandsSolid();
        }
        mGame.setGameState(GameState.TURN_END);
        //If next hand is Human, findBestHandStart does nothing; if Computer then it starts considering Discard Pile card
        mGame.findBestHandStart();
    }

    private boolean checkNewGame() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.newGame)
                .setMessage(R.string.areYouSure)
                .setPositiveButton(R.string.newGame, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), R.string.toStartGameHint, Toast.LENGTH_LONG).show();
                        mPlayButton.setText(getText(R.string.startGame));
                        if (mGame != null) mGame.setGameState(GameState.NEW_GAME);
                        clearPlayArea();
                        mGame.init();
                    }
                })
                .setNegativeButton("No", null)
                .show();
        return false;
    }

    private void clearPlayArea() {
        //hide piles and remove cards and melds
        showHideDrawAndDiscardPiles(false);
        mCurrentCards.removeAllViews();
        mCurrentMelds.removeAllViews();
    }

    private boolean startGame() {
        //User pressed [Start Game] button
        mGame.init();
        mGame.setGameState(GameState.ROUND_START);
        this.mPlayButton.setText(resFormat(R.string.nextRound, mGame.getRoundOf().getString()));
        mGame.relayoutPlayerMiniHands(this);

        return true;
    }

    public void endTurnCheckRound(boolean showCards) {
        //remove discard from player's hand and add to discardPile
        Deck.getInstance().addToDiscardPile(mGame.getCurrentPlayer().discardFromHand(mGame.getCurrentPlayer().getHandDiscard()));
        mGame.endCurrentPlayerTurn();
        // if isFinalTurn then we always show cards (because they put down their hand)
        updateHandsAndCards(showCards || mGame.isFinalTurn(), !mGame.isFinalTurn() && mGame.getCurrentPlayer().isHuman());
        // (isFinalTurn controls whether you can drag cards after final human turn, or whether computer cards show)
        if (mGame.isFinalTurn()) {
            // addToCumulativeScore() moved from animateRoundScore, because logging in checkEndRound continues before animation completes
            // maintain a separate displayed score in the miniHand
            mGame.getCurrentPlayer().addToCumulativeScore();
            animateRoundScore(mGame.getCurrentPlayer());
        }
        //checkEndRound sets GameState to one of these 3 options: Round End, Turn End, Game End
        mGame.checkEndRound();
        GameState gameState = mGame.getGameState();
        if (GameState.ROUND_END == gameState) {
            mGame.setGameState(GameState.ROUND_START);
            mPlayButton.setText(resFormat(R.string.nextRound, mGame.getRoundOf().getString()));
            setShowHint(resFormat(R.string.displayScores, null),HandleHint.SHOW_HINT , false);
            setShowHint(resFormat(R.string.nextRoundHint,mGame.getRoundOf().getString()),HandleHint.SET_HINT, false);
        }
        else if (GameState.TURN_END == gameState) {
            //If next hand is Human, findBestHandStart does nothing; if Computer then it starts considering Discard Pile card
            mGame.findBestHandStart();

            //if next player is also human, then force show hint about hiding the hand and passing the game
            mGame.animatePlayerMiniHand(mGame.getNextPlayer(), shakeAnimation);
            setShowHint((mGame.currentAndNextAreHuman() && !mGame.isFinalTurn()) ? getString(R.string.hidingYourHandHint) : getString(R.string.tapMovingHandHint),
                    HandleHint.SET_AND_SHOW_HINT , (mGame.currentAndNextAreHuman() && !mGame.isFinalTurn()));
        }
        else if (GameState.GAME_END == gameState) {
            mGame.setGameState(GameState.NEW_GAME);
            mPlayButton.setText(R.string.startGame);
            this.explodeSet = explodeHand(mGame.getWinner());
            mGame.logFinalScores();
        }
    }


    /*---------------------------------------------------*/
    /* EVENT HANDLERS for clicks/drags on cards or melds */
    /*---------------------------------------------------*/
    boolean discardedCard(final int iCardView) {
        //turn off the ability of DiscardPile to accept drags
        disableDragToDiscardPile();
        // Find the card that was dropped...
        CardView foundCardView = findViewByIndex(iCardView);
        //..remove it from the hand
        mGame.setHandDiscard(foundCardView.getCard());
        //...and add to discard pile
        endTurnCheckRound(!mGame.currentAndNextAreHuman()); //hide if Human to human
        showDiscardPileCard(); //has to be after endTurnCheckRound because that's where discard is done
        return true;
    }

    //makeNewMeld is called if you drag onto the mCurrentMelds layout - we're creating a new meld
    //to add to a existing meld, drag to an existing meld
    //TODO:A Move this into the Player because we could be dragging when CurrentPlayer is not Human
    //(Create subclass makeNewMeld and addToMeld in Human/ComputerPlayer)
    boolean makeNewMeld(final int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        Player currentPlayer = mGame.getCurrentPlayer();
        if (foundCardView != null) {
            //create trialMeld (one card at a time for now)
            ((HumanPlayer) currentPlayer).makeNewMeld(foundCardView.getCard());
            updateHandsAndCards(currentPlayer.showCards(isShowComputerCards()), currentPlayer.isHuman());
        }
        return (foundCardView != null);
    }

    //Don't test for valid meld; that is done with evaluateMelds
    boolean addToMeld(final CardList meld, final int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        Player currentPlayer = mGame.getCurrentPlayer();
        if (foundCardView != null) {
            //add to existing meld
            ((HumanPlayer) currentPlayer).addToMeld(meld, foundCardView.getCard());
            updateHandsAndCards(currentPlayer.showCards(isShowComputerCards()), currentPlayer.isHuman());
        }
        return (foundCardView != null);
    }

    CardView findViewByIndex(final int iCardView) {
        CardView foundCardView=null;
        //find the view we coded with this index - have to loop thru nested layouts
        for (int iNestedLayout = 0; iNestedLayout < mCurrentCards.getChildCount(); iNestedLayout++) {
            View rl = mCurrentCards.getChildAt(iNestedLayout);
            if ((rl != null) && (rl instanceof RelativeLayout)) {
                RelativeLayout nestedLayout = (RelativeLayout) rl;
                for (int iView = 0; iView < nestedLayout.getChildCount(); iView++) {
                    CardView cv = (CardView) nestedLayout.getChildAt(iView);
                    if ((cv != null) && (cv.getViewIndex() == iCardView)) {
                        foundCardView = cv;
                        break;
                    }
                }
            }
            if (foundCardView != null) break;
        }
        if (foundCardView == null) { //less likely, but search melded cards too
            for (int iNestedLayout = 0; iNestedLayout < mCurrentMelds.getChildCount(); iNestedLayout++) {
                View rl = mCurrentMelds.getChildAt(iNestedLayout);
                //Allows for the fake "Drag here to form new meld" TextView
                if ((rl != null) && (rl instanceof RelativeLayout)) {
                    RelativeLayout nestedLayout = (RelativeLayout) rl;
                    for (int iView = 0; iView < nestedLayout.getChildCount(); iView++) {
                        CardView cv = (CardView) nestedLayout.getChildAt(iView);
                        if (cv.getViewIndex() == iCardView) {
                            foundCardView = cv;
                            break;
                        }
                    }
                }
                if (foundCardView != null) break;
            }
        }
        if (foundCardView == null) {
            Toast.makeText(this, "No matching card found", Toast.LENGTH_SHORT).show();
            Log.e(FiveKings.APP_TAG, "findViewByIndex: No matching card found");
        }
        // else Toast.makeText(this, foundCardView.getCard().getCardString() + " discarded", Toast.LENGTH_SHORT).show();
        return foundCardView;
    }


    /*---------------------------------------------------*/
    /* COMMON METHODS FOR ENABLING/DISABLING UI ELEMENTS */
    /*---------------------------------------------------*/
    void enableDragToDiscardPile() {
        mDiscardPile.setAcceptDrag(true);
    }
    void disableDragToDiscardPile() {
        mDiscardPile.setAcceptDrag(false);
    }

    //just prevent clicking, but for animations we need the buttons to be enabled
    public void enableDrawDiscardClick() {
        if (this.mDrawPile != null) {
            this.mDrawPile.setEnabled(true);
            this.mDrawPile.setClickable(true);
        }
        if (this.mDiscardPile != null) {
            this.mDiscardPile.setEnabled(true);
            this.mDiscardPile.setClickable(true);
        }
        animatePiles();
        setShowHint(R.string.pickFromDrawOrDiscardHint, HandleHint.SET_AND_SHOW_HINT, false);
    }
    void disableDrawDiscardClick() {
        if (this.mDrawPile != null) {
            this.mDrawPile.setClickable(false);
        }
        if (this.mDiscardPile != null) {
            this.mDiscardPile.setClickable(false);
        }
    }


    /*--------------------*/
    /* ANIMATION ROUTINES */
    /*--------------------*/
    public void showSpinner(final boolean show) {
        spinner.setVisibility(show ? View.VISIBLE : View.GONE);
        spinner.invalidate();
    }

    private AnimatorSet animateDealing(final int numCards, final int numPlayers) {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);

        //the template card that animates from the center point to the hands
        // the deck (now showing before this call) remains visible underneath it
        final CardView dealtCardView = new CardView(this, CardView.sBlueBitmapCardBack);
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(this.getDrawPileWidth(), this.getDrawPileHeight());
        pileLp.addRule(RelativeLayout.ALIGN_LEFT, mDrawPile.getId());
        pileLp.addRule(RelativeLayout.ALIGN_TOP, mDrawPile.getId());
        dealtCardView.setLayoutParams(pileLp);
        drawAndDiscardPiles.addView(dealtCardView);

        final AnimatorSet dealSet = new AnimatorSet();
        dealSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mPlayButton.setText(R.string.skipDealing);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                drawAndDiscardPiles.removeView(dealtCardView);
                afterDealing(); //also shows Discard Pile card
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                //Don't use showHint, because the text gets overridden in the calling thread
                Toast.makeText(FiveKings.this, R.string.toDisableDealing, Toast.LENGTH_LONG).show();
            }
        });

        Animator dealtCardAnimator = AnimatorInflater.loadAnimator(this, R.animator.deal_from_drawpile);
        Animator lastDealtCardAnimator = null;
        ObjectAnimator alphaAnimator;

        int dealtCardViewLocation[] = new int[2];
        //have to use location of mDrawPile here, because dealtCardView has not been positioned
        mDrawPile.getLocationInWindow(dealtCardViewLocation);
        for (int iCard = 0; iCard < numCards; iCard++) {
            for (int iPlayer = 0; iPlayer < numPlayers; iPlayer++) {
                dealtCardAnimator = dealtCardAnimator.clone();
                dealtCardAnimator.setTarget(dealtCardView);

                // Now add translation between dealtCard and miniHand
                int miniHandLayoutLocation[] = new int[2];
                mGame.getPlayerByIndex(iPlayer).getMiniHandLayout().getCardView().getLocationInWindow(miniHandLayoutLocation);
                final float translationX = miniHandLayoutLocation[0] - dealtCardViewLocation[0];
                final float translationY = miniHandLayoutLocation[1] - dealtCardViewLocation[1];

                //offsets where the cards are dealt according to player
                ObjectAnimator playerOffsetXAnimator = ObjectAnimator.ofFloat(dealtCardView, "TranslationX", 0f, translationX);
                ObjectAnimator playerOffsetYAnimator = ObjectAnimator.ofFloat(dealtCardView, "TranslationY", 0f, translationY);

                dealtCardAnimator.setInterpolator(new DecelerateInterpolator());
                if (lastDealtCardAnimator == null)
                    dealSet.play(dealtCardAnimator).with(playerOffsetXAnimator).with(playerOffsetYAnimator);
                else
                    dealSet.play(dealtCardAnimator).with(playerOffsetXAnimator).with(playerOffsetYAnimator).after(lastDealtCardAnimator);
                lastDealtCardAnimator = dealtCardAnimator;
                //if this is the first card, then add an alpha animation to show the blank hand space being replaces by a card
                if (iCard == 0) {
                    CardView playerHandCard = mGame.getPlayerByIndex(iPlayer).getMiniHandLayout().getCardView();
                    alphaAnimator = ObjectAnimator.ofFloat(playerHandCard, "Alpha", ALMOST_TRANSPARENT_ALPHA, 1.0f);
                    alphaAnimator.setDuration(ANIMATION_100MS);
                    dealSet.play(dealtCardAnimator).with(alphaAnimator);
                }
            }//end for iPlayer
        }//end for numCards

        // Add a final card to the discard pile
        dealtCardAnimator = dealtCardAnimator.clone();
        dealtCardAnimator.setTarget(dealtCardView);
        int discardPileLocation[] = new int[2];
        mDiscardPile.getLocationInWindow(discardPileLocation);
        ObjectAnimator discardPileOffsetXAnimator = ObjectAnimator.ofFloat(dealtCardView, "TranslationX", 0f, (float)(discardPileLocation[0] - dealtCardViewLocation[0]));
        ObjectAnimator discardPileOffsetYAnimator = ObjectAnimator.ofFloat(dealtCardView, "TranslationY", 0f, (float)(discardPileLocation[1] - dealtCardViewLocation[1]));
        dealSet.play(dealtCardAnimator).with(discardPileOffsetXAnimator).with(discardPileOffsetYAnimator).after(lastDealtCardAnimator);

        dealSet.start();
        return dealSet;
    }//end animateDealing


    private void afterDealing() {
        showDiscardPileCard();
        mPlayButton.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
        mGame.updatePlayerMiniHands();

        //if next player is also human, don't need to show "hiding" hint, because dealer's hand isn't showing
        mGame.animatePlayerMiniHand(mGame.getNextPlayer(), shakeAnimation);
        setShowHint(getString(R.string.tapMovingHandHint),HandleHint.SET_AND_SHOW_HINT , false);

    }

    private void layoutDrawAndDiscardPiles() {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);

        //set up Discard Pile
        final RelativeLayout.LayoutParams discardPileLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mDiscardPile = new CardView(this, mGame.peekDiscardPileCard(), DISCARDPILE_VIEW_ID, highlightWildCardRank);
        mDiscardPile.setBackgroundDrawable(getResources().getDrawable(R.drawable.dashed_border));
        mDiscardPile.setId(DISCARDPILE_VIEW_ID);
        discardPileLp.addRule(RelativeLayout.RIGHT_OF, mPlayButton.getId());
        discardPileLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mDiscardPile.setLayoutParams(discardPileLp);
        mDiscardPile.setAdjustViewBounds(true);
        mDiscardPile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickedDrawOrDiscard(Game.PileDecision.DISCARD_PILE);
            }
        });
        mDiscardPile.setOnDragListener(new DiscardPileDragEventListener());
        drawAndDiscardPiles.addView(mDiscardPile);

        //set up Draw Pile
        final RelativeLayout.LayoutParams drawPileLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mDrawPile = new CardView(this, CardView.sBlueBitmapCardBack);
        mDrawPile.setId(DRAWPILE_VIEW_ID);
        drawPileLp.addRule(RelativeLayout.LEFT_OF, mPlayButton.getId());
        drawPileLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mDrawPile.setLayoutParams(drawPileLp);
        mDrawPile.setAdjustViewBounds(true);
        mDrawPile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickedDrawOrDiscard(Game.PileDecision.DRAW_PILE);
            }
        });
        drawAndDiscardPiles.addView(mDrawPile);

    }

    private void showHideDrawAndDiscardPiles(final boolean show) {
        if (show){
            mDiscardPile.setVisibility(View.VISIBLE);
            mDrawPile.setVisibility(View.VISIBLE);
        } else {
            mDiscardPile.clearAnimation();
            mDiscardPile.setVisibility(View.INVISIBLE);
            mDrawPile.clearAnimation();
            mDrawPile.setVisibility(View.INVISIBLE);
        }

    }

    //shakes the Draw and Discard Piles to indicate that you should draw from them
    private void animatePiles() {
        Animation shakeCardAnimation = AnimationUtils.loadAnimation(this, R.anim.card_shake);
        this.mDrawPile.startAnimation(shakeCardAnimation);
        this.mDiscardPile.startAnimation(shakeCardAnimation);
    }

    //TODO:A Should be able to merge some code from these two methods
    public void animateHumanPickUp(final Game.PileDecision pickedFrom, final Card drawnCard) {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout) findViewById(R.id.draw_and_discard_piles);
        final Space spacer = (Space) findViewById(R.id.spacer);

        //Create the fake card we will animate - copied from animateComputerPickUp
        final CardView pileCardView;
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int pickedCardLocation[] = new int[2];

        //create a copy of the drawnCard and position over Draw or Discard pile
        pileCardView = new CardView(this, drawnCard, -1, highlightWildCardRank);
        if (pickedFrom == Game.PileDecision.DISCARD_PILE) {
            pileLp.addRule(RelativeLayout.ALIGN_LEFT, mDiscardPile.getId());
            pileLp.addRule(RelativeLayout.ALIGN_TOP, mDiscardPile.getId());
            //show the card underneath the one we are animating
            showDiscardPileCard();
            mDiscardPile.getLocationInWindow(pickedCardLocation);
        } else {
            pileLp.addRule(RelativeLayout.ALIGN_LEFT, mDrawPile.getId());
            pileLp.addRule(RelativeLayout.ALIGN_TOP, mDrawPile.getId());
            mDrawPile.getLocationInWindow(pickedCardLocation);
        }
        pileCardView.setLayoutParams(pileLp);
        pileCardView.setAdjustViewBounds(true);
        drawAndDiscardPiles.addView(pileCardView);

        //stop the cards jiggling since we have now picked
        this.mDrawPile.clearAnimation();
        this.mDiscardPile.clearAnimation();

        //Use Animator animation to combine shrinking behavior (in XML) with translation
        final AnimatorSet pickUpSet = new AnimatorSet();
        final Animator pickedCardAnimator = AnimatorInflater.loadAnimator(this,
                (pickedFrom == Game.PileDecision.DISCARD_PILE ? R.animator.human_from_discardpile : R.animator.human_from_drawpile));
        pickedCardAnimator.setTarget(pileCardView);

        // Now add translation based on left of cards, or approximate location of where card will appear if there are none
        // Height is moved down 1/3 of the height of the currentCards view, which is 1/2 the height of a card
        int mCurrentCardsLocation[] = new int[2];
        final float translationX;
        mCurrentCards.getLocationInWindow(mCurrentCardsLocation);
        final float translationY = mCurrentCardsLocation[1] - pickedCardLocation[1] + mCurrentCards.getHeight()/3;
        if (mCurrentCards.getChildCount() > 0) {
            mCurrentCards.getChildAt(0).getLocationInWindow(mCurrentCardsLocation);
            translationX = mCurrentCardsLocation[0] - pickedCardLocation[0];
        } else {
            translationX = mCurrentCardsLocation[0] + (mCurrentCards.getWidth()/2 - CardView.INTRINSIC_WIDTH/2)  - pickedCardLocation[0];
        }

        pickedCardAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        final ObjectAnimator pickedCardXAnimator = ObjectAnimator.ofFloat(pileCardView, "TranslationX", translationX);
        pickedCardXAnimator.setDuration(ANIMATION_1S);
        final ObjectAnimator pickedCardYAnimator = ObjectAnimator.ofFloat(pileCardView, "TranslationY", translationY);
        pickedCardYAnimator.setDuration(ANIMATION_1S);
        pickUpSet.play(pickedCardAnimator).with(pickedCardXAnimator).with(pickedCardYAnimator);

        pickUpSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                updateHandsAndCards(mGame.getCurrentPlayer().showCards(isShowComputerCards()), mGame.getCurrentPlayer().isHuman());
                //remove extra cards created for the purpose of animation
                drawAndDiscardPiles.removeView(pileCardView);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }
        });
        pickUpSet.start();

    }//end animateHumanPickUp


    public void animateComputerPickUpAndDiscard(final PlayerMiniHandLayout playerMiniHandLayout, final Game.PileDecision pickedFrom) {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);

        // The discard that will be shown (using Alpha) and animated back to the Discard Pile (then removed)
        final CardView discardCardView = new CardView(this, mGame.getCurrentPlayer().getHandDiscard(),-1, null);
        final RelativeLayout.LayoutParams discardLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        discardLp.addRule(RelativeLayout.ALIGN_LEFT, mDiscardPile.getId());
        discardLp.addRule(RelativeLayout.ALIGN_TOP, mDiscardPile.getId());
        discardCardView.setLayoutParams(discardLp);
        discardCardView.setAlpha(INVISIBLE_ALPHA);
        discardCardView.setAdjustViewBounds(true);
        drawAndDiscardPiles.addView(discardCardView);


        //Create the fake card we will animate off the Draw or Discard pile
        final CardView pileCardView;
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int pickedCardLocation[] = new int[2];

        if (pickedFrom == Game.PileDecision.DISCARD_PILE) {
            //mDiscardPile still shows the old card if we used it, so we can copy it and then update the pile underneath
            //create a copy of the Discard pile and position over the current Discard Pile
            pileCardView = new CardView(this,mDiscardPile);
            pileLp.addRule(RelativeLayout.ALIGN_LEFT, mDiscardPile.getId());
            pileLp.addRule(RelativeLayout.ALIGN_TOP, mDiscardPile.getId());
            pileCardView.setLayoutParams(pileLp);
            //show the card underneath the one we are animating
            showDiscardPileCard();
            mDiscardPile.getLocationInWindow(pickedCardLocation);
        }
        else {
            //create a copy of the top Draw pile card and position over the current pile
            pileCardView = new CardView(this, CardView.sBlueBitmapCardBack);
            pileLp.addRule(RelativeLayout.ALIGN_LEFT, mDrawPile.getId());
            pileLp.addRule(RelativeLayout.ALIGN_TOP, mDrawPile.getId());
            pileCardView.setLayoutParams(pileLp);
            mDrawPile.getLocationInWindow(pickedCardLocation);
        }
        pileCardView.setAdjustViewBounds(true);
        drawAndDiscardPiles.addView(pileCardView);

        //Use Animator animation because for Computer we animate pickup to hand
        //and then discard to Discard Pile (could be two different cards)
        final AnimatorSet pickAndDiscard = new AnimatorSet();
        final Animator pickedCardAnimator = AnimatorInflater.loadAnimator(this,
                (pickedFrom == Game.PileDecision.DISCARD_PILE ? R.animator.computer_from_discardpile : R.animator.computer_from_drawpile));
        pickedCardAnimator.setTarget(pileCardView);

        // Now add translation between pickedCard and miniHand
        int miniHandLayoutLocation[] = new int[2];
        playerMiniHandLayout.getCardView().getLocationInWindow(miniHandLayoutLocation);
        float translationX = miniHandLayoutLocation[0] - pickedCardLocation[0];
        float translationY = miniHandLayoutLocation[1] - pickedCardLocation[1];

        final ObjectAnimator pickedCardXAnimator = ObjectAnimator.ofFloat(pileCardView, "TranslationX",0f, translationX);
        pickedCardXAnimator.setDuration(ANIMATION_500MS);
        final ObjectAnimator pickedCardYAnimator = ObjectAnimator.ofFloat(pileCardView, "TranslationY", 0f, translationY);
        pickedCardYAnimator.setDuration(ANIMATION_500MS);
        pickAndDiscard.play(pickedCardAnimator).with(pickedCardXAnimator).with(pickedCardYAnimator);

        // animate discardCardView back to discard pile
        mDiscardPile.getLocationInWindow(pickedCardLocation);
        translationX = miniHandLayoutLocation[0] - pickedCardLocation[0];
        translationY = miniHandLayoutLocation[1] - pickedCardLocation[1];

        final Animator discardAnimator = AnimatorInflater.loadAnimator(this, R.animator.to_discardpile);
        discardAnimator.setTarget(discardCardView);
        final ObjectAnimator discardXAnimator = ObjectAnimator.ofFloat(discardCardView, "TranslationX",translationX,0f );
        discardXAnimator.setDuration(ANIMATION_1S);
        final ObjectAnimator discardYAnimator = ObjectAnimator.ofFloat(discardCardView, "TranslationY", translationY,0f);
        discardYAnimator.setDuration(ANIMATION_1S);
        pickAndDiscard.play(discardAnimator).with(discardXAnimator).with(discardYAnimator).after(pickedCardAnimator);

        pickAndDiscard.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                //remove extra cards created for the purpose of animation
                drawAndDiscardPiles.removeView(discardCardView);
                drawAndDiscardPiles.removeView(pileCardView);
                showDiscardPileCard();
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }
        });
        pickAndDiscard.start();

    }//end animateComputerPickUpAndDiscard

    //show the score at 5x size, animating to the player hand
    void animateRoundScore(final Player currentPlayer) {
        //set roundScore to visible and show it in AnimationStart

        final TextView roundScoreView = currentPlayer.getMiniHandLayout().getRoundScoreView();
        final AnimatorSet scaleAndTranslateSet = new AnimatorSet();
        final Animator scaleAnimator = AnimatorInflater.loadAnimator(this,R.animator.scale_score);
        scaleAnimator.setTarget(roundScoreView);

        // Now add translation based on top of currentMelds (so between currentCards and currentMelds)
        int mCurrentMeldsLocation[] = new int[2];
        mCurrentMelds.getLocationInWindow(mCurrentMeldsLocation);
        int miniHandLayoutLocation[] = new int[2];
        currentPlayer.getMiniHandLayout().getLocationInWindow(miniHandLayoutLocation);
        final float translationX = miniHandLayoutLocation[0] - (mCurrentMeldsLocation[0] + mCurrentMelds.getWidth()/2);
        final float translationY = miniHandLayoutLocation[1] - (mCurrentMeldsLocation[1]);

        final ObjectAnimator scoreXAnimator = ObjectAnimator.ofFloat(roundScoreView, "TranslationX", -translationX, 0f);
        final ObjectAnimator scoreYAnimator = ObjectAnimator.ofFloat(roundScoreView, "TranslationY", -translationY, 0f);

        scaleAndTranslateSet.addListener(new Animator.AnimatorListener() {
                 @Override
                 public void onAnimationStart(Animator animator) {
                     currentPlayer.updatePlayerMiniHand(true, false);
                 }

                 @Override
                 public void onAnimationEnd(Animator animator) {
                     currentPlayer.updatePlayerMiniHand(true, true); //to show cumulative score when roundScore arrives
                 }

                 @Override
                 public void onAnimationRepeat(Animator animator) {
                 }

                 @Override
                 public void onAnimationCancel(Animator animator) {
                 }
        });
        scoreXAnimator.setDuration(ANIMATION_2S);
        scoreYAnimator.setDuration(ANIMATION_2S);
        scaleAndTranslateSet.play(scoreXAnimator).with(scoreYAnimator).with(scaleAnimator);
        scaleAndTranslateSet.start();
    }

    AnimatorSet explodeHand(final Player winningPlayer) {
        final RelativeLayout draw_and_discard = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);

        //get's the location of a point between the Draw and Discard pile (source for dealt cards)
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(this.getDrawPileWidth(),this.getDrawPileHeight());
        pileLp.addRule(RelativeLayout.CENTER_HORIZONTAL);

        mCurrentMelds.removeAllViews();
        mCurrentCards.removeAllViews();
        mDrawPile.setVisibility(View.INVISIBLE);
        mDiscardPile.setVisibility(View.INVISIBLE);
        final AnimatorSet explodeSet = new AnimatorSet();
        Animator cardAnimator = AnimatorInflater.loadAnimator(this, R.animator.spiral_outwards);
        AnimatorSet lastCardAnimatorSet=null;

        //pile the cards in the middle of the screen and then animate them
        final Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        float height =size.y;

        //deal the complete deck - have to clone it because otherwise we can't continue to next game
        Deck deck2 = (Deck) Deck.getInstance().clone();
        while (deck2.peekNext() != null) {
            AnimatorSet cardAnimatorSet = new AnimatorSet();
            Card card = deck2.deal();
            cardAnimator = cardAnimator.clone();
            addExplodeTranslation(explodeSet, cardAnimatorSet, lastCardAnimatorSet, cardAnimator,
                    card, pileLp, height, draw_and_discard);
            lastCardAnimatorSet = cardAnimatorSet;
        }
        explodeSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {

                new AlertDialog.Builder(FiveKings.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.congratulationsName)
                        .setMessage(String.format(getString(R.string.congratulationsDetails), winningPlayer.getName(), String.valueOf(winningPlayer.getCumulativeScore())))
                        .setPositiveButton(R.string.newGame, null)
                        .show();
                setShowHint(getString(R.string.toStartGameHint), HandleHint.SET_AND_SHOW_HINT, true);

            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                draw_and_discard.removeAllViews();
            }
        });
        explodeSet.start();

        return explodeSet;
    }
    private void addExplodeTranslation(final AnimatorSet explodeSet, final AnimatorSet cardAnimatorSet, final AnimatorSet lastCardAnimatorSet, final Animator cardAnimator,
                                       final Card card, RelativeLayout.LayoutParams pileLp, final float height , final RelativeLayout relativeLayout) {
        CardView cardView = new CardView(this, card, -1, null);
        cardView.setLayoutParams(pileLp);
        relativeLayout.addView(cardView);
        cardAnimator.setTarget(cardView);
        double randomAngle = Math.toRadians(45 + 90 * Math.random());
        //animate them down from the DrawPile and off the screen
        float translationX = (float) (2*height * Math.cos(randomAngle));
        float translationY = (float) (2*height * Math.sin(randomAngle)) ;
        ObjectAnimator xAnimator = ObjectAnimator.ofFloat(cardView, "TranslationX", translationX);
        ObjectAnimator yAnimator = ObjectAnimator.ofFloat(cardView, "TranslationY", translationY);

        //want effect of cards flying out one after another
        cardAnimatorSet.setDuration(3 * ANIMATION_100MS);
        cardAnimatorSet.play(cardAnimator).with(xAnimator).with(yAnimator);

        if (lastCardAnimatorSet == null) explodeSet.play(cardAnimatorSet);
        //every 4/5 (approx) we split the group to provide a more continuous experience
        else if (Math.random() < 0.8) {
            explodeSet.play(cardAnimatorSet).after(lastCardAnimatorSet);
        }else {
            explodeSet.play(cardAnimatorSet).with(lastCardAnimatorSet);
        }
    }

    /*----------------------------------------------*/
    /* SHOW CONTENTS OF YOUR HAND                   */
    /*----------------------------------------------*/
    public void updateHandsAndCards(final boolean isShowCardsOrFinalTurn, final boolean isAllowDragging) {
        //automatically shows all round scores if final round
        //TODO:A If updateHandsAndCards is called once per player, then shouldn't be updating all miniHands
        mGame.updatePlayerMiniHands();

        if (mGame.getCurrentPlayer() != null) {
            if (mGame.getCurrentPlayer() == mGame.getPlayerWentOut()) {
                setShowHint(String.format(getString(R.string.wentOut), mGame.getPlayerWentOut().getName()), HandleHint.SHOW_HINT, true);
            }
            //showCards is true if being called from Human Player or by Computer Player with showComputerCards set
            //last parameter controls whether we allow dragging and show the border to drag to create new melds
            if (isShowCardsOrFinalTurn) {
                int iBase = showCards(mGame.getCurrentPlayer().getHandUnMelded(), mCurrentCards, 0, false, isAllowDragging);
                showCards(mGame.getCurrentPlayer().getHandMelded(), mCurrentMelds, iBase, true, isAllowDragging);
            } else {
                //now just clears the cards&melds area
                showNothing(mGame.getRoundOf().getRankValue(), mCurrentCards, mCurrentMelds);
            }
        }
    }

    //shows nothing (mini-hand has the draw or discard)
    private void showNothing(final int numCards, final RelativeLayout cardsLayout, final RelativeLayout meldsLayout) {
        cardsLayout.removeAllViews();
        meldsLayout.removeAllViews();
    }

    private int showCards(final ArrayList<CardList> meldLists, final RelativeLayout relativeLayout, final int iViewBase, final boolean showBorder, final boolean allowDragging) {
        int iView=iViewBase;
        final float cardScaleFactor = scaledCardHeight / CardView.INTRINSIC_HEIGHT ;
        int xMeldOffset=0;
        int xCardOffset=0;
        int yMeldOffset= (int) (+Y_MELD_OFFSET_RATIO * scaledCardHeight);

        relativeLayout.removeAllViews();
        for (CardList cardList : meldLists) {
            if (cardList.isEmpty()) continue;
            RelativeLayout nestedLayout = new RelativeLayout(this);
            nestedLayout.setTag(cardList); //so we can retrieve which meld a card is dragged onto
            //Create and add an invisible view that fills the necessary space for all cards in this meld
            CardView cvTemplate = new CardView(this, CardView.sBlueBitmapCardBack);
            cvTemplate.setVisibility(View.INVISIBLE);
            //the final width is the first card + the offsets of the others x the height scale factor
            nestedLayout.addView(cvTemplate, (int) ((CardView.INTRINSIC_WIDTH + (cardList.size() - 1) * CARD_OFFSET_RATIO * CardView.INTRINSIC_WIDTH) * cardScaleFactor), (int) scaledCardHeight);
            //translate this meld from the LHS by the width of the last meld + the extra meld offset
            nestedLayout.setTranslationX(xMeldOffset + xCardOffset);
            nestedLayout.setTranslationY(yMeldOffset);
            xMeldOffset += cardScaleFactor * (X_MELD_OFFSET_RATIO * CardView.INTRINSIC_WIDTH) + xCardOffset;
            yMeldOffset = -yMeldOffset;


            if (showBorder) {
                //if this is actually a validated meld, outline in solid green
                if (Meld.isValidMeld(cardList))
                    nestedLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.solid_green_border));
                else
                    nestedLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.dashed_border));
            }
            xCardOffset=0;
            for (Card card : cardList) {
                //highlight wildcards unless no dragging (Computer turn, or Human final turn)
                CardView cv = new CardView(this, card, iView, allowDragging ? highlightWildCardRank : null );
                cv.setTag(iView); //allows us to pass the index into the dragData without dealing with messy object passing
                iView++;
                cv.setTranslationX(xCardOffset);
                xCardOffset += cardScaleFactor * (CARD_OFFSET_RATIO * CardView.INTRINSIC_WIDTH);
                cv.bringToFront();
                cv.setClickable(false); //TODO:B: Allow selection by clicking for multi-drag?
                if (allowDragging) {//no dragging of computer cards or Human cards after final turn
                    cv.setOnDragListener(new CardViewDragEventListener());
                    cv.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
                            // Create a new ClipData using the tag as a label, the plain text MIME type, and
                            // the string containing the View index. This will create a new ClipDescription object within the
                            // ClipData, and set its MIME type entry to "text/plain"
                            ClipData dragData = ClipData.newPlainText("Discard", v.getTag().toString());
                            View.DragShadowBuilder cardDragShadow = new CardViewDragShadowBuilder(v);

                            v.startAnimation(Utilities.instantFade(FiveKings.HALF_TRANSPARENT_ALPHA, FiveKings.HALF_TRANSPARENT_ALPHA));
                            // Starts the drag
                            v.startDrag(dragData,cardDragShadow, null,0);
                            return true;
                        }//end onClick
                    });
                }//end if allowDragging

                nestedLayout.addView(cv, (int) (cardScaleFactor*CardView.INTRINSIC_WIDTH), (int) scaledCardHeight);
            }//end cards in meld
            if (allowDragging) nestedLayout.setOnDragListener(new CurrentMeldDragListener());
            relativeLayout.addView(nestedLayout);
        }
        if (showBorder && allowDragging) {
            //Now add a "Drag here to create a new meld" space, although the handler will actually be the CurrentMeldLayoutListener
            TextView newMeldSpace = new TextView(this);
            newMeldSpace.setText(R.string.dragYourCards);
            newMeldSpace.setLines(5);
            //Move this a full card's space over from last card
            newMeldSpace.setTranslationX(xMeldOffset+ cardScaleFactor* CardView.INTRINSIC_WIDTH*(1- X_MELD_OFFSET_RATIO)+xCardOffset);
            newMeldSpace.setTranslationY(yMeldOffset);
            newMeldSpace.setTypeface(null, Typeface.ITALIC);
            newMeldSpace.setTextColor(Color.WHITE);
            newMeldSpace.setBackgroundDrawable(getResources().getDrawable(R.drawable.dashed_border));
            newMeldSpace.setEnabled(false);
            relativeLayout.addView(newMeldSpace, (int) (cardScaleFactor * CardView.INTRINSIC_WIDTH), (int) scaledCardHeight);
        }
        return iView; //used as the starting index for mCurrentMelds
    }//end showCards



    //TODO:A Logic sHould probably be in DrawAndDiscardPile?
    void showDiscardPileCard() {
        //TODO:B better would be card stack/list that automatically associates the right card
        // but right now we need to change the Card/Image for a fixed CardView (which is linked into the display)
        //this handles peekDiscardPileCard == null
        mDiscardPile.setCardAndImage(this, mGame.peekDiscardPileCard(), highlightWildCardRank);
    }

    /* if mText is null, show the existing hint
    If mText is not null, either show it or set it or both
    forceShow forces showing even if hints are turned off
     */
    public void setShowHint(final String mText, final HandleHint setShowHint, final boolean forceShow) {
        if ((setShowHint == HandleHint.SET_HINT) || (setShowHint == HandleHint.SET_AND_SHOW_HINT)) {
            this.mHint = mText;
            if (setShowHint == HandleHint.SET_HINT) return;
        }
        //Skip showing any hints if we are in the tutorial screens
        if (showingTutorial <= 0) {
            //Hints are shown if forceShow true, or until the 4th round unless you turn off showIntroduction
            if (forceShow || (getSharedPreferences(SETTINGS_NAME, 0).getBoolean(NOVICE_MODE, true) &&
                    (mGame != null) && (mGame.getRoundOf() != null) && (mGame.getRoundOf().getOrdinal() <= HELP_ROUNDS.getOrdinal()))) {
                mHintToast.setText(mText == null ? mHint : mText);
                mHintToast.show();
            }
        }
    }
    void setShowHint(final int resource, final HandleHint setShowHint, final boolean forceShow ) {
        setShowHint(getString(resource), setShowHint, forceShow);
    }

    //open Add Player dialog
    void showAddPlayers() {
        if ((mGame == null) || (mGame.getGameState() != GameState.NEW_GAME)) {
            setShowHint(getString(R.string.cantAddDelete), HandleHint.SHOW_HINT, true);
        }else {
            //pop open the Players dialog for the names
            EnterPlayersDialogFragment epdf = EnterPlayersDialogFragment.newInstance(null, false, true, -1);
            epdf.show(getFragmentManager(), null);
        }
    }

    void showEditPlayer(final String oldPlayerName, final boolean oldIsHuman, final int iPlayer) {
        EnterPlayersDialogFragment epdf = EnterPlayersDialogFragment.newInstance(oldPlayerName, oldIsHuman, false, iPlayer);
        epdf.show(getFragmentManager(),null);
    }

    /* GETTER * Yuck */

    public Game getmGame() {
        return mGame;
    }

    final int getDrawPileHeight() {
        return this.mDrawPile.getHeight();
    }

    final int getDrawPileWidth() {
        return this.mDrawPile.getWidth();
    }

    final public boolean isShowComputerCards() {
        SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, 0);
        return settings.getBoolean(SHOW_COMPUTER_HANDS_SETTING, false);
    }

    final boolean isAnimateDealing() {
        SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, 0);
        return settings.getBoolean(ANIMATE_DEALING_SETTING, true);
    }

    /* SMALL UTILITY METHODS */
    private TextView clone(final TextView vFrom) {
        final TextView vTo = new TextView(this);
        cloneLayout(vFrom, vTo);
        return vTo;
    }

    static private void cloneLayout(final TextView vFrom, final TextView vTo) {
        vTo.setLayoutParams(vFrom.getLayoutParams());
        vTo.setPadding(vFrom.getPaddingLeft(), vFrom.getPaddingTop(), vFrom.getPaddingRight(), vFrom.getPaddingBottom());
    }

    private String resFormat(final int resource, final String param) {
        return String.format(getString(resource),param);
    }


    /* DEPRECATED METHODS */
    //Keeping this code in case we reintroduce and end-of-game score list
    @Deprecated
    private void showPlayerScores2(List<Player> players) {

        if (null == mScoreDetail) {
            //mScoreDetail = (TableLayout) findViewById(R.id.scoreDetail);
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


}
