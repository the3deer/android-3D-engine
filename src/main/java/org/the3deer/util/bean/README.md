# Bean Model

## API

- @Feature: The Application has several features
- @Bean: Every Feature is implemented using components (beans)
- @BeanProperty: Every Bean may have attributes (properties)

## Annotations

- There may be 1 category for every feature
- There may be 1 category for every bean
- The component attributes may be declared in the Feature, same or upper package 

    |- Application
    |   |- @Feature
    |   |   |- category (eg: "Graphical User Interface", "Decorators", "Renderer")
    |   |   |- description
    |   |   |- experimental
    |   |- @Bean
    |   |   |- category (eg: "Graphical User Interface", "Decorators", "Renderer")
    |   |   |- name
    |   |   |- description
    |   |   |- experimental
    |   |- @Bean
    |   |   |- name
    |   |   |- description
    |   |   |- experimental
    |   |- @BeanProperty
    |   |   |- name
    |   |   |- description
    |   |   |- valueNames
    |   |   |- valuesMethod()

## Bean Factory

- The Bean Factory can be used to assemble an application using CDI
- The Bean Factory manages the Bean list using a Map<String,Object> 
- The Bean Factory supports the @BeanInit post-processor 

## Preference Keys

The key for each preference is constructed as follows:
`<className>.<propertyName>`

## Storing Preferences

Preferences are stored using the following ID strategy:
1. If `valueNames` is provided, the name is used as the stored ID (e.g., "Gray").
2. If values are `String` or `Number`, the value itself is used.
3. Otherwise, the index is used as a fallback.

## API

### Special Properties

#### The `enabled` property
If a property named `enabled` (boolean) exists in a bean, it is treated as a **master toggle** for that component. 
- It will be displayed at the top of the component section.
- All other properties within the same bean will automatically depend on it (i.e., they will be disabled in the UI if `enabled` is set to `false`).

```java
@BeanProperty
protected boolean enabled = true;
```

### Custom Property Values


    @BeanProperty(name = "Background Color", description = "Select the default color for 3D models", valueNames = {"White", "Gray", "Black"})
    private float[] backgroundColor = Constants.COLOR_GRAY;

    @BeanProperty(name = "backgroundColor", valueNames = {"White", "Gray", "Black"})
    public List<float[]> getBackgroundColorValues() {
        return Arrays.asList(Constants.COLOR_WHITE, Constants.COLOR_GRAY, Constants.COLOR_BLACK);
    }

### Delegated Property Values

    @BeanProperty(name = "Property X", description = "My delegated property X")
    public void setSomeFlag(boolean enabled){
        delegate.setSomeFlag(enabled);
    }

    @BeanProperty(description = "My delegated property X")
    public boolean isSomeFlag(){
        delegate.isSomeFlag();
        return false;
    }
