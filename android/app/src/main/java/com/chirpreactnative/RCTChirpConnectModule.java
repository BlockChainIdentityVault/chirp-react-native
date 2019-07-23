//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.chirpconnect.rctchirpconnect;

import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.io.IOException;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import io.chirp.connect.ChirpConnect;
import io.chirp.connect.interfaces.ConnectEventListener;
import io.chirp.connect.interfaces.ConnectSetConfigListener;
import io.chirp.connect.models.ChirpError;
import io.chirp.connect.models.ChirpConnectState;


public class RCTChirpConnectModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private static final String TAG = "ChirpConnect";
    private ChirpConnect chirpConnect = null;
    private ReactContext context;
    private boolean isStarted = false;

    @Override
    public String getName() {
        return TAG;
    }

    public RCTChirpConnectModule(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;
        reactContext.addLifecycleEventListener(this);
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("CHIRP_CONNECT_NOT_CREATED", ChirpConnectState.CHIRP_CONNECT_STATE_NOT_CREATED.getCode());
        constants.put("CHIRP_CONNECT_STATE_STOPPED", ChirpConnectState.CHIRP_CONNECT_STATE_STOPPED.getCode());
        constants.put("CHIRP_CONNECT_STATE_PAUSED", ChirpConnectState.CHIRP_CONNECT_STATE_PAUSED.getCode());
        constants.put("CHIRP_CONNECT_STATE_RUNNING", ChirpConnectState.CHIRP_CONNECT_STATE_RUNNING.getCode());
        constants.put("CHIRP_CONNECT_STATE_SENDING", ChirpConnectState.CHIRP_CONNECT_STATE_SENDING.getCode());
        constants.put("CHIRP_CONNECT_STATE_RECEIVING", ChirpConnectState.CHIRP_CONNECT_STATE_RECEIVING.getCode());
        return constants;
    }

    /**
     * init(key, secret)
     *
     * Initialise the SDK with an application key and secret.
     * Callbacks are also set up here.
     */
    @ReactMethod
    public void init(String key, String secret) {
        chirpConnect = new ChirpConnect(this.getCurrentActivity(), key, secret);

        chirpConnect.setListener(new ConnectEventListener() {

            @Override
            public void onSending(byte[] data, int channel) {
                WritableMap params = assembleData(data);
                context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onSending", params);
            }

            @Override
            public void onSent(byte[] data, int channel) {
                WritableMap params = assembleData(data);
                context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onSent", params);
            }

            @Override
            public void onReceiving(int channel) {
                WritableMap params = Arguments.createMap();
                context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onReceiving", params);
            }

            @Override
            public void onReceived(byte[] data, int channel) {
                WritableMap params = assembleData(data);
                context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onReceived", params);
            }

            @Override
            public void onStateChanged(int oldState, int newState) {
                WritableMap params = Arguments.createMap();
                params.putInt("status", newState);
                context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onStateChanged", params);
            }

            @Override
            public void onSystemVolumeChanged(float oldVolume, float newVolume) {
            }
        });
    }

    /**
     * setConfigFromNetwork()
     *
     * Fetch default config from the network to configure the SDK.
     */
    @ReactMethod
    public void setConfigFromNetwork(final Promise promise) {
        chirpConnect.setConfigFromNetwork(new ConnectSetConfigListener() {

            @Override
            public void onSuccess() {
                promise.resolve("Initialisation Success");
            }

            @Override
            public void onError(ChirpError setConfigError) {
                promise.reject("Network Error", setConfigError.getMessage());
            }
        });
    }

    /**
     * setConfig(config)
     *
     * Configure the SDK with a config string.
     */
    @ReactMethod
    public void setConfig(String config) {

        ChirpError error = chirpConnect.setConfig(config);
        if (error.getCode() > 0) {
            onError(context, error.getMessage());
        }
    }

    /**
     * start()
     *
     * Starts the SDK.
     */
    @ReactMethod
    public void start() {
        ChirpError error = chirpConnect.start();
        if (error.getCode() > 0) {
            onError(context, error.getMessage());
        } else {
            isStarted = true;
        }
    }

    /**
     * stop()
     *
     * Stops the SDK.
     */
    @ReactMethod
    public void stop() {
        ChirpError error = chirpConnect.stop();
        if (error.getCode() > 0) {
            onError(context, error.getMessage());
        } else {
            isStarted = false;
        }
    }

    /**
     * send(data)
     *
     * Encodes a payload of bytes, and sends to the speaker.
     */
    @ReactMethod
    public void send(ReadableArray data) {
        byte[] payload = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            payload[i] = (byte)data.getInt(i);
        }

        long maxSize = chirpConnect.maxPayloadLength();
        if (maxSize < payload.length) {
            onError(context, "Invalid payload");
            return;
        }
        ChirpError error = chirpConnect.send(payload);
        if (error.getCode() > 0) {
            onError(context, error.getMessage());
        }
    }

    /**
     * sendRandom()
     *
     * Sends a random payload to the speaker.
     */
    @ReactMethod
    public void sendRandom() {
        long maxPayloadLength = chirpConnect.maxPayloadLength();
        long size = (long) new Random().nextInt((int) maxPayloadLength) + 1;
        byte[] payload = chirpConnect.randomPayload((byte)size);

        ChirpError error = chirpConnect.send(payload);
        if (error.getCode() > 0) {
            onError(context, error.getMessage());
        }
    }

    /**
     * asString(data)
     *
     * Returns a payload represented as a hexadecimal string.
     */
    public static String asString(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static WritableMap assembleData(byte[] data) {
        WritableArray payload = Arguments.createArray();
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                payload.pushInt(data[i]);
            }
        }
        WritableMap params = Arguments.createMap();
        params.putArray("data", payload);
        return params;
    }

    private void onError(ReactContext reactContext,
                         String error) {
        WritableMap params = Arguments.createMap();
        params.putString("message", error);
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onError", params);
    }

    @Override
    public void onHostResume() {
        if (chirpConnect != null && isStarted) {
            chirpConnect.start();
        }
    }

    @Override
    public void onHostPause() {
        if (chirpConnect != null) {
            chirpConnect.stop();
        }
    }

    @Override
    public void onHostDestroy() {
        if (chirpConnect != null) {
            chirpConnect.close();
        }
    }
}
