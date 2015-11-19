/*
 * Copyright Jeffrey Pugh (pipper.publishing@gmail.com) (c) 2015. All rights reserved.
 */

package com.pipperpublishing.fivekings;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.animation.Animation;
import android.widget.LinearLayout;

import com.pipperpublishing.fivekings.view.FiveKings;
import com.pipperpublishing.fivekings.view.PlayerMiniHandLayout;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

/**
 * Created by Jeffrey on 3/22/2015.
 * 3/22/2015    Encapsulates list of players for Game
 * 3/24/2015    Moved next Dealer to initRound() (so we can handle deleting the dealer)
 * 8/29/2015    Layout mini hands relative to draw_and_discard_pile RelativeLayout
 * 9/21/2015    Record which player hand is animated (should really be pushed down into mini hands itself)
 * 9/30/2015    setAnimated loops through all players to unset animated - this may happen if we animated the wrong hand
 *              when coming back from an orientation change
  10/7/2015     removePlayerMiniHands was removing from fullscreen_content, not from draw_and_discard_piles
 10/8/2015      Change miniHand layout to move them all to a separate miniHand strip - hopefully this will also make it easier to do a slide-out drawer
 10/12/2015     Switch order of Standard Players so Human ends up on the right
                Moved the addDefaultPlayers call to Game so this class can stay generic
 10/18/2015     deletePlayer was removing layout from wrong view
 11/8/2015      Added parcelable support
 11/19/2015     Switched from using PlayerType to referencing the native class and using reflection
 */
public class PlayerList extends ArrayList<Player> implements Parcelable {

    private Player dealer;
    private Player currentPlayer;
    private Player playerWentOut;
    private Player animatedPlayerHand;

    PlayerList() {
        dealer = null;
        currentPlayer = null;
        playerWentOut = null;
        animatedPlayerHand = null;
    }

    //Called on first game and then if you want another game
    final void initGame() {
        this.dealer = this.get(0);
        for (Player player : this) player.initGame();
    }

    final void initRound() {
        currentPlayer=this.dealer;
        playerWentOut=null;
        animatedPlayerHand = null;
        this.dealer = getNextPlayer(currentPlayer);   //set this here so that if we delete the dealer we can advance the dealer
    }

    void addPlayer (final String newPlayerName, final Class<? extends Player> newPlayerClass) {
        final Player newPlayer;
        Constructor constructor;
        try {
            constructor = newPlayerClass.asSubclass(newPlayerClass).getConstructor(String.class);
        } catch (NoSuchMethodException e) {
            Log.d(FiveKings.APP_TAG,String.format("addPlayer: Unknown Player class %s without Constructor", newPlayerClass.toString()), e);
            return;
        }
        try {
            newPlayer = (Player) constructor.newInstance(newPlayerName);
        } catch (Exception e) {
            Log.d(FiveKings.APP_TAG,String.format("addPlayer: Constructor for class %s failed", newPlayerClass.toString()), e);
            return;
        }
        //record the new player in the player list
        this.add(newPlayer);
    }

    Player getPlayer(final int iPlayer) {
        return this.get(iPlayer);
    }

    void deletePlayer(final int iDeletedPlayer, Activity activity) {
        //need to advance to next player before we remove it
        if (this.get(iDeletedPlayer) == currentPlayer) currentPlayer = getNextPlayer(this.currentPlayer);
        if (this.get(iDeletedPlayer) == playerWentOut) playerWentOut = null;
        if (this.get(iDeletedPlayer) == dealer) dealer = getNextPlayer(dealer);

        //remove the PlayerLayout
        final LinearLayout miniHands = (LinearLayout) activity.findViewById(R.id.mini_hands);
        final PlayerMiniHandLayout deletedPlayerMiniHandLayout = this.getPlayer(iDeletedPlayer).getMiniHandLayout();
        //this one won't be deleted in the loop because we deleted it from the player list
        if (null != deletedPlayerMiniHandLayout) miniHands.removeView(deletedPlayerMiniHandLayout);
        this.remove(iDeletedPlayer);

        relayoutPlayerMiniHands(activity);
    }

    void updatePlayer(final String newPlayerName, final Class<? extends Player> newPlayerClass , final int iPlayer) {
        final Player oldPlayer=this.get(iPlayer);
        final Player newPlayer;

        //if we're changing player type, then we need to create a new one as a copy
        // the Copy Constructor also updates the miniHand to the new player reference
        if (oldPlayer.getClass() != newPlayerClass) {
            //get the Copy constructor
            Constructor copyConstructor;
            try {
                copyConstructor = newPlayerClass.getConstructor(Player.class);
            } catch (NoSuchMethodException e) {
                Log.d(FiveKings.APP_TAG,String.format("updatePlayer: Unknown Player class %s without Copy Constructor", newPlayerClass.toString()), e);
                return;
            }
            try {
                newPlayer = (Player) copyConstructor.newInstance(oldPlayer);
            } catch (Exception e) {
                Log.d(FiveKings.APP_TAG,String.format("updatePlayer: Copy constructor for class %s failed", newPlayerClass.toString()), e);
                return;
            }
            //record the new player in the player list
            this.set(iPlayer, newPlayer);
            if (oldPlayer == currentPlayer) currentPlayer = newPlayer;
            if (oldPlayer == playerWentOut) playerWentOut = newPlayer;
            if (oldPlayer == dealer) dealer = newPlayer;
        }
        else newPlayer=oldPlayer;
        //and now update the name (which is the only other thing that can be changed in the dialog)
        newPlayer.updateName(newPlayerName);
    }

