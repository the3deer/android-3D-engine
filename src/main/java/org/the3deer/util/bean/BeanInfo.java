package org.the3deer.util.bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Technical summary of a managed Bean for AI and external systems.
 * 
 * @author andresoviedo
 */
public class BeanInfo {
    
    private final String id;
    private String name;
    private String description;
    private String category;
    private String categoryDescription;
    private final Map<String, BeanPropertyInfo> properties = new LinkedHashMap<>();

    public BeanInfo(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryDescription() {
        return categoryDescription;
    }

    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    public Map<String, BeanPropertyInfo> getProperties() {
        return properties;
    }

    public void addProperty(String id, BeanPropertyInfo metadata) {
        this.properties.put(id, metadata);
    }
}
