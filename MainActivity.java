
/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.vgoliveira.panificadora;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity implements View.OnTouchListener {

    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final int REQUEST_SELECT_RECIPE = 3;
    public static final int REQUEST_FACEBOOK_LOGIN = 4;
    public static final String TAG = "Panificadora";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;

    private static final int OPERATION = 0x00;
    private static final int NO_OPERATION = 0x00;
    private static final int COMMAND = 0x01;
    private static final int PROGRAM = 0x02;

    private static final int PRESSED = 0x01;
    private static final int NOT_PRESSED = 0x00;

    private static final int TIME_MORE = 0x04;
    private static final int TIME_LESS = 0x05;
    private static final int DOUGH_QNT = 0x02;
    private static final int INIT_STOP = 0x06;
    private static final int OPTIONS = 0x01;
    private static final int COLOR = 0x03;
    private static final int TEST_LED = 0x07;

    byte[] value;
    private boolean isConnected;
    private Bakery bakery;
    private Recipe recipe;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private Button btnConnectDisconnect,time_more, time_less, dough_qnt, options, color, init_stop,startRecipe;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


       mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        btnConnectDisconnect = (Button) findViewById(R.id.btn_select);
        time_more = (Button) findViewById(R.id.button_time_more);
        time_less = (Button) findViewById(R.id.button_time_less);
        dough_qnt = (Button) findViewById(R.id.button_dough_qnt);
        options = (Button) findViewById(R.id.button_options);
        color = (Button) findViewById(R.id.button_color);
        init_stop = (Button) findViewById(R.id.button_init_stop);
        startRecipe = (Button) findViewById(R.id.startRecipesBookSelectionActivity);

        // don´t allow the user click on these buttons if not connected and GATT service discovered.
        time_more.setEnabled(false);
        time_less.setEnabled(false);
        dough_qnt.setEnabled(false);
        options.setEnabled(false);
        color.setEnabled(false);
        init_stop.setEnabled(false);

        isConnected = false;

        service_init();

        // Handler Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();

                        }
                    }
                }
            }
        });

        //Handler startRecipe
        startRecipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent booksIntent = new Intent(MainActivity.this, SelectionListActivity.class);
                booksIntent.putExtra ("file","recipes_books");
                booksIntent.putExtra ("level","books");
                booksIntent.putExtra ("list_title","Livros de receitas");

                booksIntent.putExtra("isConnected", isConnected);
                startActivityForResult(booksIntent, REQUEST_SELECT_RECIPE);
            }
        });

        time_more.setOnTouchListener(this);
        time_less.setOnTouchListener(this);
        options.setOnTouchListener(this);
        dough_qnt.setOnTouchListener(this);
        color.setOnTouchListener(this);
        init_stop.setOnTouchListener(this);

    checkLogin();


    }

    private void checkLogin() {

        Intent facebookIntent = new Intent(MainActivity.this, FacebookLoginActitvity.class);
        //booksIntent.putExtra ("file","recipes_books");

        startActivityForResult(facebookIntent, REQUEST_FACEBOOK_LOGIN);

    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            nfc_init(); // only check for NFC after ServiceConnection is finished;
        }

        public void onServiceDisconnected(ComponentName classname) {
        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();


            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - ready");
                        mState = UART_PROFILE_CONNECTED;
                        isConnected = true;
                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        time_more.setEnabled(false);
                        time_less.setEnabled(false);
                        dough_qnt.setEnabled(false);
                        options.setEnabled(false);
                        color.setEnabled(false);
                        init_stop.setEnabled(false);
                        ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.disconnect();
                        isConnected = false;

                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
               // mService.enableTXNotification();
                time_more.setEnabled(true);
                time_less.setEnabled(true);
                dough_qnt.setEnabled(true);
                options.setEnabled(true);
                color.setEnabled(true);
                init_stop.setEnabled(true);

            }

            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }


        }
    };
    /*
    Known bugs of nfc_init:
        - tap the NFC when app is running breaks the software
        - Does not check if the device is there since .getRemoteDevice(deviceAddress) does not do it
        - sometimes mDevice.getName() is returning null if the app connects to the machine before any manual attempt (using scanlist)
    Possible solution:
        Implements scan here using the MAC Address from tha NFC tag, but only connect if this was found in the scan process
     */
    private void nfc_init(){
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            NdefMessage[] msgs = null;
            Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            String deviceAddress;

            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; ++i) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
                NdefRecord[] records = msgs[0].getRecords();
                deviceAddress = new String(records[1].getPayload(), 1, records[1].getPayload().length-1, Charset.forName("UTF-8")); // record 1 contains the MAC Address
                deviceAddress = deviceAddress.substring(2); // remove the language mark "en" coded in the NDEF text/plain record
                mDevice = mBtAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress); // não testa só retorna mDevice com o endereço passado
                ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                mService.connect(deviceAddress);
                Log.d(TAG, "... NFC_init mDevice= " + mDevice + " mService= " + mService);
                }
            }
        }

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    mService.connect(deviceAddress);


                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case MainActivity.REQUEST_SELECT_RECIPE:
                if (resultCode != Activity.RESULT_CANCELED ) {
                    value = null;
                    bakery = (Bakery) data.getSerializableExtra("bakery");
                    recipe = (Recipe) data.getSerializableExtra("recipe");

                    value = bakery.setProgram(value);

                    new AlertDialog.Builder(this)
                            .setTitle("Atenção")
                            .setMessage("Antes de continuar, cetifique-se que a bandeja foi inserida com todos os ingredientes.")
                            .setPositiveButton("Continuar",new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //send program to the machine
                                    mService.writeRXCharacteristic(value);
                                }
                            })
                            .show();

                    //register this operation in cloud
                }
                break;
            case MainActivity.REQUEST_FACEBOOK_LOGIN:
                Toast.makeText(MainActivity.this, "Return Facebook Login", Toast.LENGTH_SHORT).show();
                String name = data.getStringExtra("name");
                String fbId = data.getStringExtra("id");
                String email = data.getStringExtra("email");
                String gender = data.getStringExtra("gender");
                String ageRange = data.getStringExtra("age_range");
                Toast.makeText(MainActivity.this, "User name: " + name, Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, "Facebook id: " + fbId, Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, "User email: " + email, Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, "Gender: " + gender, Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, "Age range: " + ageRange, Toast.LENGTH_SHORT).show();

                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {

            new AlertDialog.Builder(this)
                    .setTitle("Atenção")
                    .setMessage("Deseja sair da aplicação?")
                    .setPositiveButton("Sim", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mService.close();
                            mService.stopSelf();
                            finish();
                        }
                    })
                    .setNegativeButton("Não", null)
                    .show();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        byte[] value;
        value = new byte[] {NO_OPERATION,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED};

         if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            value[OPERATION] = COMMAND;
            switch(view.getId()) {
                case R.id.button_time_more:
                    value[TIME_MORE] = PRESSED;
                    break;
                case R.id.button_time_less:
                    value[TIME_LESS] = PRESSED;
                    break;
                case R.id.button_dough_qnt :
                    value[DOUGH_QNT] = PRESSED;
                    break;
                case R.id.button_options:
                    value[OPTIONS] = PRESSED;
                    break;
                case R.id.button_color:
                    value[COLOR] = PRESSED;
                    break;
                case R.id.button_init_stop:
                    value[INIT_STOP] = PRESSED;
                    break;
            }

            mService.writeRXCharacteristic(value);
            view.setBackgroundColor(0xFFFDFBB3);
        }
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            value[OPERATION] = COMMAND;

            switch(view.getId()) {
                case R.id.button_time_more:
                    value[TIME_MORE] = NOT_PRESSED;
                    break;
                case R.id.button_time_less:
                    value[TIME_LESS] = NOT_PRESSED;
                    break;
                case R.id.button_dough_qnt :
                    value[DOUGH_QNT] = NOT_PRESSED;
                    break;
                case R.id.button_options:
                    value[OPTIONS] = NOT_PRESSED;
                    break;
                case R.id.button_color:
                    value[COLOR] = NOT_PRESSED;
                    break;
                case R.id.button_init_stop:
                    value[INIT_STOP] = NOT_PRESSED;
                    break;
            }

            mService.writeRXCharacteristic(value);
            view.setBackgroundColor(0xFFCAC7C7);
        }
        return true;
    }

}