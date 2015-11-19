/*
 * Copyright Jeffrey Pugh (pipper.publishing@gmail.com) (c) 2015. All rights reserved.
 */

package com.pipperpublishing.fivekings;

import android.os.Parcel;

/**
 * Created by Jeffrey on 3/12/2015.
 * 3/12/2015    For testing new algorithms on computer logic
 * We could do a mixture of approaches for the computer players - from Smart to Stupid
 * 3/13/2015    Provide a scoring comparator to look at partial melds and singles
 * 3/15/2015    Override isTestBetterThanBest and introduce keepDiscardDecision
 * 3/17/2015    Implement interface that is passed to Meld.decomposeAndCheck
 * 3/18/2015    Wasn't checking for case of numUnmeldedCards increased
 * 3/19/2015    isFirstBetterThanSecond: If everything else is equal, prefer more rank melds to sequence melds
 * 3/20/2015    Extend rank meld preference to keepDiscardDecision
 * 3/21/2015    Prefer full rank melds over sequences
 * 4/5/2015     Only override strategy portions - execution is same as ComputerPlayer
 * 11/16/2015   Provide a Copy constructor for ExpertComputerPlayer for edit Player option
 * 11/18/2015   Created HardComputerPlayer from previous ComputerPlayer
 * 11/19/2015   Constructors must be public to support reflection (used in add/update player)
 */
public class HardComputerPlayer extends EasyComputerPlayer {
    /* uses evaluation method to decide between hands (value of unmelded cards)
    and there is not "second guessing" using keepDiscardDecision */
    public HardComputerPlayer(final String name) {
        super(name);
    }

    public HardComputerPlayer(final Player oldPlayer) {
        super(oldPlayer);
    }

    @Override
    public boolean isFirstBetterThanSecond(final MeldedCardList testHand, final MeldedCardList bestHand, final boolean isFinalTurn) {
        return testHand.calculateValueAndScore(isFinalTurn) <= bestHand.calculateValueAndScore(isFinalTurn);
    }

    /* PARCELABLE read/write for StrategyComputerPlayer (use superclass implementation) */
    protected HardComputerPlayer(Parcel parcel) {
        super(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
    }

    @SuppressWarnings("unused")
    public static final Creator<HardComputerPlayer> CREATOR = new Creator<HardComputerPlayer>() {
        @Override
        public HardComputerPlayer createFromParcel(Parcel in) {
            return new HardComputerPlayer(in);
        }

        @Override
        public HardComputerPlayer[] newArray(int size) {
            return new HardComputerPlayer[size];
        }
    };

}
