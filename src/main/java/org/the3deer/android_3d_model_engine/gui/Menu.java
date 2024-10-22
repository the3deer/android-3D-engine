package org.the3deer.android_3d_model_engine.gui;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public class Menu extends Panel {

    public static class Option {
        private final String id;
        private final Widget widget;
        private final Object object;
        private int index;

        Option(String id, Widget widget, Object object) {
            this.id = id;
            this.widget = widget;
            this.object = object;
        }

        /**
         * @return the id of this option
         */
        public String getId() {
            return id;
        }

        /**
         * @return the widget link to this option
         */
        public Widget getWidget() {
            return widget;
        }

        /**
         * @return the object linked to this option
         */
        public Object getObject() {
            return object;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    public static class OptionSelectedEvent extends Widget.Event {

        private final Option option;
        OptionSelectedEvent(Object source, Option option) {
            super(source);
            this.option = option;
        }

        public Option getOption(){
            return option;
        }
    }

    public enum Type {Checkbox, Radio, Button };

    private final Type type;

    private int selected = -1;

    private Map<Widget,Option> options = new HashMap<>();
    private int index = 0;

    public Menu(Type type) {
        super();
        this.type = type;
        setLayout(Layout.Vertical);
    }

    @Override
    public void addChild(Widget child) {
        super.addChild(child);
        if (!(child instanceof Background)) {
            child.setClickable(true);
        }
    }

    public void addOption(Option option){
        addChild(option.getWidget());
        this.options.put(option.getWidget(),option);
        option.setIndex(index++);
    }

    @Override
    public void removeChild(Widget child) {
        super.removeChild(child);
        this.options.remove(child);
    }

    @Override
    public boolean onEvent(EventObject event) {
        if (event instanceof Widget.ClickEvent) {
            Widget.ClickEvent clickEvent = (Widget.ClickEvent) event;
            if (options.containsKey(clickEvent.getWidget())){
                final OptionSelectedEvent optionSelectedEvent =
                        new OptionSelectedEvent(this, this.options.get(clickEvent.getWidget()));
                propagate(optionSelectedEvent);
                return true;
            }
        }
        return super.onEvent(event);
    }
}
