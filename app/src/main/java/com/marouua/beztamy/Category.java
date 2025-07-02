package com.marouua.beztamy;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Category {
    private String id;
    private String name;
    private String icon;
    private String color;
    private String type;

    public Category() {
        // Constructeur vide requis pour Firestore
    }

    public Category(String id, String name, String icon, String color, String type) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.type = type;
    }

    // Getters et setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
