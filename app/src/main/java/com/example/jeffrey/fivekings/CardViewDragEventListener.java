package com.example.jeffrey.fivekings;

import android.util.Log;
import android.view.DragEvent;
import android.view.View;

/**
 * Created by Jeffrey on 2/23/2015.
 * 2/26/2015    Currently does nothing, but eventually we'd like to rearrange cards when you drop one on another
 */
class CardViewDragEventListener implements View.OnDragListener {

    // This is the method that the system calls when it dispatches a drag event to the
    // listener.
    @Override
    public boolean onDrag(View v, DragEvent event) {
        CardView cardView = (CardView)v;

        // Defines a variable to store the action type for the incoming event
        final int action = event.getAction();

        // Handles each of the expected events
        switch(action) {
            case DragEvent.ACTION_DRAG_STARTED:
                // Returns false. During the current drag and drop operation, this View will
                // not receive events again until ACTION_DRAG_ENDED is sent.
                return false;

            case DragEvent.ACTION_DRAG_ENTERED:
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                // Ignore the event
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                return true;

            case DragEvent.ACTION_DROP:
                return true;

            case DragEvent.ACTION_DRAG_ENDED:
                cardView.clearColorFilter();
                cardView.setEnabled(true);
                cardView.invalidate();
                return true;

            // An unknown action type was received.
            default:
                Log.e("CardViewOnDragListener", "Unknown action type received by OnDragListener.");
        }

        return false;
    }//end onDrag
}//end  class OnDragListener

