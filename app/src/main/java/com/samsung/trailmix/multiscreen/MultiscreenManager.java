/*******************************************************************************
 * Copyright (c) 2015 Samsung Electronics
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/

package com.samsung.trailmix.multiscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiManager;

import com.samsung.multiscreen.Channel;
import com.samsung.multiscreen.Client;
import com.samsung.multiscreen.Message;
import com.samsung.multiscreen.Result;
import com.samsung.multiscreen.Search;
import com.samsung.multiscreen.Service;
import com.samsung.multiscreen.util.JSONUtil;
import com.samsung.trailmix.App;
import com.samsung.trailmix.multiscreen.events.AppStateEvent;
import com.samsung.trailmix.multiscreen.events.ConnectionChangedEvent;
import com.samsung.trailmix.multiscreen.events.PlaybackEvent;
import com.samsung.trailmix.multiscreen.events.ServiceChangedEvent;
import com.samsung.trailmix.multiscreen.events.VideoStatusEvent;
import com.samsung.trailmix.multiscreen.model.CurrentStatus;
import com.samsung.trailmix.multiscreen.model.MetaData;
import com.samsung.trailmix.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import de.greenrobot.event.EventBus;


/**
 * Provides the Samsung MultiScreen functions.
 */
public class MultiscreenManager {
    public static final String APP_URL = "http://s3-us-west-1.amazonaws.com/dev-multiscreen-examples/examples/trailmix/tv/index.html";
    public static final String CHANNEL_ID = "com.samsung.trailmix";

    public enum ServiceType {
        // Other unknown type
        Other,
        // Samsung smart TV.
        TV,
        // Samsung smart speaker.
        Speaker
    }


    /**
     * The app state event sent from TV app. Payload is a CurrentStatus model.
     */
    public static final String EVENT_APP_STATE = "appState";

    /**
     *  TV (host) publishes to all clients to report any status changes about the video
     *  (current play head time, or play/pause state). Payload is a CurrentStatus model.
     */
    public static final String EVENT_VIDEO_STATUS = "videoStatus";
    /**
     * Published to all clients when the host begins playing a video.
     */
    public static final String EVENT_VIDEO_START = "videoStart";
    /**
     * Published to all clients when the host finishes playing a video
     */
    public static final String EVENT_VIDEO_END = "videoEnd";

    /**
     * The app state command. The EVENT_APP_STATE event should be called later with app status.
     */
    public static final String CMD_APP_STATE = "appStateRequest";
    /**
     * The command to play video.
     */
    public static final String CMD_PLAY = "play";
    /**
     * The command to pause video.
     */
    public static final String CMD_PAUSE = "pause";
    /**
     * The command to stop video.
     */
    public static final String CMD_STOP = "stop";
    /**
     * The command to resume the paused video.
     */
    public static final String CMD_RESUME = "resume";
    /**
     * The command to seek.
     */
    public static final String CMD_SEEK = "seek";
    /**
     * Request TV (host) to replay the current video from the beginning.
     */
    public static final String CMD_REPLAY = "replay";

    /**
     * An singleton instance of this class
     */
    private static MultiscreenManager instance = null;

    /**
     * A lock used to synchronize creation of this object and access to the service map.
     */
    protected static final Object lock = new Object();

    /**
     * The Search object which is going to run discovery service.
     */
    private Search search = null;

    /**
     * Multiscreen TV service
     */
    private com.samsung.multiscreen.Service service;

    /**
     * Multiscreen TV application
     */
    private com.samsung.multiscreen.Application multiscreenApp;

    /**
     * The array list to hold TV services.
     */
    private ArrayList<Service> serviceList = new ArrayList<>();


