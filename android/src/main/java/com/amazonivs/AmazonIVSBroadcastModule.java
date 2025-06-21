package com.amazonivs;

import android.media.AudioManager;
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

            if (videoConfig.hasKey("maxBitrate")) {
                video.setMaxBitrate(videoConfig.getInt("maxBitrate"));
            }

            if (videoConfig.hasKey("minBitrate")) {
                video.setMinBitrate(videoConfig.getInt("minBitrate"));
            }

            if (videoConfig.hasKey("qualityOptimization")) {
                String optimization = videoConfig.getString("qualityOptimization");
                video.setQualityOptimization(
                    optimization.equals("quality") ? 
                    Video.QualityOptimization.QUALITY : 
                    Video.QualityOptimization.LATENCY
                );
            }

            if (videoConfig.hasKey("useH265")) {
                video.setUseH265(videoConfig.getBoolean("useH265"));
            }

            if (videoConfig.hasKey("enableTranscoding")) {
                video.setEnableTranscoding(videoConfig.getBoolean("enableTranscoding"));
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

            if (audioConfig.hasKey("sampleRate")) {
                audio.setSampleRate(audioConfig.getInt("sampleRate"));
            }

            if (audioConfig.hasKey("enableEchoCancellation")) {
                audio.setEnableEchoCancellation(audioConfig.getBoolean("enableEchoCancellation"));
            }

            if (audioConfig.hasKey("enableNoiseSuppression")) {
                audio.setEnableNoiseSuppression(audioConfig.getBoolean("enableNoiseSuppression"));
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

            if (mixerConfig.hasKey("backgroundColor")) {
                String color = mixerConfig.getString("backgroundColor");
                mixer.setBackgroundColor(Color.parseColor(color));
            }
        }

        // Auto-reconnect ayarları
        if (options.hasKey("enableAutoReconnect")) {
            config.setEnableAutoReconnect(options.getBoolean("enableAutoReconnect"));

            if (options.hasKey("autoReconnectMaxRetries")) {
                config.setAutoReconnectMaxRetries(options.getInt("autoReconnectMaxRetries"));
            }

            if (options.hasKey("autoReconnectRetryInterval")) {
                config.setAutoReconnectRetryInterval(options.getDouble("autoReconnectRetryInterval"));
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

    @ReactMethod
    public void getStreamMetrics(Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }

        WritableMap metrics = Arguments.createMap();
        metrics.putDouble("cpu", broadcastSession.getStreamMetrics().getCpuUsage());
        metrics.putDouble("memory", broadcastSession.getStreamMetrics().getMemoryUsage());
        metrics.putDouble("battery", broadcastSession.getStreamMetrics().getBatteryLevel());
        metrics.putDouble("temperature", broadcastSession.getStreamMetrics().getDeviceTemperature());
        promise.resolve(metrics);
    }

    @ReactMethod
    public void configureAudioSession(ReadableMap config, Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }

        try {
            String category = config.hasKey("category") ? config.getString("category") : "playAndRecord";
            String mode = config.hasKey("mode") ? config.getString("mode") : "default";
            boolean mixWithOthers = config.hasKey("mixWithOthers") ? config.getBoolean("mixWithOthers") : false;

            AudioManager.Mode audioMode = getAudioMode(mode);
            int streamType = getStreamType(category);

            AudioManager audioManager = (AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMode(audioMode);
            audioManager.setStreamVolume(streamType, audioManager.getStreamMaxVolume(streamType), 0);
            
            if (mixWithOthers) {
                audioManager.setMicrophoneMute(false);
                audioManager.setSpeakerphoneOn(true);
            }

            promise.resolve(null);
        } catch (Exception e) {
            promise.reject("E_AUDIO_SESSION", "Audio session yapılandırılamadı: " + e.getMessage());
        }
    }

    private int getStreamType(String category) {
        switch (category) {
            case "ambient":
                return AudioManager.STREAM_SYSTEM;
            case "playback":
                return AudioManager.STREAM_MUSIC;
            case "record":
                return AudioManager.STREAM_VOICE_CALL;
            case "playAndRecord":
            default:
                return AudioManager.STREAM_VOICE_CALL;
        }
    }

    private int getAudioMode(String mode) {
        switch (mode) {
            case "voiceChat":
                return AudioManager.MODE_IN_COMMUNICATION;
            case "gameChat":
                return AudioManager.MODE_NORMAL;
            case "videoChat":
                return AudioManager.MODE_IN_COMMUNICATION;
            default:
                return AudioManager.MODE_NORMAL;
        }
    }
} 