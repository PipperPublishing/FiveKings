package com.pipperpublishing.fivekings.view;

import android.content.ClipData;
import android.content.ClipDescription;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Created by Jeffrey on 2/26/2015. Checks the cards you drag in to be melded
 * 2/27/2015    This is the layout itself (the dashed box) and creates a new meld
 */
class CurrentMeldsLayoutDragListener implements View.OnDragListener {
    // This is the method that the system calls when it dispatches a drag event to the listener.
    @Override
    public boolean onDrag(final View v, final DragEvent event) {
        RelativeLayout rl = (RelativeLayout)v;

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
                rl.invalidate();
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                // Ignore the event
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                rl.invalidate();
                return true;

            case DragEvent.ACTION_DROP:
                // Gets the item containing the dragged data
                ClipData.Item item = event.getClipData().getItemAt(0); //this is the View index to recover which card
                // Gets the text data from the item.
                String dragData = item.getText().toString();
                rl.invalidate();

                //Handle exception here and return false (drop wasn't handled) if it wasn't found
                int iView = -1;
                try {
                    iView = Integer.parseInt(dragData);
                }catch (NumberFormatException e) {
                    return false;
                }
                return ((FiveKings)rl.getContext()).makeNewMeld(iView);

            case DragEvent.ACTION_DRAG_ENDED:
                // Turns off any color tints and invalidate the view to force a redraw
                rl.invalidate();
                return true;

            // An unknown action type was received.
            default:
                Log.e("CurMeldsDragListener", "Unknown action type received by OnDragListener.");
        }

        return false;
    }
}
