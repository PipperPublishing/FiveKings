package com.pipperpublishing.fivekings;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Created by Jeffrey on 2/22/2015.
 * 2/23/2015    Set shadow to be size of card, with touch point at bottom (so easy to see)
 */

class CardViewDragShadowBuilder extends View.DragShadowBuilder {

    // The drag shadow image, defined as a drawable thing
    private Drawable shadow;

    // Defines the constructor for myDragShadowBuilder
    CardViewDragShadowBuilder(final View v) {

        // Stores the View parameter passed to myDragShadowBuilder.
        super(v);

        // Creates a draggable image that will fill the Canvas provided by the system.
        shadow = ((CardView)v).getDrawable().getConstantState().newDrawable();
    }

    // Defines a callback that sends the drag shadow dimensions and touch point back to the system.
    @Override
    public void onProvideShadowMetrics(final Point size, final Point touch) {
        int width, height;
        // Sets the shadow to the same size as the card (otherwise hard to see under finger)
        width = getView().getWidth();
        height = getView().getHeight();
        size.set(width, height);
        // The drag shadow is a ColorDrawable. This sets its dimensions to be the same as the
        // Canvas that the system will provide. As a result, the drag shadow will fill the Canvas
        shadow.setBounds(0, 0, width, height);

        // Sets the touch point's position to be at the bottom of the drag shadow
        touch.set(width/2, (int)(0.8f * height));
    }

    // Defines a callback that draws the drag shadow in a Canvas that the system constructs
    // from the dimensions passed in onProvideShadowMetrics().
    @Override
    public void onDrawShadow(final Canvas canvas) {
        // Draws the ColorDrawable in the Canvas passed in from the system.
        shadow.draw(canvas);
    }
}

