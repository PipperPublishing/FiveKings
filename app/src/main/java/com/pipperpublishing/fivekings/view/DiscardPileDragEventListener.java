package com.pipperpublishing.fivekings.view;

import android.content.ClipData;
import android.content.ClipDescription;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;

/**
 * Created by Jeffrey on 2/21/2015 using example from http://developer.android.com/guide/topics/ui/drag-drop.html
 * 2/22/2015    Changed to be a Listener on DiscardPile
 * 2/22/2015    We don't transfer the Card object in the ClipData; rather we capture it by what card (index) has been dragged out
 * 2/24/2015    Handle parseInt exception and just return false (ACTION_DROP not handled)
 * 2/26/2015    Changed to a CardView from ImageButton
 * 4/9/2015    Added enable/disableDragToDiscardPile to prevent drag and drop before you've picked
 *              We rely on the Listener not firing if we don't accept the DRAG_STARTED event
 */
class DiscardPileDragEventListener implements View.OnDragListener {

    // This is the method that the system calls when it dispatches a drag event to the
    // listener.
    @Override
    public boolean onDrag(final View v, final DragEvent event) {
        //that we are not ready to accept the drag if not at the right point of the play
        CardView cv = (CardView)v;

        // Defines a variable to store the action type for the incoming event
        final int action = event.getAction();

        // Handles each of the expected events
        switch(action) {
            case DragEvent.ACTION_DRAG_STARTED:
                // Determines if this View can accept the dragged data
                if (cv.isAcceptDrag() && event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    //fades out of the draw pile
                    cv.startAnimation(Utilities.instantFade(FiveKings.HALF_TRANSPARENT_ALPHA, FiveKings.HALF_TRANSPARENT_ALPHA));
                    // returns true to indicate that the View can accept the dragged data.
                    return true;
                } else {
                    //remind user what they should be doing if in the first few rounds
                    ((FiveKings) cv.getContext()).setShowHint(null, FiveKings.HandleHint.SHOW_HINT, false);
                    // Returns false. During the current drag and drop operation, this View will
                    // not receive events again until ACTION_DRAG_ENDED is sent.
                    return false;
                }

            case DragEvent.ACTION_DRAG_ENTERED:
                cv.clearAnimation();
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                // Ignore the event
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                cv.startAnimation(Utilities.instantFade(FiveKings.HALF_TRANSPARENT_ALPHA, FiveKings.HALF_TRANSPARENT_ALPHA));
                return true;

            case DragEvent.ACTION_DROP:
                cv.clearAnimation();
                // Gets the item containing the dragged data
                ClipData.Item item = event.getClipData().getItemAt(0); //this is the View index to recover which card

                // Gets the text data from the item.
                String dragData = item.getText().toString();

                //Handle exception here and return false (drop wasn't handled) if it wasn't found
                int iView = -1;
                try {
                    iView = Integer.parseInt(dragData);
                }catch (NumberFormatException e) {
                    return false;
                }
                return ((FiveKings)cv.getContext()).discardedCard(iView);

            case DragEvent.ACTION_DRAG_ENDED:
                if (cv.isAcceptDrag()) cv.clearAnimation();
                return true;

            // An unknown action type was received.
            default:
                Log.e("DiscardPileDragEventListener", "Unknown action type received by OnDragListener.");
        }

        return false;
    }
}
