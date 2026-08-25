package org.the3deer.android.engine.text;

import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.bean.BeanProperty;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

@Bean(category = "rendering")
public class FontManager
{

    @BeanProperty
    private String factory = "font_factory";

    @Inject
    private Map<String, FontFactory> factories;

    /**
     * First time we initialize with the first factory available
     */
    @BeanInit
    private void setUp(){
        if (factory == null && factories != null && !factories.isEmpty()){
            factory = factories.keySet().stream().findFirst().orElse(null);
        }
    }

    /**
     * This is called by the settings manager
     * @return the ids of the factories available
     */
    public Set<String> getFactoryValues(){
        if (factories == null) return Collections.emptySet();
        return factories.keySet();
    }

    public void setFactory(String factory){
        this.factory = factory;
    }

    public FontFactory getFactoryObject(){
        if (factories == null || factories.isEmpty()) return null;
        return factories.get(this.factory);
    }
}
