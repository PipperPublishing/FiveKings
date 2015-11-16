/*
 * Copyright Jeffrey Pugh (pipper.publishing@gmail.com) (c) 2015. All rights reserved.
 */

package com.pipperpublishing.fivekings.view;

import android.content.ClipDescription;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by Jeffrey on 11/15/2015. Listens for a card drag and asks if you melded
 * 11/15/2015   Created
 */
class MiniHandsLayoutDragListener implements View.OnDragListener {
    // This is the method that the system calls when it dispatches a drag event to the listener.
    @Override
    public boolean onDrag(final View v, final DragEvent event) {
        LinearLayout rl = (LinearLayout)v;

        // Defines a variable to store the action type for the incoming event
        final int action = event.getAction();

        // Handles each of the expected events
        switch(action) {
            case DragEvent.ACTION_DRAG_STARTED:
                // Determines if this View can accept the dragged data
                if (event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    // applies a blue color tint to the View to indicate that it can accept data.
                    rl.invalidate();

                    // returns true to indicate that the View can accept the dragged data.
                    return true;
                }
                // Returns false. During the current drag and drop operation, this View will
                // not receive events again until ACTION_DRAG_ENDED is sent.
                return false;

            case DragEvent.ACTION_DRAG_ENTERED:
                ((FiveKings)rl.getContext()).checkMeldedOnDiscard();
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                return true;

            case DragEvent.ACTION_DROP:
                return true;

            case DragEvent.ACTION_DRAG_ENDED:
                return true;

            // An unknown action type was received.
            default:
                Log.e("CurMeldsDragListener", "Unknown action type received by OnDragListener.");
        }

        return false;
    }
}