    Player rotatePlayer() {
        this.currentPlayer.setTurnState(Player.TurnState.NOT_MY_TURN);
        this.currentPlayer = getNextPlayer(this.currentPlayer);
        this.currentPlayer.setTurnState(Player.TurnState.PREPARE_TURN);
        return this.currentPlayer;
    }

    //(Shouldn't be starting animations from outside FiveKings or related code)
    void setAnimated(final Player setAnimated, final Animation bounceAnimation) {
        clearAllAnimated();
        //if setAnimated == null, then use the saved animatedPlayerHand and reanimate
        animatedPlayerHand = setAnimated!=null ? setAnimated : animatedPlayerHand ;
        if ((animatedPlayerHand != null) && (animatedPlayerHand.getTurnState() == Player.TurnState.NOT_MY_TURN))
        {
            animatedPlayerHand.miniHandLayout.startAnimateMiniHand(bounceAnimation);
        }
    }
    void clearAllAnimated() {
        //clear existing animations
        for (Player player : this) player.getMiniHandLayout().clearAnimatedMiniHand();
    }


    Player getNextPlayer() {
        return getNextPlayer(this.currentPlayer);
    }

    Player getNextPlayer(final Player player){
        final Player nextPlayer;
        if (this.isEmpty()) nextPlayer=null;
        else {
            final int playerIndex = this.indexOf(player);
            if (playerIndex == -1) nextPlayer = null;
                //if this is the "last" player, return the "first" one
            else if (playerIndex == this.size() - 1) nextPlayer = this.get(0);
            else nextPlayer =  this.get(playerIndex + 1);
        }
        return nextPlayer;
    }

    Player getWinner() {
        Player winningPlayer = this.get(0);

        for (Player player : this) {
            if (player.getCumulativeScore() <= winningPlayer.getCumulativeScore()) {
                winningPlayer = player;
            }
        }
        return winningPlayer;
    }

    boolean nextPlayerWentOut() {
        return (getNextPlayer(currentPlayer) == playerWentOut);
    }

    Player endCurrentPlayerTurn() {
        this.playerWentOut = this.getCurrentPlayer().endTurn(this.getPlayerWentOut());
        return this.playerWentOut;
    }


    void logRoundScores() {
        if (playerWentOut == null) throw new RuntimeException("Error - playerWentOut is null");
        Log.i(FiveKings.APP_TAG, "Player "+playerWentOut.getName()+" went out");
        Log.i(FiveKings.APP_TAG, "        Current scores:");
        for (Player player : this){
            //add to cumulative score is done in animateRoundScore()
            player.logRoundScore();
        }

        if (this.dealer == null) throw new RuntimeException("logRoundScores: dealer is null" );
        this.currentPlayer = null;

    }


    void logFinalScores() {
        for (Player player : this) {
            Log.i(FiveKings.APP_TAG, player.getName() + "'s final score is " + player.getCumulativeScore());
        }
    }


    /* GETTERS and SETTERS */

    Player getCurrentPlayer() {
        return currentPlayer;
    }

    Player getPlayerWentOut() {
        return playerWentOut;
    }

    /* Update PlayerLayout */
    void resetPlayerMiniHandsRoundStart() {
        for (Player player : this) player.resetPlayerMiniHand();
    }

    void updatePlayerMiniHands() {
        for (Player player : this) player.updatePlayerMiniHand(this.currentPlayer == player, true);
    }

    void setMiniHandsSolid() {
        for (Player player : this) player.getMiniHandLayout().setGreyedOut(false);
    }


    void removePlayerMiniHands(Activity a) {
        final LinearLayout linearLayout = (LinearLayout) a.findViewById(R.id.mini_hands);
        for (int iPlayer = 0; iPlayer < this.size(); iPlayer++) {
            PlayerMiniHandLayout playerMiniHandLayout = this.get(iPlayer).getMiniHandLayout();
            linearLayout.removeView(playerMiniHandLayout);
            this.get(iPlayer).removePlayerMiniHand();
        }
    }

    //re-layout when we've changed number of players by adding/deleting
    void relayoutPlayerMiniHands(Activity a) {
        removePlayerMiniHands(a);
        final LinearLayout linearLayout = (LinearLayout) a.findViewById(R.id.mini_hands);
        for (int iPlayer = 0; iPlayer < this.size(); iPlayer++) {
            linearLayout.addView(this.get(iPlayer).addPlayerMiniHandLayout(a, iPlayer, this.size()));
        }
    }


    /* PARCELABLE readers/writers */

    protected PlayerList(Parcel parcel) {
        //repopulate the actual player list
        parcel.readList(this, Player.class.getClassLoader());
        //and reference back into it
        int index = parcel.readInt();
        //index = -1 means player reference = null
        dealer = index <0 ? null : this.get(index);
        index = parcel.readInt();
        currentPlayer = index <0 ? null : this.get(index);
        index = parcel.readInt();
        playerWentOut = index <0 ? null : this.get(index);
        index = parcel.readInt();
        animatedPlayerHand = index <0 ? null : this.get(index);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeList(this);   //the list of all players
        //Have to use indexOf so that we are referencing the new players, not creating a new one
        parcel.writeInt(this.indexOf(dealer));
        parcel.writeInt(this.indexOf(currentPlayer));
        parcel.writeInt(this.indexOf(playerWentOut));
        parcel.writeInt(this.indexOf(animatedPlayerHand));
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<PlayerList> CREATOR = new Parcelable.Creator<PlayerList>() {
        @Override
        public PlayerList createFromParcel(Parcel parcel) {
            return new PlayerList(parcel);
        }

        @Override
        public PlayerList[] newArray(int size) {
            return new PlayerList[size];
        }
    };

}
