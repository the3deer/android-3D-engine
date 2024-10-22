package org.the3deer.android_3d_model_engine.gui;

import org.the3deer.android_3d_model_engine.model.Dimensions;
import org.the3deer.util.bean.BeanProperty;
import org.the3deer.util.math.MathUtils;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public class GridPanel extends Widget {

    private static final float UI_PANEL_MARGIN = 24;
    private static final float UI_PANEL_PADDING = 16;
    public enum Layout {Horizontal, Vertical}
    @BeanProperty
    private Layout layout;

    private float margin[] = new float[]{UI_PANEL_MARGIN, UI_PANEL_MARGIN, 0};
    private float padding[] = new float[]{UI_PANEL_PADDING, UI_PANEL_PADDING, 0};

    private final int rows;
    private final int cols;
    private final float[] rowHeights;
    private final float[] colWidths;

    private Map<Widget,int[]> cells = new HashMap<>();

    public GridPanel(int rows, int cols) {
        super();
        this.rows = rows;
        this.cols = cols;
        this.rowHeights = new float[rows];
        this.colWidths = new float[cols];
        setRender(false);
        setSolid(false);
    }

    public void addChild(Widget child, int row, int col) {
        cells.put(child,new int[]{row,col});
        super.addChild(child);
    }

    @Override
    public void removeChild(Widget child) {
        cells.remove(child);
        super.removeChild(child);
    }

    @Override
    public void dispose() {
        cells.clear();
        super.dispose();
    }

    @Override
    public boolean onEvent(EventObject event) {
        if (event instanceof ChildAdded || event instanceof ChildRemoved) {
            if (((Widget) event.getSource()).getParent() == this) {
                refreshDimensions();
                refresh();
            }
        }
        return super.onEvent(event);
    }

    private void refreshDimensions() {

        if (!widgets.isEmpty()) {
            for (Widget widget : widgets) {
                if (widget instanceof Background) continue; // this is to ignore the background :/
                final Dimensions currentDimensions = widget.getCurrentDimensions();

                float childHeight = currentDimensions.getHeight();
                float childWidth = currentDimensions.getWidth();

                int[] cell = cells.get(widget);
                if (cell == null) continue;

                if (childHeight > rowHeights[cell[0]]){
                    rowHeights[cell[0]] = childHeight;
                }
                if (childWidth > colWidths[cell[1]]){
                    colWidths[cell[1]] = childWidth;
                }
            }
        }
        this.height = MathUtils.sum(rowHeights) + padding[1]*(rows-1) + margin[1]*2;
        this.width = MathUtils.sum(colWidths) + padding[0]*(cols-1) + margin[0]*2;

        Dimensions thisDim = new Dimensions(
                new float[]{0, 0, 0, 1},
                new float[]{this.width, this.height, 0, 1});

        super.setDimensions(thisDim);
    }

    @Override
    public void refresh() {
        super.refresh();
        updateChildrenLocation();
    }

    private void updateChildrenLocation() {
        if (!widgets.isEmpty()) {
            final Dimensions thisDim = getCurrentDimensions();
            final float offsetX = thisDim.getMin()[0] + margin[0];
            final float offsetY = thisDim.getMax()[1] - margin[1];

            for (int i = 0; i < widgets.size(); i++) {
                final Widget widget = widgets.get(i);
                if (widget instanceof Background) continue;
                int[] cell = cells.get(widget);
                if (cell == null) continue;

                float cellOffsetX = offsetX + MathUtils.sum(this.colWidths, 0, cell[1]) + (cell[1] > 0 ? padding[0] : 0);
                float cellOffsetY = offsetY -  MathUtils.sum(this.rowHeights, 0, cell[0]) - (cell[0] > 0 ? padding[1] : 0);
                final Dimensions childDim = widget.getCurrentDimensions2();

                final float[] childLocation = {cellOffsetX, cellOffsetY - childDim.getMax()[1], 0};
                widget.setLocation(childLocation);

                // chain forward
                widget.refresh();
            }
        }
    }
}