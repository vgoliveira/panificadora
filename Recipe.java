package com.github.vgoliveira.panificadora;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by vgarcia on 12/09/2016.
 */
public class Recipe implements Serializable {
    //constants
    public static final int W450 = 0;
    public static final int W600 = 1;
    public static final int W900 = 2;
    public static final int W1200 = 3;

    private boolean[] weights;
    private String id;
    private String title;
    private ArrayList<Ingredient> ingredients;
    private String description;
    private String option;

    Recipe() {
        weights = new boolean[4];
        weights[W450] = false;
        weights[W600] = false;
        weights[W900] = false;
        weights[W1200] = false;

        ingredients = new ArrayList<Ingredient>();
    }
    Recipe(String title){
        this();
        this.title = title;
    }
    Recipe(String id, String title){
        this(title);
        this.id = id;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setId(String id) {
        this.id = id;
    }
    public void setOption(String option) {
        this.option = option;
    }
    public void setAvailableWeight(int weight) {
        if ((weight == W450) ||(weight == W600)|| (weight == W900)||(weight == W1200)) {
            this.weights[weight] = true;
        }
    }
    public void setAvailableWeight(String weight) {
        switch (weight) {
            case "W450":
                this.weights[W450] = true;
                break;
            case "W600":
                this.weights[W600] = true;
                break;
            case "W900":
                this.weights[W900] = true;
                break;
            case "W1200":
                this.weights[W1200] = true;
                break;
        }
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void addIngredient (Ingredient ingredient) {
        ingredients.add(ingredient);
    }
    public String getOption() {
        return this.option;
    }
    public boolean getAvailableWeight(int weight) {
        if ((weight == W450) ||(weight == W600)|| (weight == W900)||(weight == W1200)) {
            return this.weights[weight];
        }
        else {
            return false;
        }
    }
    public boolean[] getAvailableWeight(){
        return weights;
    }
    public Ingredient getIngredient (int index) {
        return ingredients.get(index);
    }
    public int getNumberofIngredients () {
        return ingredients.size();
    }
    public String getTitle() {
        return this.title;
    }
    public String getId() {
        return this.id;
    }
    public String getDescription() {
        return this.description;
    }
}