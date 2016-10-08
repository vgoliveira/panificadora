package com.github.vgoliveira.panificadora;

//https://www.sitepoint.com/integrating-the-facebook-api-with-android/

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class FacebookLoginActitvity extends FragmentActivity {

    private LoginButton loginButton;
    private CallbackManager callbackManager;
//    private AccessTokenTracker accessTokenTracker;
//    private ProfileTracker profileTracker;
    public Profile profile;
    public AccessToken accessToken;

    private FacebookCallback<LoginResult> callback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext()); //must be called before setContentView
        setContentView(R.layout.activity_facebook_login_actitvity);

        LoginButton loginButton = (LoginButton)findViewById(R.id.login_button);

        //https://developers.facebook.com/docs/facebook-login/permissions#reference
        loginButton.setReadPermissions("public_profile");
        loginButton.setReadPermissions("email");


        callbackManager = CallbackManager.Factory.create();
        callback = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                accessToken = loginResult.getAccessToken();

                Set<String> permissions = accessToken.getPermissions();

                profile = Profile.getCurrentProfile();

                GraphRequest request = GraphRequest.newMeRequest(
                        accessToken, new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(
                                    JSONObject object,
                                    GraphResponse response) {

                                String email = null;
                                String gender = null;
                                String locale = null;
                                String ageRange = null;
                                String name = null;
                                String fbId = null;

                                try {

                                    fbId = object.getString("id");
                                    name =  object.getString("name");
                                    email = object.getString("email");
                                    gender= object.getString("gender");
                                    ageRange = object.getString("age_range");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Intent result = new Intent();
                                Bundle b = new Bundle();
                                b.putString("name",name);
                                b.putString("id",fbId);
                                b.putString("email",email);
                                b.putString("gender",gender);
                                b.putString("age_range",ageRange);
                                result.putExtras(b);
                                setResult(MainActivity.REQUEST_FACEBOOK_LOGIN, result);
                                finish();
                            }
                        });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender,age_range");
                request.setParameters(parameters);
                request.executeAsync();
            }
            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException e) {
            }
        };

        loginButton.registerCallback(callbackManager, callback);
        //Toast.makeText(getApplicationContext(), "Logging in...", Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);
        //Facebook login
        callbackManager.onActivityResult(requestCode, responseCode, intent);

    }

}