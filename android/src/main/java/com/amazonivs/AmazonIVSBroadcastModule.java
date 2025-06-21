package com.amazonivs;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import androidx.annotation.NonNull;

import com.amazonaws.ivs.broadcast.BroadcastConfiguration;
import com.amazonaws.ivs.broadcast.BroadcastConfiguration.AspectMode;
import com.amazonaws.ivs.broadcast.BroadcastConfiguration.Video;
import com.amazonaws.ivs.broadcast.BroadcastConfiguration.Audio;
import com.amazonaws.ivs.broadcast.BroadcastConfiguration.Mixer;
import com.amazonaws.ivs.broadcast.BroadcastSession;
import com.amazonaws.ivs.broadcast.BroadcastSession.State;
import com.amazonaws.ivs.broadcast.BroadcastSession.Listener;
import com.amazonaws.ivs.broadcast.Device;
import com.amazonaws.ivs.broadcast.DeviceDiscovery;
import com.amazonaws.ivs.broadcast.ImageDevice;
import com.amazonaws.ivs.broadcast.Microphone;
import com.amazonaws.ivs.broadcast.TransmissionStats;

import java.util.List;

public class AmazonIVSBroadcastModule extends ReactContextBaseJavaModule {
    public static BroadcastSession sharedSession;
    private BroadcastSession broadcastSession;
    private DeviceDiscovery deviceDiscovery;
    private Device currentCamera;
    private Device currentMicrophone;
    private final ReactApplicationContext reactContext;

    public AmazonIVSBroadcastModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.deviceDiscovery = new DeviceDiscovery(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return "AmazonIVSBroadcastModule";
    }

    private void sendEvent(String eventName, Object params) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    private final Listener sessionListener = new Listener() {
        @Override
        public void onStateChanged(@NonNull BroadcastSession session, @NonNull State state) {
            WritableMap map = Arguments.createMap();
            map.putString("state", state.name());
            sendEvent("stateChanged", map);
        }

        @Override
        public void onError(@NonNull BroadcastSession session, @NonNull Exception e) {
            WritableMap map = Arguments.createMap();
            map.putString("message", e.getMessage());
            map.putString("code", e.getClass().getSimpleName());
            map.putBoolean("isFatal", e instanceof BroadcastSession.FatalError);
            sendEvent("error", map);
        }

        @Override
        public void onDeviceChange(@NonNull BroadcastSession session, @NonNull List<Device> devices) {
            WritableArray arr = Arguments.createArray();
            for (Device d : devices) {
                WritableMap dev = Arguments.createMap();
                dev.putString("id", d.getDescriptor().getUid());
                dev.putString("name", d.getDescriptor().getName());
                dev.putString("position", d.getDescriptor().getPosition().name());
                dev.putString("type", d.getType().name());
                arr.pushMap(dev);
            }
            sendEvent("deviceChanged", arr);
        }

        @Override
        public void onBitrateChanged(@NonNull BroadcastSession session, int bitrate) {
            WritableMap map = Arguments.createMap();
            map.putInt("bitrate", bitrate);
            sendEvent("bitrateChanged", map);
        }

        @Override
        public void onNetworkHealthChanged(@NonNull BroadcastSession session, @NonNull TransmissionStats.NetworkHealth health) {
            WritableMap map = Arguments.createMap();
            map.putString("networkHealth", health.name());
            sendEvent("networkHealthChanged", map);
        }

        @Override
        public void onAudioSessionInterrupted(@NonNull BroadcastSession session) {
            sendEvent("audioSessionInterrupted", null);
        }

        @Override
        public void onAudioSessionResumed(@NonNull BroadcastSession session) {
            sendEvent("audioSessionResumed", null);
        }

        @Override
        public void onReconnectStart(@NonNull BroadcastSession session) {
            sendEvent("reconnecting", null);
        }

        @Override
        public void onReconnectSuccess(@NonNull BroadcastSession session) {
            sendEvent("reconnected", null);
        }
    };

    @ReactMethod
    public void startBroadcast(ReadableMap options, Promise promise) {
        if (broadcastSession != null) {
            promise.reject("E_ALREADY_RUNNING", "Yayın zaten başlatılmış.");
            return;
        }
        if (!options.hasKey("rtmpsUrl") || !options.hasKey("streamKey")) {
            promise.reject("E_MISSING_PARAMS", "rtmpsUrl ve streamKey gereklidir");
            return;
        }

        String rtmpsUrl = options.getString("rtmpsUrl");
        String streamKey = options.getString("streamKey");

        try {
            BroadcastConfiguration config = createBroadcastConfig(options);
            broadcastSession = new BroadcastSession(reactContext, deviceDiscovery, config, sessionListener);
            sharedSession = broadcastSession;
            broadcastSession.start(rtmpsUrl, streamKey);
            promise.resolve(null);
        } catch (Exception e) {
            promise.reject("E_BROADCAST_START", "Yayın başlatılamadı: " + e.getMessage(), e);
        }
    }

