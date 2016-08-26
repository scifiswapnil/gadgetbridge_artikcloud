package nodomain.freeyourgadget.gadgetbridge;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cloud.artik.api.UsersApi;
import cloud.artik.model.Acknowledgement;
import cloud.artik.model.ActionDetails;
import cloud.artik.model.ActionDetailsArray;
import cloud.artik.model.ActionIn;
import cloud.artik.model.ActionOut;
import cloud.artik.model.MessageAction;
import cloud.artik.model.MessageIn;
import cloud.artik.model.MessageOut;
import cloud.artik.model.RegisterMessage;
import cloud.artik.model.WebSocketError;
import cloud.artik.websocket.ArtikCloudWebSocketCallback;
import cloud.artik.websocket.DeviceChannelWebSocket;


public class ArtikCloudSession {
    private final static String TAG = "sw";

    // Copy from the corresponding application in the Developer Dashboard
    public static final String CLIENT_ID = "b64d9a9f0c7b4a19b98644741b441ebb";

    // Copy from the Device Info screen in My ARTIK Cloud
    private final static String DEVICE_ID = "a49a44e349cb4ed5be4d196d392734cb";

    private static final String ARTIK_CLOUD_AUTH_BASE_URL = "https://accounts.artik.cloud";
    public static final String REDIRECT_URL = "android-app://redirect";

    private final static String DEVICE_NAME = "Intel Edison";
    private final static String ACTION_NAME_ON = "buzzer_on";
    private final static String ACTION_NAME_OFF = "buzzer_off";


    private static ArtikCloudSession ourInstance = new ArtikCloudSession();
    private static Context ourContext;

    public final static String WEBSOCKET_LIVE_ONOPEN =
            "cloud.artik.example.iot.WEBSOCKET_LIVE_ONOPEN";
    public final static String WEBSOCKET_LIVE_ONMSG =
            "cloud.artik.example.iot.WEBSOCKET_LIVE_ONMSG";
    public final static String WEBSOCKET_LIVE_ONCLOSE =
            "cloud.artik.example.iot.WEBSOCKET_LIVE_ONCLOSE";
    public final static String WEBSOCKET_LIVE_ONERROR =
            "cloud.artik.example.iot.WEBSOCKET_LIVE_ONERROR";
    public final static String WEBSOCKET_WS_ONOPEN =
            "cloud.artik.example.iot.WEBSOCKET_WS_ONOPEN";
    public final static String WEBSOCKET_WS_ONREG =
            "cloud.artik.example.iot.WEBSOCKET_WS_ONREG";
    public final static String WEBSOCKET_WS_ONMSG =
            "cloud.artik.example.iot.WEBSOCKET_WS_ONMSG";
    public final static String WEBSOCKET_WS_ONACK =
            "cloud.artik.example.iot.WEBSOCKET_WS_ONACK";
    public final static String WEBSOCKET_WS_ONCLOSE =
            "cloud.artik.example.iot.WEBSOCKET_WS_ONCLOSE";
    public final static String WEBSOCKET_WS_ONERROR =
            "cloud.artik.example.iot.WEBSOCKET_WS_ONERROR";
    public final static String SDID = "sdid";
    public final static String DEVICE_DATA = "data";
    public final static String TIMESTEP = "ts";
    public final static String ACK = "ack";
    public final static String ERROR = "error";

    private UsersApi mUsersApi = null;
    private String mAccessToken = null;
    private String mUserId = null;




    private DeviceChannelWebSocket mDeviceChannelWS = null; // end point: /websocket

    public static ArtikCloudSession getInstance() {
        return ourInstance;
    }

    private ArtikCloudSession() {
        // Do nothing
    }

    public void setContext(Context context) {
        ourContext = context;
    }

    public String getDeviceID() {
        return DEVICE_ID;
    }

    public String getDeviceName() {
        return DEVICE_NAME;
    }

    public void setAccessToken(String token) {
        if (token == null || token.length() <= 0) {
            Log.e(TAG, "Attempt to set an invalid token");
            mAccessToken = null;
            return;
        }
        mAccessToken = token;
    }

    public String getAuthorizationRequestUri() {
        //https://accounts.artik.cloud/authorize?client=mobile&client_id=xxxx&response_type=token&redirect_uri=http://localhost:8000/acdemo/index.php
        return ARTIK_CLOUD_AUTH_BASE_URL + "/authorize?client=mobile&response_type=token&" +
                "client_id=" + CLIENT_ID + "&redirect_uri=" + REDIRECT_URL;
    }

    public void reset() {
        mUsersApi = null;
        mAccessToken = null;
        mUserId = null;
        mDeviceChannelWS = null;
    }

