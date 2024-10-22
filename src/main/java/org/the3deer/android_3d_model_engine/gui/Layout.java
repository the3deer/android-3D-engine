package org.the3deer.android_3d_model_engine.gui;

/**
 * A layout is a transparent container that manager the structure of the user interface widgets
 */
public class Layout extends Widget {

    public Layout(Widget parent) {
        super(parent, parent.contentDimensions.getWidth(), parent.contentDimensions.getHeight());
        setRender(false);
    }

    /**
     * A layout that arranges widgets either horizontally in a single column or vertically in a single row.
     */
    public static class LinearLayout extends Layout {

        public enum Orientation { Horizontal, Vertical}

        private final Orientation orientation;

        public LinearLayout(Widget parent, Orientation orientation) {
            super(parent);
            this.orientation = orientation;
        }
    }
}