    private BroadcastConfiguration createBroadcastConfig(ReadableMap options) {
        BroadcastConfiguration config = new BroadcastConfiguration();

        // Video ayarları
        if (options.hasKey("video")) {
            ReadableMap videoConfig = options.getMap("video");
            Video video = config.video;
            
            if (videoConfig.hasKey("width") && videoConfig.hasKey("height")) {
                video.setSize(
                    videoConfig.getInt("width"),
                    videoConfig.getInt("height")
                );
            }
            
            if (videoConfig.hasKey("bitrate")) {
                video.setInitialBitrate(videoConfig.getInt("bitrate"));
            }
            
            if (videoConfig.hasKey("targetFramerate")) {
                video.setTargetFramerate(videoConfig.getInt("targetFramerate"));
            }
            
            if (videoConfig.hasKey("keyframeInterval")) {
                video.setKeyframeInterval(videoConfig.getInt("keyframeInterval"));
            }
        }

        // Audio ayarları
        if (options.hasKey("audio")) {
            ReadableMap audioConfig = options.getMap("audio");
            Audio audio = config.audio;
            
            if (audioConfig.hasKey("bitrate")) {
                audio.setBitrate(audioConfig.getInt("bitrate"));
            }
            
            if (audioConfig.hasKey("channels")) {
                audio.setChannels(audioConfig.getInt("channels"));
            }
        }

        // Mixer ayarları
        if (options.hasKey("mixer")) {
            ReadableMap mixerConfig = options.getMap("mixer");
            Mixer mixer = config.mixer;
            
            if (mixerConfig.hasKey("canvasWidth") && mixerConfig.hasKey("canvasHeight")) {
                mixer.setCanvasSize(
                    mixerConfig.getInt("canvasWidth"),
                    mixerConfig.getInt("canvasHeight")
                );
            }
        }

        return config;
    }

    @ReactMethod
    public void stopBroadcast(Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }
        
        broadcastSession.stop();
        broadcastSession = null;
        sharedSession = null;
        promise.resolve(null);
    }

    @ReactMethod
    public void switchCamera(Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }

        List<Device> cameras = deviceDiscovery.listAvailableDevices(Device.Type.CAMERA);
        if (currentCamera == null && !cameras.isEmpty()) {
            currentCamera = cameras.get(0);
        }
        if (cameras.size() < 2) {
            promise.reject("E_NO_CAMERA", "Birden fazla kamera yok");
            return;
        }

        int idx = cameras.indexOf(currentCamera);
        int nextIdx = (idx + 1) % cameras.size();
        Device nextCamera = cameras.get(nextIdx);

        broadcastSession.exchangeOldDevice(currentCamera, nextCamera, (newDevice, error) -> {
            if (error != null) {
                promise.reject("E_SWITCH_CAMERA", "Kamera değiştirilemedi: " + error.getMessage(), error);
            } else {
                currentCamera = newDevice;
                WritableMap result = Arguments.createMap();
                result.putString("position", newDevice.getDescriptor().getPosition().name());
                promise.resolve(result);
            }
        });
    }

    @ReactMethod
    public void setMicrophoneMuted(boolean muted, Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }

        if (currentMicrophone == null || !(currentMicrophone instanceof Microphone)) {
            promise.reject("E_NO_MIC", "Aktif mikrofon bulunamadı");
            return;
        }

        ((Microphone) currentMicrophone).setMuted(muted);
        promise.resolve(null);
    }

    @ReactMethod
    public void getTransmissionStatistics(Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }

        TransmissionStats stats = broadcastSession.getTransmissionStatistics();
        WritableMap result = Arguments.createMap();
        result.putInt("currentBitrate", stats.getCurrentBitrate());
        result.putInt("recommendedBitrate", stats.getRecommendedBitrate());
        result.putInt("roundTripTime", stats.getRoundTripTime());
        result.putString("networkHealth", stats.getNetworkHealth().name());
        result.putString("broadcastQuality", stats.getBroadcastQuality().name());
        promise.resolve(result);
    }
} 