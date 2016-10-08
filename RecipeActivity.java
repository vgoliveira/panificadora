package com.github.vgoliveira.panificadora;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

public class RecipeActivity extends Activity {

    Bakery bakery;
    Recipe recipe;
    Ingredient ingredient;
    private TextView recipeTitle, recipeBody;
    private Switch timerEnabler;
    private Button programRecipe;
    private TimePicker timer;
    private Bundle parameters;
    private RadioButton colorLight, colorMedium, colorDark;
    private int selectedWeight;
    private boolean isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        recipeTitle = (TextView) findViewById(R.id.recipeTitle);
        recipeBody = (TextView) findViewById(R.id.recipeBody);
        timerEnabler = (Switch) findViewById(R.id.timeEnabler);
        timer = (TimePicker) findViewById(R.id.timePicker);
        programRecipe = (Button) findViewById(R.id.programRecipe);
        colorLight = (RadioButton) findViewById(R.id.colorLight);
        colorMedium = (RadioButton) findViewById(R.id.colorMedium);
        colorDark = (RadioButton) findViewById(R.id.colorDark);
        programRecipe = (Button) findViewById((R.id.programRecipe));

        timer.setIs24HourView(true);
        timer.setEnabled(false);

        timerEnabler.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    timer.setEnabled(true);
                    timer.setCurrentHour(bakery.getMinHour());
                    timer.setCurrentMinute(bakery.getMinMinute());
                    bakery.timerChecked(true);

                } else {
                    timer.setEnabled(false);
                    bakery.timerChecked(false);
                }
            }
        });

        recipe = (Recipe) getIntent().getSerializableExtra("recipe");
        parameters = getIntent().getExtras();
        isConnected = parameters.getBoolean("isConnected");

        switch (parameters.getString("selectedWeight")) {
            case "450 gramas":
                selectedWeight = Recipe.W450;
                break;
            case "600 gramas":
                selectedWeight = Recipe.W600;
                break;
            case "900 gramas":
                selectedWeight = Recipe.W900;
                break;
            case "1200 gramas":
                selectedWeight = Recipe.W1200;
                break;
        }

        bakery = new Bakery(recipe.getOption(), selectedWeight);

        displayRecipe();
        setButtonsAvailability();

        // Handler program recipe
        programRecipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (bakery.isColorOptionAvailable()) {
                    if (colorLight.isChecked()) {
                        bakery.setColor(bakery.COLORLIGHT);
                    } else if (colorMedium.isChecked()) {
                        bakery.setColor(bakery.COLORMEDIUM);
                    } else if (colorDark.isChecked()) {
                        bakery.setColor(bakery.COLORDARK);
                    }
                }
                if (timerEnabler.isChecked()) {
                    int i = bakery.setTimer(timer.getCurrentHour(), timer.getCurrentMinute());
                    if (i == bakery.TIMETOOSHORT) {
                        Toast.makeText(RecipeActivity.this, "Tempo para terminar é menor que o tempo da receita", Toast.LENGTH_LONG).show();
                        return;

                    } else if (i == bakery.TIMETOOLONG) {
                        Toast.makeText(RecipeActivity.this, "Tempo para terminar é maior que o suportado pela máquina", Toast.LENGTH_LONG).show();
                        return;

                    } else if (i == bakery.TIMEOK) {
                        //Toast.makeText(RecipeActivity.this, "Tempo ok", Toast.LENGTH_SHORT).show();
                        bakery.setTimer(timer.getCurrentHour(), timer.getCurrentMinute());

                    } else if (i == bakery.TIMERNOTAVAILABLE) {
                        Toast.makeText(RecipeActivity.this, "Timer não está disponível para esta receita", Toast.LENGTH_LONG).show();
                    }
                }
                if(isConnected) {
                    Intent result = new Intent();
                    Bundle b = new Bundle();
                    b.putSerializable("bakery", bakery);
                    b.putSerializable("recipe", recipe);
                    result.putExtras(b);
                    setResult(MainActivity.REQUEST_SELECT_RECIPE, result);
                    finish();
                }
                else {
                    Toast.makeText(RecipeActivity.this, "Não está conectado com a máquina", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void displayRecipe() {
        recipeTitle.setText(recipe.getTitle());

        //fill the recipe body
        recipeBody.setText("\nReceita para " + parameters.getString("selectedWeight") + ":\n\n");
        for (int i = 0; i < recipe.getNumberofIngredients(); i++) {
            ingredient = recipe.getIngredient(i);
            recipeBody.setText(recipeBody.getText() + "- " + ingredient.getName() + " : " + ingredient.getMeasure(selectedWeight) + "\n");

        }
        recipeBody.setText(recipeBody.getText() + "\nModo de preparo:\n" + recipe.getDescription());
        recipeBody.setText(recipeBody.getText() + "\n\nTempo de preparo:\n" + bakery.getHour() + ":" + bakery.getMinute());
        if (bakery.getMinute() == 0) {
            recipeBody.setText(recipeBody.getText() + "0");
        }
        if (!bakery.isColorOptionAvailable() || !bakery.isTimerAvailable()) {
            recipeBody.setText(recipeBody.getText() + "\n\nObservações:");
            if (!bakery.isColorOptionAvailable()) {
                recipeBody.setText(recipeBody.getText() + "\nEsta receita não suporta seleção de cor");
            }
            if (!bakery.isTimerAvailable()) {
                recipeBody.setText(recipeBody.getText() + "\nEsta receita não suporta o uso do timer");
            }
        }
    }

    public void setButtonsAvailability() {
        if (!bakery.isColorOptionAvailable()) {
            this.colorLight.setEnabled(false);
            this.colorMedium.setEnabled(false);
            this.colorDark.setEnabled(false);

        }
        if (!bakery.isTimerAvailable()) {
            this.timerEnabler.setEnabled(false);

        }
    }
}