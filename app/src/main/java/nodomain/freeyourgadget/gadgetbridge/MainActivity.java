package nodomain.freeyourgadget.gadgetbridge;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenter;
import nodomain.freeyourgadget.gadgetbridge.activities.DebugActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;



public class MainActivity extends Activity {
    static final String TAG = "LoginActivity";

    private View mLoginView;
    private WebView mWebView;


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case GBApplication.ACTION_QUIT:
                    finish();

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBApplication.ACTION_QUIT);
        filterLocal.addAction(DeviceManager.ACTION_DEVICES_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,filterLocal);
        setContentView(R.layout.activity_main_artik);
        mWebView = (WebView)findViewById(R.id.webview);
        mWebView.setVisibility(View.GONE);
        mLoginView = findViewById(R.id.ask_for_login);
        mLoginView.setVisibility(View.VISIBLE);
        Button button = (Button)findViewById(R.id.btn);


        Log.v(TAG, "::onCreate");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Log.v(TAG, ": button is clicked.");
                    loadWebView();
                } catch (Exception e) {
                    Log.v(TAG, "Run into Exception");
                    e.printStackTrace();
                }
            }
        });

        // Reset to start a new session cleanly
        ArtikCloudSession.getInstance().reset();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,filterLocal);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void loadWebView() {
        Log.v(TAG, "::loadWebView");
        mLoginView.setVisibility(View.GONE);
        mWebView.setVisibility(View.VISIBLE);
        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String uri) {
                if (uri.startsWith(ArtikCloudSession.REDIRECT_URL)) {
                    // Redirect URL has format android-app://redirect#expires_in=1209600&token_type=bearer&access_token=xxxx
                    // Extract OAuth2 access_token in URL
                    if (uri.contains("access_token=")) {
                        String accessToken;
                        String containingStr = uri.split("access_token=")[1];
                        if (containingStr.contains("&")) {
                            accessToken = containingStr.split("&")[0];
                        } else {
                            accessToken = containingStr;
                        }
                        onGetAccessToken(accessToken);
                    }
                    return true;
                }
                // Load the web page from URL (login and grant access)
                return super.shouldOverrideUrlLoading(view, uri);
            }
        });

        String url = ArtikCloudSession.getInstance().getAuthorizationRequestUri();
        Log.v(TAG, "webview loading url: " + url);
        mWebView.loadUrl(url);
    }


    private void onGetAccessToken(String accessToken)
    {
        Log.d(TAG, "onGetAccessToken(" + accessToken +")");
        ArtikCloudSession.getInstance().setAccessToken(accessToken);
        startControlActivity();
    }

    private void startControlActivity() {
        Intent activityIntent = new Intent(this,ControlCenter.class);
        startActivity(activityIntent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.action_debug:
                Intent debugIntent = new Intent(this, DebugActivity.class);
                startActivity(debugIntent);
                return true;
            case R.id.action_quit:
                GBApplication.quit();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }



}