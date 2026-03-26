package org.the3deer.android.engine;

import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanProperty;

@Bean(name = "Application Settings")
public class UISettings {

    @BeanProperty(name = "language", description = "Application Language", values = {"en", "es"}, valueNames = {"English", "Español"})
    private String language = "en";

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
