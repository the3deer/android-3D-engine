package org.the3deer.android.engine.gui;

import androidx.annotation.NonNull;

import org.the3deer.bean.Bean;
import org.the3deer.bean.BeanInit;
import org.the3deer.bean.BeanManaged;
import org.the3deer.bean.BeanManager;
import org.the3deer.bean.BeanProperty;
import org.the3deer.android.engine.event.FPSEvent;
import org.the3deer.android.engine.event.GLEvent;
import org.the3deer.android.engine.event.SelectedObjectEvent;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Dimensions;
import org.the3deer.android.engine.model.ModelEvent;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.event.EventListener;

import java.lang.reflect.Field;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

@Bean(name="gui", experimental = true)
public class GUI extends Widget implements EventListener, BeanManaged {

    private static final Logger logger = Logger.getLogger(GUI.class.getSimpleName());;

    private Label icon;
    private boolean showFPS = true;

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

    @BeanInit
    public void setUp() {

        setVisible(true);
        setRender(false);

        //this.camera = BeanFactory.getInstance().find(Camera.class, "gui");
        Dimensions dimensions = calculateScreenDimensions();
        setDimensions(dimensions);

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
            logger.finest("icon location: " + Arrays.toString(icon.getLocation()));*/

            if (axis !=  null) {
                addChild(axis);
                axis.setRelativeLocation(Widget.POSITION_TOP_RIGHT);
                axis.setRelativeScale(new float[]{0.1f, 0.1f, 0.1f});
                axis.setVisible(true);
                axis.setScreen(screen);
            }

            initFPS();
            initInfo();

            super.refresh();

        } catch (Exception e) {
            logger.log(Level.SEVERE,  e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private Dimensions calculateScreenDimensions() {

        // check
        if (screen == null){
            return new Dimensions(-Constants.UNIT * Constants.SCREEN_DEFAULT_RATIO, Constants.UNIT * Constants.SCREEN_DEFAULT_RATIO, Constants.UNIT, -Constants.UNIT, 1, 0);
        } else {
            return new Dimensions(-Constants.UNIT * screen.getRatio(), Constants.UNIT * screen.getRatio(), Constants.UNIT, -Constants.UNIT, 1, 0);
        }
    }

    public void setShowFPS(boolean enable){
        this.showFPS = enable;
        initFPS();
        propagate(new ChangeEvent(this));
    }

    public boolean isShowFPS(){
        return this.showFPS;
    }

    private void initFPS() {

        // frame-per-second
        if (showFPS) {
            if (fps == null) {
                fps = new Label(FontFactory.getInstance(), 7, 1);
                fps.setId("fps");
                //fps.setPadding(1);
                fps.setText("0 fps");
                fps.setVisible(true);
                fps.setScreen(screen);
                addChild(fps);
            }
            fps.setRelativeScale(new float[]{0.10f, 0.10f, 0.10f});
            fps.setRelativeLocation(Widget.POSITION_TOP);
        } else {
            this.removeChild(fps);
            fps = null;
        }
    }

    private void initInfo() {
        // model info
        if (info == null) {
            info = Text.allocate(this, 15, 3, Text.PADDING_01);
            info.setId("info");
            info.setVisible(true);
            info.setScreen(screen);
            //info.setParent(this);
            addChild(info);
        }
        //info.setRelativeScale(new float[]{0.85f,0.85f,0.85f});
        info.setRelativeScale(new float[]{0.25f, 0.25f, 0.25f});
        info.setRelativeLocation(Widget.POSITION_BOTTOM);
        //addBackground(fps).setColor(new float[]{0.25f, 0.25f, 0.25f, 0.25f});
    }

    @Override
    public boolean onEvent(EventObject event) {
        if (event instanceof ModelEvent) {
            final ModelEvent rev = (ModelEvent) event;
            if (rev.getCode() == ModelEvent.Code.SCREEN_CHANGED) {
                setDimensions(calculateScreenDimensions());
                refresh();
                logger.info("Refreshed after screen changed");
            }
        } if (event instanceof GLEvent) {
            final GLEvent rev = (GLEvent) event;
            if (rev.getCode() == GLEvent.Code.SURFACE_CHANGED) {
                setDimensions(calculateScreenDimensions());
                refresh();
                logger.info("Refreshed after surface changed");
            }
        } else if (super.onEvent(event)) {
            return true;
        } else if (event instanceof FPSEvent) {
            if (fps != null && fps.isVisible()) {
                FPSEvent fpsEvent = (FPSEvent) event;
                fps.setText(fpsEvent.getFps() + " fps");
                //logger.finest("FPS: "+fpsEvent.getFps());
            }
        } else if (event instanceof SelectedObjectEvent) {
           logger.finest("onEvent. SelectedObjectEvent: "+((SelectedObjectEvent) event).getSelected());
            if (this.info != null) {
                final Object3D selected = ((SelectedObjectEvent) event).getSelected();
                if (selected != null) {
                    final StringBuilder info = new StringBuilder();
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

                    logger.finest("Selected object info: " + info);
                    this.info.update(info.toString().toLowerCase());
                   this.info.setVisible(true);
                } else {
                   this.info.setVisible(false);
                }
            }
        } else if (event instanceof Window.WindowClosed) {
            window.setVisible(false);
        } else if (event instanceof ClickEvent) {
            Widget widget = ((Event) event).getWidget();
            if (widget == icon) {
                logger.finest("Toggling menu visibility... "+icon.getId());
                mainMenu = createMenu_main();
                mainMenu.setVisible(true, true);
                mainMenu.setFloating(true);
                mainMenu.setClickable(true);
                addChild(mainMenu);
                return true;
            }
        }else if (event instanceof Menu.OptionSelectedEvent) {
            logger.finest("Item selected: "+ event);
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
        final Map<String,Object> beans = BeanManager.getInstance().getBeans();
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
                final List<Field> fields = BeanManager.getFields(option.getObject(), BeanProperty.class);
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
                            if (event instanceof ChangeEvent && event.getSource() == GUI.this)
                                setText(String.valueOf(GUI.this.showFPS));
                            return super.onEvent(event);
                        }
                    };
                    field.setAccessible(false);

                    container.addChild(fieldLabel, row, 0);
                    container.addChild(fieldValue, row++, 1);
                    fieldValue.setClickable(true);
                    fieldValue.setOnClick(()->{

                        logger.finest("field onClick: "+field.getType());
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
                                        // FIXME: field.set(option.getObject(), value);
                                        //final Method setter = BeanFactory.getSetter(field);
                                        //setter.invoke(item.getOption().getObject(),value);
                                        dispose();
                                        return true;
                                    }
                                } catch (Exception e) {
                                    logger.log(Level.SEVERE, "Error getting setter for field: "+field.getName(), e);
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
