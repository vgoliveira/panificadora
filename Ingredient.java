package com.github.vgoliveira.panificadora;

import java.io.Serializable;

/**
 * Created by vgarcia on 13/09/2016.
 */
public class Ingredient implements Serializable {
    private String name;
    private String[] measure;
    Ingredient(String name){
        this.name = name;
        measure = new String[4];
    }
    void setMeasure(int weight, String measure) {
        if ((weight == Recipe.W450) ||(weight == Recipe.W600)|| (weight == Recipe.W900)||(weight == Recipe.W1200)) {
            this.measure[weight] = measure;
        }
    }
    void setMeasure(String weight, String measure) {
        switch (weight) {
            case "W450":
                this.measure[Recipe.W450] = measure;
                break;
            case "W600":
                this.measure[Recipe.W600] = measure;
                break;
            case "W900":
                this.measure[Recipe.W900] = measure;
                break;
            case "W1200":
                this.measure[Recipe.W1200] = measure;
                break;
        }
    }
    String getMeasure(int weight) {
        if ((weight == Recipe.W450) ||(weight == Recipe.W600)|| (weight == Recipe.W900)||(weight == Recipe.W1200)) {
            return this.measure[weight];
        }
        else {
            return null;
        }
    }
    String getName() {
        return this.name;
    }
}
