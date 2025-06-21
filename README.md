# amazon-ivs-broadcast-react-native

Amazon IVS Broadcast için React Native kapsayıcı modülüdür. (iOS ve Android)

## Özellikler
- En güncel Amazon IVS Broadcast SDK (iOS: 1.31.0, Android: 1.31.0)
- Modern React Native köprüsü (TurboModule/JSI desteği)
- Kamera, mikrofon, yayın başlatma/durdurma, cihaz yönetimi
- Dinamik yayın ayarları (bitrate, çözünürlük, framerate, layout)
- Event/Callback desteği (yayın durumu, hata, cihaz değişimi, bitrate, vs.)
- Gelişmiş fonksiyonlar: sessionId, broadcast test, simulcast, auto-reconnect, custom source, screen capture (iskele)

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
  getAvailableCameras,
  getAvailableMicrophones,
  attachCamera,
  attachMicrophone,
  setVideoConfig,
  setAudioConfig,
  setMixerLayout,
  getSessionId,
  runBroadcastTest,
  addListener,
  removeListener,
} from 'amazon-ivs-broadcast-react-native';

// Yayın başlat
await startBroadcast({ rtmpsUrl, streamKey });
// Yayını durdur
await stopBroadcast();
// Kamera değiştir
await switchCamera();
// Mikrofonu kapat/aç
await muteMicrophone(true);
// Kamera/mikrofon listesi
const cameras = await getAvailableCameras();
const mics = await getAvailableMicrophones();
// Kamera/mikrofon ata
await attachCamera(cameras[0].id);
await attachMicrophone(mics[0].id);
// Video/audio ayarları
await setVideoConfig({ bitrate: 1500000, width: 1280, height: 720, framerate: 30 });
await setAudioConfig({ bitrate: 128000 });
// Event dinleme
const sub = addListener('stateChanged', (data) => { /* ... */ });
sub.remove();
```

## Event Listesi
- `stateChanged`
- `error`
- `deviceChanged`
- `bitrateChanged`
- `networkQualityChanged`
- `reconnecting`
- `reconnected`

## Gelişmiş Fonksiyonlar
- `getSessionId()` — Aktif yayın sessionId'si
- `runBroadcastTest({ rtmpsUrl, streamKey })` — Yayın öncesi bağlantı testi
- `setSimulcastConfig(config)` — Simulcast ayarları (iskele)
- `setAutoReconnect(enabled)` — Otomatik yeniden bağlanma (iskele)
- `setCustomImageSource(buffer)` — Custom video kaynağı (iskele)
- `setCustomAudioSource(buffer)` — Custom audio kaynağı (iskele)
- `startScreenCapture()` / `stopScreenCapture()` — Ekran paylaşımı (iskele)

## Örnek Uygulama
Tüm özelliklerin canlı örneği için `example/` klasörüne bakınız.

## NPM Yayını
1. Versiyon numarasını güncelleyin (`package.json`)
2. Giriş klasöründe `npm publish --access public` komutunu çalıştırın
3. Native modül olduğu için, kullanıcıların iOS'ta `pod install` çalıştırması gerektiğini belirtin

## Katkı ve Lisans
MIT Lisansı. Katkılarınızı bekleriz!

---

Daha fazla bilgi ve dökümantasyon eklenecektir.

> **Uyarı:** `removeListener` fonksiyonu artık mevcuttur. Ayrıca aşağıdaki fonksiyonlar şu an sadece iskelet olarak yer almakta ve native tarafta gerçek bir işlevi yoktur:
> - `startScreenCapture`, `stopScreenCapture`
> - `setCustomImageSource`, `setCustomAudioSource`
> - `runBroadcastTest`
> - `setSimulcastConfig`
> - `setAutoReconnect`
> Bu fonksiyonlar çağrıldığında hata döner. Gerçek native entegrasyon için katkı beklenmektedir.