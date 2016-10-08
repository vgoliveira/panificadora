package com.github.vgoliveira.panificadora;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


import java.io.InputStream;
import java.util.ArrayList;

public class SelectionListActivity extends ListActivity {

    private ArrayList<String> bookTitles;
    private ArrayList<String> recipeTitles;
    private ArrayList<String> bookFiles;
    private ArrayList<String> availableWeights;
    private TextView listTitle;
    private ArrayAdapter<String> listAdapter;
    private XMLPullParserHandler fetcher;
    private String level = "";
    private Bundle parameters;
    private Recipe recipe;
    private int selected_weight;
    private Bundle bundle;
    private boolean isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_selection);
        listTitle = (TextView) findViewById(R.id.listTitle);
        parameters = getIntent().getExtras();
        isConnected = parameters.getBoolean("isConnected");
        InputStream is;

        level = parameters.getString("level");
        switch(level) {
            case "books":
                listTitle.setText(parameters.getString("list_title"));
                is = getResources().openRawResource(getResources().getIdentifier(parameters.getString("file"),"raw", getPackageName()));
                fetcher = new XMLPullParserHandler(is);
                bookTitles = fetcher.parseForTagList("title");
                bookFiles = fetcher.parseForTagList("filename");
                listAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item, bookTitles);
                setListAdapter(listAdapter);
                break;
            case "recipes":
                listTitle.setText(parameters.getString("list_title"));
                is = getResources().openRawResource(getResources().getIdentifier(parameters.getString("file"),"raw", getPackageName()));
                fetcher = new XMLPullParserHandler(is);
                recipeTitles = fetcher.parseForTagList("title");
                listAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item, recipeTitles);
                setListAdapter(listAdapter);
                break;
            case "weights":
                listTitle.setText(parameters.getString("list_title"));
                availableWeights = new ArrayList<>();
                boolean[] weights = parameters.getBooleanArray("available_weights");
                if(weights[Recipe.W450]){
                    availableWeights.add("450 gramas");
                }
                if(weights[Recipe.W600]){
                    availableWeights.add("600 gramas");
                }
                if(weights[Recipe.W900]){
                    availableWeights.add("900 gramas");
                }
                if(weights[Recipe.W1200]){
                    availableWeights.add("1200 gramas");
                }
                listAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item, availableWeights);
                setListAdapter(listAdapter);

                break;

        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        switch(level) {
            case "books":
                String filename = bookFiles.get(bookTitles.indexOf(getListAdapter().getItem(position).toString()));
                Intent recipesIntent = new Intent(SelectionListActivity.this, SelectionListActivity.class);
                recipesIntent.putExtra("isConnected", isConnected);
                recipesIntent.putExtra("file", filename);
                recipesIntent.putExtra("level", "recipes");
                recipesIntent.putExtra ("list_title",getListAdapter().getItem(position).toString());
                startActivityForResult(recipesIntent, MainActivity.REQUEST_SELECT_RECIPE);
                break;
            case "recipes":
                String test = getListAdapter().getItem(position).toString();
                recipe = fetcher.parseForRecipe(getListAdapter().getItem(position).toString());
                Intent weigthtsIntent = new Intent(SelectionListActivity.this, SelectionListActivity.class);
                bundle = new Bundle();
                bundle.putSerializable("recipe",recipe);
                weigthtsIntent.putExtra("isConnected", isConnected);
                weigthtsIntent.putExtra("level", "weights");
                weigthtsIntent.putExtra ("list_title",recipe.getTitle()+": escolha o peso");
                weigthtsIntent.putExtra("available_weights", recipe.getAvailableWeight());
                weigthtsIntent.putExtras(bundle); // problema
                startActivityForResult(weigthtsIntent, MainActivity.REQUEST_SELECT_RECIPE);
                break;
            case "weights":
                Intent recipeIntent = new Intent(SelectionListActivity.this, RecipeActivity.class);
                recipe = (Recipe)getIntent().getSerializableExtra("recipe");
                bundle = new Bundle();
                bundle.putSerializable("recipe",recipe);
                recipeIntent.putExtra("isConnected", isConnected);
                recipeIntent.putExtra("selectedWeight", getListAdapter().getItem(position).toString());
                recipeIntent.putExtras(bundle);
                startActivityForResult(recipeIntent, MainActivity.REQUEST_SELECT_RECIPE);
                break;
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case MainActivity.REQUEST_SELECT_RECIPE:
                if (resultCode != Activity.RESULT_CANCELED) {
                    Bakery bakery;
                    bakery = (Bakery)data.getSerializableExtra("bakery");
                    recipe = (Recipe) data.getSerializableExtra("recipe");

                    Intent result = new Intent();
                    Bundle b = new Bundle();
                    b.putSerializable("bakery", bakery);
                    b.putSerializable("recipe", recipe);
                    result.putExtras(b);
                    setResult(MainActivity.REQUEST_SELECT_RECIPE, result);
                    finish();
                }
                break;
        }
    }
}