    private void createDeviceChannelWebSockets() {
        try {
            mDeviceChannelWS = new DeviceChannelWebSocket(true, new ArtikCloudWebSocketCallback() {
                @Override
                public void onOpen(int i, String s) {
                    Log.d(TAG, "Registering " + DEVICE_ID);
                    final Intent intent = new Intent(WEBSOCKET_WS_ONOPEN);
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);

                    RegisterMessage registerMessage = new RegisterMessage();
                    registerMessage.setAuthorization("bearer " + mAccessToken);
                    registerMessage.setCid("myRegisterMessage");
                    registerMessage.setSdid(DEVICE_ID);

                    try {
                        Log.d(TAG, "DeviceChannelWebSocket::onOpen: registering" + DEVICE_ID);
                        mDeviceChannelWS.registerChannel(registerMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(MessageOut messageOut) {
                    Log.d(TAG, "DeviceChannelWebSocket::onMessage(" + messageOut.toString());
                    final Intent intent = new Intent(WEBSOCKET_WS_ONMSG);
                    intent.putExtra(ACK, messageOut.toString());
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onAction(ActionOut actionOut) {

                }

                @Override
                public void onAck(Acknowledgement acknowledgement) {
                    Log.d(TAG, "DeviceChannelWebSocket::onAck(" + acknowledgement.toString());
                    Intent intent;
                    if (acknowledgement.getMessage() != null && acknowledgement.getMessage().equals("OK")) {
                        intent = new Intent(WEBSOCKET_WS_ONREG);
                    } else {
                        intent = new Intent(WEBSOCKET_WS_ONACK);
                        intent.putExtra(ACK, acknowledgement.toString());
                    }
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);

                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    final Intent intent = new Intent(WEBSOCKET_WS_ONCLOSE);
                    intent.putExtra(ERROR, "mWebSocket is closed. code: " + code + "; reason: " + reason);
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);

                }

                @Override
                public void onError(WebSocketError error) {
                    final Intent intent = new Intent(WEBSOCKET_WS_ONERROR);
                    intent.putExtra(ERROR, "mWebSocket error: " + error.getMessage());
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onPing(long timestamp) {
                    Log.d(TAG, "DeviceChannelWebSocket::onPing: " + timestamp);
                }
            });
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes a websocket /websocket connection
     */
    public void disconnectDeviceChannelWS() {
        if (mDeviceChannelWS != null) {
            try {
                mDeviceChannelWS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mDeviceChannelWS = null;
    }

    public void connectDeviceChannelWS() {
        createDeviceChannelWebSockets();
        try {
            mDeviceChannelWS.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendRTsteps(String steps,String totsteps) {
        new sendMsgInBackground().execute(steps,totsteps);
    }

    public void sendTsteps() {
        new sendActionInBackground().execute();
    }

    /*
     * Example of Action sent to ARTIK Cloud over /websocket endpoint
     *  {
        cid:  setOff
        data: {"RT_steps":393,"Total_steps":894}
        ddid:  fde8715961f84798a841be23480b8ce5
        sdid:  null
        ts:   1451606965889
        }

     *
     */


    private void sendActionInDeviceChannelWS() {

        ActionIn actionIn = new ActionIn();
        ActionDetails action0 = new ActionDetails();
        ArrayList<ActionDetails> actions = new ArrayList<>();
        ActionDetailsArray actionDetailsArray = new ActionDetailsArray();

        Map<String,Object> map0 = new HashMap<String, Object>();
        map0.put("RT_steps",Integer.parseInt("12"));
        map0.put("Total_steps",Integer.parseInt("123"));

        action0.setName("data");
        action0.setParameters(map0);
        actions.add(action0);

        actionDetailsArray.setActions(actions);
        actionIn.setData(actionDetailsArray);
        actionIn.setCid("te");
        actionIn.setSdid(DEVICE_ID);
        actionIn.setTs(System.currentTimeMillis());
        Log.d(TAG, "DeviceChannelWebSocket sendAction:" + actionIn.toString());

        try {
            mDeviceChannelWS.sendAction(actionIn);
            Log.d(TAG, "DeviceChannelWebSocket sendAction:" + actionIn.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    class sendActionInBackground extends AsyncTask<String, Void, Void> {
        final static String TAG = "sendActionInBackground";
        @Override
        protected Void doInBackground(String... actionName) {
            try {
                sendActionInDeviceChannelWS();
            } catch (Exception e) {
                Log.v(TAG, "::doInBackground run into Exception");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Do nothing!
        }
    }

    /*
     * Example of messagse sent to ARTIK Cloud over /websocket endpoint

     *
     */
    private void sendMsgInDeviceChannelWS(String steps,String totsteps) {
        MessageIn messagein = new MessageIn();
        Map<String,Object> map0 = new HashMap<String, Object>();
        map0.put("RT_steps",Integer.parseInt(steps));
        map0.put("Total_steps",Integer.parseInt(totsteps));
        messagein.setCid("temp");
        messagein.setData(map0);
        messagein.setDdid(DEVICE_ID);
        messagein.setSdid(DEVICE_ID);
        messagein.setTs(System.currentTimeMillis());

        Log.d("sw", "DeviceChannelWebSocket sendmsg:" + messagein.toString());

        try {
            mDeviceChannelWS.sendMessage(messagein);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    class sendMsgInBackground extends AsyncTask<String, Void, Void> {
        final static String TAG = "sendActionInBackground";
        @Override
        protected Void doInBackground(String... actionName) {
            try {
                sendMsgInDeviceChannelWS(actionName[0],actionName[1]);
            } catch (Exception e) {
                Log.v(TAG, "::doInBackground run into Exception");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Do nothing!
        }
    }



}