package org.the3deer.android_3d_model_engine.gui;

import org.the3deer.android_3d_model_engine.model.Dimensions;
import org.the3deer.util.bean.BeanProperty;

public class Panel extends Widget {

    private static final String TAG = Panel.class.getSimpleName();
    public static final float UI_PANEL_MARGIN = 24;
    public static final float UI_PANEL_PADDING = 16;

    public enum Layout {Horizontal, Vertical}
    @BeanProperty
    private Layout layout;

    private float margin[] = new float[]{0, 0, 0};
    private float padding[] = new float[]{0, 0, 0};

    public Panel() {
        super();
        setRender(false);
        setSolid(false);
    }

    public void setMargin(float[] margin) {
        this.margin = margin;
    }

    public void setPadding(float[] padding) {
        this.padding = padding;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

/*    @Override
    public void addChild(Widget child) {
        super.addChild(child);
        refreshDimensions();
        refresh();
    }*/
/*
    @Override
    public boolean onEvent(EventObject event) {
        if (event instanceof ChildAdded || event instanceof ChildRemoved) {
            if (((Widget) event.getSource()).getParent() == this) {
                refreshDimensions();
                refreshLocation();
            }
        }
        return super.onEvent(event);
    }*/

    @Override
    public void refresh() {
        refreshDimensions();
        super.refresh();
        refreshChildrenLocation();
    }

    private void refreshDimensions() {

        float totalHeight = 0;
        float totalWidth = 0;
        if (!widgets.isEmpty()) {
            for (Widget widget : widgets) {
                if (widget instanceof Background) continue; // this is to ignore the background :/
                float childHeight = widget.getCurrentDimensions().getHeight();
                float childWidth = widget.getCurrentDimensions().getWidth();
                if (layout == null || layout == Layout.Vertical) {
                    totalHeight += childHeight + padding[1];
                    if (childWidth > totalWidth) {
                        totalWidth = childWidth;
                    }
                } else if (layout == Layout.Horizontal) {
                    totalWidth += childWidth + padding[0];
                    if (childHeight > totalHeight) {
                        totalHeight = childHeight;
                    }
                }
            }
        }
        this.height = totalHeight - padding[1] + margin[1]*2;
        this.width = totalWidth - padding[0] + margin[0]*2;

        Dimensions thisDim = new Dimensions(
                new float[]{0, 0, 0, 1},
                new float[]{this.width, this.height, 0, 1});

        super.setDimensions(thisDim);
    }



    private void refreshChildrenLocation() {
        if (!widgets.isEmpty()) {
            //Log.v(TAG, "Refreshing ("+getId()+") children: "+widgets.size());
            final Dimensions thisDim = getCurrentDimensions();
            float currentX = thisDim.getMin()[0] + margin[0];
            float currentY = thisDim.getMax()[1] - margin[1];
            float[] childLocation = {currentX, currentY, 0};
            for (int i = 0; i < widgets.size(); i++) {

                if (widgets.get(i) instanceof Background) continue;

                // this element
                final Dimensions childDim = widgets.get(i).getCurrentDimensions2();
                childLocation[0] -= childDim.getMin()[0];
                childLocation[1] -= childDim.getMax()[1];
                widgets.get(i).setLocation(childLocation);

                // chain forward
                widgets.get(i).refresh();

                // next element
                if (layout == null || layout == Layout.Vertical) {
                    currentY -= childDim.getHeight() + padding[1];
                } else if (layout == Layout.Horizontal) {
                    currentX += childDim.getWidth() + padding[0];
                }
                childLocation = new float[]{currentX, currentY, 0};
            }
        }
    }
}