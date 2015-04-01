package com.example.jeffrey.fivekings;

import android.content.ClipData;
import android.content.ClipDescription;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.example.jeffrey.fivekings.util.Utilities;

/**
 * Created by Jeffrey on 2/27/2015.
 * An existing meld listens for a card being dragged on top of it
 * 3/3/2015 Fade melds even more so highlighted ones stand out
 */
class CurrentMeldDragListener  implements View.OnDragListener  {
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
                    rl.startAnimation(Utilities.instantFade(FiveKings.THIRD_TRANSPARENT_ALPHA, FiveKings.THIRD_TRANSPARENT_ALPHA));
                    // returns true to indicate that the View can accept the dragged data.
                    return true;
                }
                // Returns false. During the current drag and drop operation, this View will
                // not receive events again until ACTION_DRAG_ENDED is sent.
                return false;

            case DragEvent.ACTION_DRAG_ENTERED:
                rl.clearAnimation();
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                // Ignore the event
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                rl.startAnimation(Utilities.instantFade(0.3f, 0.3f));
                return true;

            case DragEvent.ACTION_DROP:
                rl.clearAnimation();
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
                return ((FiveKings)rl.getContext()).addToMeld((CardList) rl.getTag(), iView);

            case DragEvent.ACTION_DRAG_ENDED:
                rl.clearAnimation();
                return true;

            // An unknown action type was received.
            default:
                Log.e("CurMeldsDragListener", "Unknown action type received by OnDragListener.");
        }

        return false;
    }

}
