package org.the3deer.android_3d_model_engine.gui;

import android.util.Log;

import androidx.annotation.NonNull;

import org.the3deer.android_3d_model_engine.event.SelectedObjectEvent;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.android_3d_model_engine.renderer.FPSEvent;
import org.the3deer.android_3d_model_engine.renderer.RenderEvent;
import org.the3deer.util.bean.BeanFactory;
import org.the3deer.util.bean.BeanManaged;
import org.the3deer.util.bean.BeanProperty;
import org.the3deer.util.event.EventListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

public class GUIDefault extends Widget implements EventListener, BeanManaged {

    private final static String TAG = GUIDefault.class.getSimpleName();;

    private Label icon;

    @BeanProperty
    private boolean enableFPS = true;

    private Label fps;
    private Text info;
    private Rotator rotator;
    private Widget mainMenu;
    private Widget window;
    private Menu menu;

    @Inject
    private Scene scene;
    /*@Inject
    private List<BeanManaged> beans;*/
    @Inject
    private Axis axis;
    @Inject
    private Camera camera;
    @Inject
    private Screen screen;

    public void setUp() {

        setVisible(true);
        setRender(false);

        //this.camera = BeanFactory.getInstance().find(Camera.class, "gui");
        if (camera != null) {
            Log.v("GUIDefault", "Dimensions: " + camera.getDimensions2D());
            setDimensions(camera.getDimensions2D());
        } else {
            Log.e("GUIDefault", "No camera");
        }

        //this.scene = BeanFactory.getInstance().find(SceneLoader.class);
        //camera.addListener(this);

        //refreshLocation();

        try {

            // GUI
            // icon
            /*icon = Label.forSymbol(FontFactory.getInstance());
            icon.setSymbol("burger");
            //icon.setRelativeScale(new float[]{0.1f,0.1f,0.1f});
            this.addChild(icon);
            //icon.setMargin(0.025f);
            icon.setRelativeLocation(Widget.POSITION_TOP_RIGHT);
            icon.setVisible(true);
            icon.setClickable(true);
            icon.addListener(this);
            Log.v("GUIDefault", "icon location: " + Arrays.toString(icon.getLocation()));*/

            if (axis !=  null) {
                addChild(axis);
                axis.setRelativeLocation(Widget.POSITION_TOP_LEFT);
                axis.setRelativeScale(new float[]{0.1f, 0.1f, 0.1f});
                axis.setVisible(true);
            }

            initFPS();
            initInfo();

            super.refresh();

        } catch (Exception e) {
            Log.e("GUIDefault", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void setEnableFPS(boolean enable){
        this.enableFPS = enable;
        initFPS();
        propagate(new ChangeEvent(this));
    }

    private void initFPS() {

        // frame-per-second
        if (enableFPS) {
            if (fps != null) return;
            fps = new Label(FontFactory.getInstance(), 7, 1);
            fps.setId("fps");
            //fps.setPadding(1);
            fps.setText("fps");
            fps.setVisible(true);

            addChild(fps);
            fps.setRelativeScale(new float[]{0.10f, 0.10f, 0.10f});
            fps.setRelativeLocation(Widget.POSITION_TOP);
        } else {
            this.removeChild(fps);
            fps = null;
        }
    }

    private void initInfo() {
        // model info
        if (info != null) return;
        info = Text.allocate(this, 15, 3, Text.PADDING_01);
        info.setId("info");
        info.setVisible(true);
        info.setParent(this);
        //info.setRelativeScale(new float[]{0.85f,0.85f,0.85f});
        info.setRelativeScale(new float[]{0.25f, 0.25f, 0.25f});

        addChild(info);

        info.setRelativeLocation(Widget.POSITION_BOTTOM);
        //addBackground(fps).setColor(new float[]{0.25f, 0.25f, 0.25f, 0.25f});
    }

    @Override
    public boolean onEvent(EventObject event) {
        if (event instanceof RenderEvent) {
            final RenderEvent rev = (RenderEvent) event;
            if (rev.getCode() == RenderEvent.Code.SURFACE_CHANGED) {
                setDimensions(camera.getDimensions2D());
                refresh();
                Log.v(TAG, "onEvent. SURFACE_CHANGED. Refreshed...");
            }
        } else if (super.onEvent(event)) {
            return true;
        } else if (event instanceof FPSEvent) {
            if (fps != null && fps.isVisible()) {
                FPSEvent fpsEvent = (FPSEvent) event;
                fps.setText(fpsEvent.getFps() + " fps");
            }
        } else if (event instanceof Camera.CameraUpdatedEvent){
            setDimensions(camera.getDimensions2D());
            refresh();
            //Log.v("GUIDefault", "onEvent. CameraUpdatedEvent. Refreshed...");
        } else if (event instanceof SelectedObjectEvent) {
            if (this.info != null && this.info.isVisible()) {
                final Object3DData selected = ((SelectedObjectEvent) event).getSelected();
                final StringBuilder info = new StringBuilder();
                if (selected != null) {
                    if (selected.getId() != null) {
                        if (selected.getId().indexOf('/') == -1) {
                            info.append(selected.getId());
                        } else {
                            info.append(selected.getId().substring(selected.getId().lastIndexOf('/') + 1));
                        }
                    } else {
                        info.append("unnamed");
                    }
                    info.append('\n');
                    info.append("size: ");
                    info.append(String.format(Locale.getDefault(), "%.2f", selected.getDimensions().getLargest()));
                    info.append('\n');
                    info.append("scale: ");
                    info.append(String.format(Locale.getDefault(), "%.2f", selected.getScaleX()));
                    //final DecimalFormat df = new DecimalFormat("0.##");
                    //info.append(df.format(selected.getScaleX()));
                    info.append("x");
                }
                Log.v("GUIDefault", "Selected object info: " + info);
                this.info.update(info.toString().toLowerCase());
            }
        } else if (event instanceof Window.WindowClosed) {
            window.setVisible(false);
        } else if (event instanceof ClickEvent) {
            Widget widget = ((Event) event).getWidget();
            if (widget == icon) {
                Log.v("GUIDefault", "Toggling menu visibility... "+icon.getId());
                mainMenu = createMenu_main();
                mainMenu.setVisible(true, true);
                mainMenu.setFloating(true);
                mainMenu.setClickable(true);
                addChild(mainMenu);
                return true;
            }
        }else if (event instanceof Menu.OptionSelectedEvent) {
            Log.v("GUIDefault", "Item selected: "+ event);
            final Menu.OptionSelectedEvent optionSelectedEvent = (Menu.OptionSelectedEvent) event;
            if (event.getSource() == mainMenu) {
                window = createMenu(mainMenu, optionSelectedEvent);
                //mainMenu.addChild(window);
                optionSelectedEvent.getOption().getWidget().addChild(window);
                window.setVisible(true, true);
                // window.setFloating(true);
                return true;
            }
        }

        return false;
    }

    @NonNull
    private Widget createMenu_main() {

        //final Widget container = new Panel(this);
        final Menu container = new Menu(Menu.Type.Button);
        container.setMargin(new float[]{24,24,24});
        container.setPadding(new float[]{16,16,16});
        container.setRelativeLocation(Widget.POSITION_TOP_RIGHT);
        //container.setMovable(true);
        container.addChild(new Label("Entities"));
        //container.setId("container_test");

        // main menu
        //final List<String> mainOptions = new ArrayList<>();
        final Map<String,Object> beans = BeanFactory.getInstance().getBeans();
        for (Map.Entry<String,Object> entry : beans.entrySet()){
            if (!BeanManaged.class.isAssignableFrom(entry.getValue().getClass())) continue;
            //mainOptions.add(entry.getKey());
            Panel optionPanel = new Panel();
            //optionPanel.setId("panel_test");
            container.addOption(new Menu.Option(entry.getKey(), optionPanel, entry.getValue()));

            Label widget = new Label(entry.getKey());
            optionPanel.addChild(widget);
            /*List<Field> fields = BeanFactory.getFields(entry.getValue(), BeanProperty.class);
            for (Field field : fields) {
                //mainOptions.add(field.getName());
                container.addChild(new Label(container, field.getName()));
            }*/
        }

        // design
        /*mainMenu = new Menu(container, Menu.Type.Button);
        mainMenu.setCentered(true);
        //mainMenu.setRelativeLocation(Widget.POSITION_MIDDLE);

        // action
        mainMenu.setClickable(true);
        mainMenu.addListener(this);*/

        // background
        final Widget mainMenuBackground = new Background(container);
        mainMenuBackground.setColor(Constants.COLOR_GRAY_TRANSLUCENT);
        container.addChild(mainMenuBackground);
        //container.setBackground(mainMenuBackground);

        // container
        //container.addChild(mainMenu);

        return container;
    }

    private Widget createMenu(Widget parent, Menu.OptionSelectedEvent item) {

        final Menu.Option option = item.getOption();
        try {
            if (option != null) {
                final List<Field> fields = BeanFactory.getFields(option.getObject(), BeanProperty.class);
                if (fields.isEmpty()) return new Label("empty: "+option.getId());

                final GridPanel container = new GridPanel(fields.size(), 2);
                container.setFloating(true);
                int row = 0;
                //container.addChild(new Label(option.getId()), row++, 0);

                for (Field field : fields) {

                    field.setAccessible(true);
                    final Label fieldLabel = new Label(field.getName());
                    final Label fieldValue = new Label(String.valueOf(field.get(option.getObject()))){
                        @Override
                        public boolean onEvent(EventObject event) {
                            if (event instanceof ChangeEvent && event.getSource() == GUIDefault.this)
                                setText(String.valueOf(GUIDefault.this.enableFPS));
                            return super.onEvent(event);
                        }
                    };
                    field.setAccessible(false);

                    container.addChild(fieldLabel, row, 0);
                    container.addChild(fieldValue, row++, 1);
                    fieldValue.setClickable(true);
                    fieldValue.setOnClick(()->{

                        Log.v("GUIDefault","field onClick: "+field.getType());
                        if (field.getType() == Boolean.TYPE){
                            final Menu menuValue = new Menu(Menu.Type.Button);
                            menuValue.setMargin(new float[]{24,24,24});
                            menuValue.setPadding(new float[]{16,16,16});
                            menuValue.addOption(new Menu.Option("true", new Label("true"), Boolean.TRUE));
                            menuValue.addOption(new Menu.Option("false", new Label("false"), Boolean.FALSE));
                            menuValue.setRelativeLocation(Widget.POSITION_CHILD_BOTTOM);

                            final Widget backgroundValue = new Background(menuValue);
                            backgroundValue.setColor(Constants.COLOR_GRAY_TRANSLUCENT);
                            menuValue.addChild(backgroundValue);

                            menuValue.setVisible(true, true);
                            fieldValue.addChild(menuValue);

                            menuValue.addListener(event -> {
                                try {
                                    if (event instanceof Menu.OptionSelectedEvent) {
                                        final Object value = ((Menu.OptionSelectedEvent) event).getOption().getObject();
                                        final Method setter = BeanFactory.getSetter(field);
                                        setter.invoke(item.getOption().getObject(),value);
                                        dispose();
                                        return true;
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error getting setter for field: "+field.getName(), e);
                                }
                                return false;
                            });
                        }
                    });
                    this.addListener(fieldValue);
                }

                // background
                final Widget background = new Background(container);
                background.setColor(Constants.COLOR_GRAY_TRANSLUCENT);
                container.addChild(background);

                return container;
            }

        } catch (IllegalAccessException e) {
            return new Label("Error ("+item.getOption().getId()+"): "+e.getMessage());
        }
        return new Label("Error ("+item.getOption().getId()+")");
    }

    /*@NonNull
    private Widget createMenu_main2() {


        final Widget container = new Panel();
        container.setRelativeLocation(Widget.POSITION_TOP_RIGHT);

        // main menu
        final List<String> mainOptions = new ArrayList<>();
        mainOptions.add("lights");
        mainOptions.add("wireframe");
        mainOptions.add("textures");
        mainOptions.add("colors");
        mainOptions.add("animation");
        mainOptions.add("stereoscopic");

        // design
        mainMenu = MenuBak.build(container, mainOptions.toArray(new String[mainOptions.size()]), MenuBak.Type.Button);
        mainMenu.setCentered(true);
        mainMenu.setRelativeLocation(Widget.POSITION_MIDDLE);

        // action
        mainMenu.setClickable(true);
        mainMenu.addListener(this);

        // background
        final Widget mainMenuBackground = new Background(container);
        mainMenuBackground.setColor(Constants.COLOR_GRAY_TRANSLUCENT);
        //container.setBackground(mainMenuBackground);
        //container.addChild(mainMenuBackground);


        // container
        container.addChild(mainMenu);

        return container;
    }*/
}
