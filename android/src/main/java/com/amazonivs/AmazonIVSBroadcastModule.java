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
import com.amazonaws.ivs.broadcast.BroadcastSession;
import com.amazonaws.ivs.broadcast.BroadcastSession.State;
import com.amazonaws.ivs.broadcast.BroadcastSession.Listener;
import com.amazonaws.ivs.broadcast.Device;
import com.amazonaws.ivs.broadcast.DeviceDiscovery;
import com.amazonaws.ivs.broadcast.ImageDevice;
import com.amazonaws.ivs.broadcast.Microphone;

import java.util.List;

public class AmazonIVSBroadcastModule extends ReactContextBaseJavaModule {
    public static BroadcastSession sharedSession; // PreviewView erişimi için static referans
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

    // Event gönderimi
    private void sendEvent(String eventName, Object params) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    // BroadcastSession Listener
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
            sendEvent("error", map);
        }

        @Override
        public void onDeviceChange(@NonNull BroadcastSession session, @NonNull List<Device> devices) {
            WritableArray arr = Arguments.createArray();
            for (Device d : devices) {
                WritableMap dev = Arguments.createMap();
                dev.putString("id", d.getDescriptor().getUid());
                dev.putString("name", d.getDescriptor().getName());
                dev.putString("type", d.getType().name());
                arr.pushMap(dev);
            }
            sendEvent("deviceChanged", arr);
        }
    };

    // Yayın başlatma
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
            BroadcastConfiguration config = new BroadcastConfiguration();
            broadcastSession = new BroadcastSession(reactContext, deviceDiscovery, config, sessionListener);
            broadcastSession.start(rtmpsUrl, streamKey);
            promise.resolve(null);
        } catch (Exception e) {
            promise.reject("E_BROADCAST_START", "Yayın başlatılamadı", e);
        }
    }

    @ReactMethod
    public void stopBroadcast(Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }
        broadcastSession.stop();
        broadcastSession = null;
        promise.resolve(null);
    }

    // Kamera/Mikrofon yönetimi
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
                promise.reject("E_SWITCH_CAMERA", "Kamera değiştirilemedi", error);
            } else {
                currentCamera = newDevice;
                promise.resolve(null);
            }
        });
    }

    @ReactMethod
    public void muteMicrophone(boolean mute, Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }
        List<Device> mics = deviceDiscovery.listAvailableDevices(Device.Type.MICROPHONE);
        if (mics.isEmpty()) {
            promise.reject("E_NO_MIC", "Mikrofon bulunamadı");
            return;
        }
        Microphone mic = (Microphone) mics.get(0);
        mic.setMuted(mute);
        promise.resolve(null);
    }

    @ReactMethod
    public void getAvailableCameras(Promise promise) {
        List<Device> cameras = deviceDiscovery.listAvailableDevices(Device.Type.CAMERA);
        WritableArray arr = Arguments.createArray();
        for (Device d : cameras) {
            WritableMap dev = Arguments.createMap();
            dev.putString("id", d.getDescriptor().getUid());
            dev.putString("name", d.getDescriptor().getName());
            arr.pushMap(dev);
        }
        promise.resolve(arr);
    }

    @ReactMethod
    public void getAvailableMicrophones(Promise promise) {
        List<Device> mics = deviceDiscovery.listAvailableDevices(Device.Type.MICROPHONE);
        WritableArray arr = Arguments.createArray();
        for (Device d : mics) {
            WritableMap dev = Arguments.createMap();
            dev.putString("id", d.getDescriptor().getUid());
            dev.putString("name", d.getDescriptor().getName());
            arr.pushMap(dev);
        }
        promise.resolve(arr);
    }

    @ReactMethod
    public void attachCamera(String deviceId, Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }
        List<Device> cameras = deviceDiscovery.listAvailableDevices(Device.Type.CAMERA);
        Device camera = null;
        for (Device d : cameras) {
            if (d.getDescriptor().getUid().equals(deviceId)) {
                camera = d;
                break;
            }
        }
        if (camera == null) {
            promise.reject("E_CAMERA_NOT_FOUND", "Kamera bulunamadı");
            return;
        }
        broadcastSession.attach(camera, "camera", (device, error) -> {
            if (error != null) {
                promise.reject("E_ATTACH_CAMERA", "Kamera eklenemedi", error);
            } else {
                currentCamera = device;
                promise.resolve(null);
            }
        });
    }

    @ReactMethod
    public void attachMicrophone(String deviceId, Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }
        List<Device> mics = deviceDiscovery.listAvailableDevices(Device.Type.MICROPHONE);
        Device mic = null;
        for (Device d : mics) {
            if (d.getDescriptor().getUid().equals(deviceId)) {
                mic = d;
                break;
            }
        }
        if (mic == null) {
            promise.reject("E_MIC_NOT_FOUND", "Mikrofon bulunamadı");
            return;
        }
        broadcastSession.attach(mic, "microphone", (device, error) -> {
            if (error != null) {
                promise.reject("E_ATTACH_MIC", "Mikrofon eklenemedi", error);
            } else {
                currentMicrophone = device;
                promise.resolve(null);
            }
        });
    }

    @ReactMethod
    public void setVideoConfig(ReadableMap config, Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }
        try {
            BroadcastConfiguration.Video videoConfig = broadcastSession.getConfiguration().getVideo();
            if (config.hasKey("bitrate")) {
                videoConfig.setInitialBitrate(config.getInt("bitrate"));
            }
            if (config.hasKey("width") && config.hasKey("height")) {
                videoConfig.setSize(config.getInt("width"), config.getInt("height"));
            }
            if (config.hasKey("framerate")) {
                videoConfig.setTargetFramerate(config.getInt("framerate"));
            }
            promise.resolve(null);
        } catch (Exception e) {
            promise.reject("E_SET_VIDEO_CONFIG", "Video ayarları güncellenemedi", e);
        }
    }

    @ReactMethod
    public void setAudioConfig(ReadableMap config, Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }
        try {
            BroadcastConfiguration.Audio audioConfig = broadcastSession.getConfiguration().getAudio();
            if (config.hasKey("bitrate")) {
                audioConfig.setBitrate(config.getInt("bitrate"));
            }
            promise.resolve(null);
        } catch (Exception e) {
            promise.reject("E_SET_AUDIO_CONFIG", "Audio ayarları güncellenemedi", e);
        }
    }

    @ReactMethod
    public void setMixerLayout(ReadableMap layout, Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }
        try {
            if (layout.hasKey("slots")) {
                // Basit örnek: slot isimleri ve zIndex
                List<BroadcastConfiguration.MixerSlot> slots = new java.util.ArrayList<>();
                for (int i = 0; i < layout.getArray("slots").size(); i++) {
                    ReadableMap slot = layout.getArray("slots").getMap(i);
                    BroadcastConfiguration.MixerSlot slotConfig = new BroadcastConfiguration.MixerSlot();
                    if (slot.hasKey("name")) {
                        slotConfig.setName(slot.getString("name"));
                    }
                    if (slot.hasKey("zIndex")) {
                        slotConfig.setZIndex(slot.getInt("zIndex"));
                    }
                    slots.add(slotConfig);
                }
                broadcastSession.getConfiguration().getMixer().setSlots(slots);
            }
            promise.resolve(null);
        } catch (Exception e) {
            promise.reject("E_SET_MIXER_LAYOUT", "Mixer layout güncellenemedi", e);
        }
    }

    @ReactMethod
    public void startScreenCapture(Promise promise) {
        // Android'de ekran paylaşımı için MediaProjection entegrasyonu gerekir. Burada iskelet bırakıyorum.
        promise.reject("E_NOT_IMPLEMENTED", "Android'de ekran paylaşımı için MediaProjection entegrasyonu gereklidir.");
    }

    @ReactMethod
    public void stopScreenCapture(Promise promise) {
        // Android'de ekran paylaşımı için MediaProjection entegrasyonu gerekir. Burada iskelet bırakıyorum.
        promise.reject("E_NOT_IMPLEMENTED", "Android'de ekran paylaşımı için MediaProjection entegrasyonu gereklidir.");
    }

    @ReactMethod
    public void setCustomImageSource(ReadableMap buffer, Promise promise) {
        // Custom video kaynağı eklemek için iskelet fonksiyon.
        promise.reject("E_NOT_IMPLEMENTED", "Custom image source için native entegrasyon gereklidir.");
    }

    @ReactMethod
    public void setCustomAudioSource(ReadableMap buffer, Promise promise) {
        // Custom audio kaynağı eklemek için iskelet fonksiyon.
        promise.reject("E_NOT_IMPLEMENTED", "Custom audio source için native entegrasyon gereklidir.");
    }

    @ReactMethod
    public void runBroadcastTest(ReadableMap options, Promise promise) {
        // Android SDK'da broadcast test için iskelet fonksiyon.
        promise.reject("E_NOT_IMPLEMENTED", "Broadcast test için native entegrasyon gereklidir.");
    }

    @ReactMethod
    public void getSessionId(Promise promise) {
        if (broadcastSession == null) {
            promise.reject("E_NO_SESSION", "Aktif bir yayın yok");
            return;
        }
        promise.resolve(broadcastSession.getSessionId());
    }

    @ReactMethod
    public void setSimulcastConfig(ReadableMap config, Promise promise) {
        // Simulcast ayarları için iskelet fonksiyon.
        promise.reject("E_NOT_IMPLEMENTED", "Simulcast için native entegrasyon gereklidir.");
    }

    @ReactMethod
    public void setAutoReconnect(boolean enabled, Promise promise) {
        // Auto-reconnect ayarı için iskelet fonksiyon.
        promise.reject("E_NOT_IMPLEMENTED", "Auto-reconnect için native entegrasyon gereklidir.");
    }
} 