    /**
     * Returns the instance.
     *
     * @return
     */
    public static MultiscreenManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new MultiscreenManager();
                }
            }
        }
        return instance;
    }

    private MultiscreenManager() {
        // Register Wifi state listener.
        registerWiFiStateListener();
    }

    /**
     * Clean up the service.
     */
    public void release() {
        // Unregister the WiFi state listener.
        try {
            App.getInstance().unregisterReceiver(mWifiStateChangedReceiver);
        } catch (Exception e) {//ignore if there is error.
        }

        // Disconnect TV if it is connected.
        disconnect();

        service = null;
    }

    /**
     * Check if discovery process is running.
     *
     * @return true discovery is running otherwise false.
     */
    public boolean isDiscovering() {
        return (search != null && search.isSearching());
    }

    /**
     * Check if the discovery process is stopping.
     * @return true if it is stopping.
     */
    public boolean isStoppingDiscovery() {
        return (search != null && search.isStopping());
    }

    /**
     * start TV discovery.
     */
    public void startDiscovery() {
        Util.d("startDiscovery");

        // Create the search object if it is null.
        if (search == null) {
            search = Service.search(App.getInstance());
            search.setOnServiceFoundListener(mOnServiceFoundListener);
            search.setOnServiceLostListener(mOnServiceLostListener);
        }

        // When WiFi is connected and search process is not running.
        if (Util.isWiFiConnected() && !isDiscovering()) {

            if (!search.isStarting()) {
                // Clear the TV list.
                removeAllServices();

                // Start discovery.
                search.start();
            }
        }
    }


    /**
     * Stop TV discovery.
     */
    public void stopDiscovery() {
        Util.d("stopDiscovery");

        // Stop discovery if search object it not null and it is search.
        if (isDiscovering()) {
            if (!search.isStopping()) {
                try {
                    search.stop();
                } catch (Exception e) { //ignore any error during stop search.
                }
            }
        }
    }


    /**
     * restart a new discovery.
     */
    public void restartDiscovery() {
        if (search == null) {
            startDiscovery();
            return;
        }

        if (isDiscovering()) {
            // Set the listener. Called when search is completely stopped.
            setDiscoveryOnStopListener(new Search.OnStopListener() {
                @Override
                public void onStop() {

                    //Clear the onStopListener.
                    search.setOnStopListener(null);

                    //Start a new discovery.
                    startDiscovery();
                }
            });

            // Start to stop discovery.
            stopDiscovery();
        } else {
            // There is no search process, start a new discovery.
            startDiscovery();
        }
    }

    private Search.OnServiceFoundListener mOnServiceFoundListener = new Search.OnServiceFoundListener() {
        @Override
        public void onFound(Service service) {
            Util.d("Service onFound: " + service);

            // TV is found, update the service list.
            updateService(service);
        }
    };
    private Search.OnServiceLostListener mOnServiceLostListener = new Search.OnServiceLostListener() {
        @Override
        public void onLost(Service service) {
            // Remove the TV from TV list.
            removeService(service);
        }
    };

    /**
     * Get the services found during discovery.
     */
    public ArrayList<Service> getServiceList() {
        return serviceList;
    }

    /**
     * Remove all the services.
     */
    public void removeAllServices() {
        serviceList.clear();

        // Notify UI to update cast icon.
        EventBus.getDefault().post(new ServiceChangedEvent(null, false));
    }

    /**
     * Update the service. If the service has existed, replace it. Otherwise add a new service.
     * @param service the new service.
     */
    public void updateService(Service service) {
        if (service == null) {
            return;
        }

        // Get the service position.
        int position = serviceList.indexOf(service);

        // Check if position is valid.
        if (position >= 0) {
            serviceList.set(position, service);

            // Notify new service is added.
            EventBus.getDefault().post(new ServiceChangedEvent(service, true));
        } else {
            addService(service);
        }
    }

    /**
     * Add a new service into service list.
     * @param service
     */
    public void addService(Service service) {
        if (service == null) {
            return;
        }

        serviceList.add(service);

        // Notify new service is added.
        EventBus.getDefault().post(new ServiceChangedEvent(service, true));
    }


    /**
     * Remove TV from the TV list.
     *
     * @param service
     */
    private void removeService(Service service) {
        if (service == null) {
            return;
        }

        // Remove the service if it exists.
        if (serviceList.remove(service)) {

            // Notify new service is added.
            EventBus.getDefault().post(new ServiceChangedEvent(service, true));
        }
    }

    /**
     * Connect to given service.
     *
     * @param service the service to be connected. disconnect current service if it is null.
     */
    public void connectToService(final Service service) {
        if (service == null) {
            disconnect();
            return;
        }

        if (this.service != null) {

            // Launch the TV App directly if we already got the TV service.
            if (this.service.equals(service)) {

                // Launch the TV app if it is not connected, otherwise do nothing.
                if (!multiscreenApp.isConnected()) {
                    launchApplication();
                }

            } else {

                // If different TV is selected, disconnect the previous application.
                if (multiscreenApp != null && multiscreenApp.isConnected()) {
                    multiscreenApp.disconnect(new Result<Client>() {
                        @Override
                        public void onSuccess(Client client) {
                            // disconnect onSuccess, update service.
                            updateServiceAndConnect(service);
                        }

                        @Override
                        public void onError(com.samsung.multiscreen.Error error) {
                            // disconnect failed.
                            Util.e("disconnect onError: " + error.getMessage());

                            // Update service.
                            updateServiceAndConnect(service);
                        }
                    });
                } else {
                    updateServiceAndConnect(service);
                }
            }
        } else {
            //connect to a new TV.
            updateServiceAndConnect(service);
        }
    }

    public Service getConnectedService() {
        return service;
    }

    /**
     * Update the current service and start to launch TV application.
     *
     * @param service the new TV service.
     */
    private void updateServiceAndConnect(Service service) {
        this.service = service;

        //Start to launch TV application.
        if (this.service != null) {
            launchApplication();
        }
    }

    /**
     * Check if TV is connected already.
     *
     * @return true if TV is connected otherwise false.
     */
    public boolean isTVConnected() {
        return service != null && multiscreenApp != null && multiscreenApp.isConnected();
    }


    /**
     * Return the service type of the connected service.
     * @return
     */
    public ServiceType getConnectedServiceType() {
        return getServiceType(this.service);
    }

    /**
     * Return the service type of given service.
     * @param service
     * @return
     */
    public ServiceType getServiceType(Service service) {
        if (service == null) {
            return ServiceType.Other;
        }

        String type = service.getType();
        if (type.toLowerCase().endsWith("speaker")) {
            return ServiceType.Speaker;
        }

        return ServiceType.TV;
    }

    /**
     * Makes connection to the TV and start the application on the TV
     * if the current service is available.
     */
    public void launchApplication() {
        if (service == null) {
            return;
        }

        // Parse Application Url.
        Uri url = Uri.parse(APP_URL);

        // Get an instance of Application.
        multiscreenApp = service.createApplication(url, CHANNEL_ID);

        // Set the connection timeout to 20 seconds.
        // When the TV is unavailable after 20 seconds, onDisconnect event is called.
        multiscreenApp.setConnectionTimeout(20000);

        // Listen for the disconnect event.
        multiscreenApp.setOnDisconnectListener(new Channel.OnDisconnectListener() {
            @Override
            public void onDisconnect(Client client) {
                if (client != null) {

                    // Notify service change listeners.
                    EventBus.getDefault().post(new ConnectionChangedEvent(null));
                }
            }
        });

        // Listen for the connect event
        multiscreenApp.setOnConnectListener(new Channel.OnConnectListener() {
            @Override
            public void onConnect(Client client) {

                // Notify to update UI.
                EventBus.getDefault().post(new ConnectionChangedEvent(null));
            }
        });

        // Listen for the errors.
        multiscreenApp.setOnErrorListener(new Channel.OnErrorListener() {
            @Override
            public void onError(com.samsung.multiscreen.Error error) {
                Util.e("setOnErrorListener: " + error.toString());
                EventBus.getDefault().post(new ConnectionChangedEvent(error.getMessage()));
            }
        });

        // Add message listeners.
        multiscreenApp.addOnMessageListener(EVENT_APP_STATE, onAppStateListener);
        multiscreenApp.addOnMessageListener(EVENT_VIDEO_STATUS, onVideoStatusListener);
        multiscreenApp.addOnMessageListener(EVENT_VIDEO_START, onVideoStartListener);
        multiscreenApp.addOnMessageListener(EVENT_VIDEO_END, onVideoEndListener);

        // Connect and launch the TV application.
        // The timeout is 30 seconds.
        multiscreenApp.connect(null, 30000, new Result<Client>() {

            @Override
            public void onSuccess(Client client) {
            }

            @Override
            public void onError(com.samsung.multiscreen.Error error) {
                Util.e("connect onError: " + error.toString());

                // failed to launch TV application. Notify TV service changes.
                EventBus.getDefault().post(new ConnectionChangedEvent(error.getMessage()));
            }
        });
    }

    /**
     * Disconnect the multiscreen web application.
     */
    public void disconnect() {
        if (isTVConnected()) {
            multiscreenApp.removeOnMessageListeners();
            multiscreenApp.disconnect(false, null);
            service = null;
        }
    }

    /**
     * Set the discovery stop listener called when discovery is stopped.
     *
     * @param listener the listener.
     */
    public void setDiscoveryOnStopListener(Search.OnStopListener listener) {
        if (search != null) {
            search.setOnStopListener(listener);
        }
    }


    /**
     * Get the clients amount.
     * @return
     */
    public int getClientCount() {
        int count = 0;

        if (isTVConnected()) {
            count = multiscreenApp.getClients().size();
        }

        return count;
    }

    //===============================Protocol related code======================================

    /**
     * Send the app state request.
     */
    public void requestAppState() {
        sendToTV(CMD_APP_STATE, null, Message.TARGET_HOST);
    }


    /**
     * Pause the playing video.
     */
    public void pause() {
        sendToTV(CMD_PAUSE, null, Message.TARGET_HOST);
    }

    /**
     * Play the video from beginning.
     */
    public void play(MetaData metaData) {
        if (metaData != null) {
            sendToTV(CMD_PLAY, metaData.getJsonObject(), Message.TARGET_HOST);
        }
    }

    /**
     * Play the video from with given start time.
     * @param time the start time in seconds.
     */
    public void play(MetaData metaData, float time, String state) {
        if (metaData != null) {
            JSONObject jo = metaData.getJsonObject();
            try {
                jo.put("time", time);
                jo.put("state", state);
            } catch (JSONException e) {
            }
            sendToTV(CMD_PLAY, jo, Message.TARGET_HOST);
        }
    }

    /**
     * stop the current playing video.
     */
    public void stop() {
        sendToTV(CMD_STOP, null, Message.TARGET_HOST);
    }


    /**
     * resume the video.
     */
    public void resume() {
        sendToTV(CMD_RESUME, null, Message.TARGET_HOST);
    }


    /**
     * replay the video from beginning.
     */
    public void replay() {
        sendToTV(CMD_REPLAY, null, Message.TARGET_HOST);
    }


    /**
     * seek the current playing video.
     */
    public void seek(float position) {
        sendToTV(CMD_SEEK, position, Message.TARGET_HOST);
    }


    /**
     * Receive the response data of app state request.
     */
    private Channel.OnMessageListener onAppStateListener = new Channel.OnMessageListener() {
        @Override
        public void onMessage(Message message) {
            Util.d("onAppStateListener = " + message.toString());

            if (message != null && message.getData() != null) {
                HashMap map = (HashMap)message.getData();
                if (map.containsKey("currentStatus")) {

                    // Read the json string from message map.
                    String jsonString = JSONUtil.toJSONString((HashMap)map.get("currentStatus"));

                    // Broadcast app state event.
                    EventBus.getDefault().post(new AppStateEvent(CurrentStatus.parse(jsonString, CurrentStatus.class)));
                }
            }
        }
    };

    /**
     * Receive the update of track status.
     */
    private Channel.OnMessageListener onVideoStatusListener = new Channel.OnMessageListener() {
        @Override
        public void onMessage(Message message) {
            Util.d("onVideoStatusListener: " + message.toString());

            if (message != null && message.getData() != null) {

                if (message.getData() instanceof HashMap) {
                    String jsonString = JSONUtil.toJSONString((HashMap)message.getData());

                    EventBus.getDefault().post(new VideoStatusEvent(CurrentStatus.parse(jsonString, CurrentStatus.class)));
                }
            }
        }
    };

    /**
     * Receive the track start event.
     */
    private Channel.OnMessageListener onVideoStartListener = new Channel.OnMessageListener() {
        @Override
        public void onMessage(Message message) {
            if (message != null && message.getData() != null) {
                EventBus.getDefault().post(new PlaybackEvent(message.getData().toString(),
                        message.getEvent()));
            }
        }
    };

    /**
     * Receive the track end event.
     */
    private Channel.OnMessageListener onVideoEndListener = new Channel.OnMessageListener() {
        @Override
        public void onMessage(Message message) {
            if (message != null && message.getData() != null) {
                EventBus.getDefault().post(new PlaybackEvent(message.getData().toString(),
                        message.getEvent()));
            }
        }
    };



    /**
     * Sent the data to TV.
     *
     * @param event  the channel event.
     * @param data   the object to sent to TV.
     * @param target the target to receive message.
     */
    private void sendToTV(String event, Object data, String target) {
        if (multiscreenApp != null && multiscreenApp.isConnected()) {
            multiscreenApp.publish(event, data, target);
        }
    }

    //===================================WiFi State Monitor===============================


    /**
     * Register network change listeners.
     */
    private void registerWiFiStateListener() {
        IntentFilter mWiFiStateFilter = new IntentFilter();
        mWiFiStateFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWiFiStateFilter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
        App.getInstance().registerReceiver(mWifiStateChangedReceiver, mWiFiStateFilter);
    }

    /**
     * Broadcast receiver for network changes.
     */
    BroadcastReceiver mWifiStateChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

                switch (extraWifiState) {
                    case WifiManager.WIFI_STATE_DISABLED:
                    case WifiManager.WIFI_STATE_DISABLING:
                        // WiFi is not available.
                        stopDiscovery();

                        // Clear the TV list.
                        removeAllServices();
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        // Use ConnectivityManager.CONNECTIVITY_ACTION instead.
                        break;
                }
            } else if (android.net.ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                // WiFi is connected
                if (Util.isWiFiConnected()) {
                    startDiscovery();
                }
            }
        }
    };
}
