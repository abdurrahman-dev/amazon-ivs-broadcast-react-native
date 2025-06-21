# amazon-ivs-broadcast-react-native

Amazon IVS Broadcast için React Native kapsayıcı modülüdür. (iOS ve Android)

## Özellikler
- En güncel Amazon IVS Broadcast SDK (iOS: 1.31.0, Android: 1.31.0)
- Modern React Native köprüsü
- Tam özellikli yayın yönetimi:
  - Kamera ve mikrofon kontrolü
  - Video/Audio konfigürasyonu
  - Mixer ve layout yönetimi
  - Auto-reconnect desteği
  - H.265 kodlama desteği
  - Kalite optimizasyonu
- Gelişmiş özellikler:
  - Network sağlığı izleme
  - Yayın kalitesi metrikleri
  - CPU, bellek, batarya kullanımı
  - Audio session yönetimi
  - Echo cancellation ve gürültü önleme
- Kapsamlı event sistemi:
  - Yayın durumu değişiklikleri
  - Hata yönetimi
  - Cihaz değişiklikleri
  - Bitrate değişiklikleri
  - Network sağlığı
  - Resource limitleri

## Gereksinimler
- iOS 14.0 ve üzeri
- Android API 28 (Android 9.0) ve üzeri
- React Native 0.71.0 ve üzeri

## Kurulum

```sh
npm install amazon-ivs-broadcast-react-native
```
veya
```sh
yarn add amazon-ivs-broadcast-react-native
```

> **Not:** Native modül olduğu için iOS'ta `cd ios && pod install` komutunu çalıştırmayı unutmayın.

## Temel Kullanım

```tsx
import {
  startBroadcast,
  stopBroadcast,
  switchCamera,
  muteMicrophone,
  getAvailableDevices,
  attachCamera,
  attachMicrophone,
  setVideoConfig,
  setAudioConfig,
  setMixerConfig,
  getTransmissionStats,
  getStreamMetrics,
  configureAudioSession,
  addListener,
  removeAllListeners,
  IVSBroadcastPreview,
} from 'amazon-ivs-broadcast-react-native';

// Preview bileşeni
<IVSBroadcastPreview
  style={styles.preview}
  aspectMode="fit"
  mirrored={true}
  onErrorOccurred={handleError}
/>

// Yayın başlat
await startBroadcast({
  rtmpsUrl,
  streamKey,
  video: {
    width: 1280,
    height: 720,
    bitrate: 2500000,
    targetFramerate: 30,
    keyframeInterval: 2,
    maxBitrate: 3000000,
    minBitrate: 1000000,
    qualityOptimization: 'quality',
    useH265: true,
  },
  audio: {
    bitrate: 128000,
    channels: 2,
    sampleRate: 44100,
    enableEchoCancellation: true,
    enableNoiseSuppression: true,
  },
  mixer: {
    canvasWidth: 1280,
    canvasHeight: 720,
    backgroundColor: '#000000',
  },
  enableAutoReconnect: true,
  autoReconnectMaxRetries: 3,
  autoReconnectRetryInterval: 5,
});

// Yayını durdur
await stopBroadcast();

// Cihaz yönetimi
const devices = await getAvailableDevices();
await attachCamera(devices[0].id);
await attachMicrophone(devices[1].id);
await switchCamera();
await muteMicrophone(true);

// Konfigürasyon
await setVideoConfig({ /* ... */ });
await setAudioConfig({ /* ... */ });
await setMixerConfig({ /* ... */ });

// İstatistikler
const stats = await getTransmissionStats();
const metrics = await getStreamMetrics();

// Audio session
await configureAudioSession({
  category: 'playAndRecord',
  mode: 'videoChat',
  mixWithOthers: true,
});

// Event dinleme
const subscription = addListener('stateChanged', (event) => {
  console.log('Yayın durumu:', event.state);
});
subscription.remove();
```

## Event Listesi
- `stateChanged`: Yayın durumu değişiklikleri
- `error`: Hata durumları
- `deviceChanged`: Kamera/mikrofon değişiklikleri
- `bitrateChanged`: Bitrate değişiklikleri
- `networkHealthChanged`: Network sağlığı değişiklikleri
- `audioSessionInterrupted`: Audio session kesintileri
- `audioSessionResumed`: Audio session devam etme
- `cameraError`: Kamera hataları
- `microphoneError`: Mikrofon hataları
- `reconnecting`: Yeniden bağlanma başladı
- `reconnected`: Yeniden bağlanma başarılı
- `streamHealthChanged`: Yayın sağlığı değişiklikleri
- `resourceLimitExceeded`: Kaynak limit aşımları
- `streamInterrupted`: Yayın kesintileri
- `streamResumed`: Yayın devam etme

## Lisans
MIT