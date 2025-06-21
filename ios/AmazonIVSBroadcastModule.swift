import Foundation
import React
import AmazonIVSBroadcast

@objc(AmazonIVSBroadcastModule)
class AmazonIVSBroadcastModule: RCTEventEmitter, IVSBroadcastSession.Delegate {
  override static func moduleName() -> String! {
    return "AmazonIVSBroadcastModule"
  }

  override static func requiresMainQueueSetup() -> Bool {
    return true
  }

  static var sharedSession: IVSBroadcastSession? // PreviewView erişimi için static referans

  private var broadcastSession: IVSBroadcastSession?
  private var currentCamera: IVSDevice?
  private var currentMicrophone: IVSDevice?
  private var hasListeners = false

  // MARK: - EventEmitter
  override func supportedEvents() -> [String]! {
    return ["stateChanged", "error", "deviceChanged", "bitrateChanged", "networkQualityChanged", "reconnecting", "reconnected"]
  }

  override func startObserving() {
    hasListeners = true
  }

  override func stopObserving() {
    hasListeners = false
  }

  private func sendEvent(_ name: String, body: Any?) {
    if hasListeners {
      sendEvent(withName: name, body: body)
    }
  }

  // MARK: - IVSBroadcastSession.Delegate
  func broadcastSession(_ session: IVSBroadcastSession, didChange state: IVSBroadcastSession.State) {
    sendEvent("stateChanged", body: ["state": state.rawValue])
  }

  func broadcastSession(_ session: IVSBroadcastSession, didEmitError error: Error) {
    let nsError = error as NSError
    sendEvent("error", body: [
      "message": error.localizedDescription,
      "code": nsError.domain + ":" + String(nsError.code)
    ])
  }

  func broadcastSession(_ session: IVSBroadcastSession, didChange devices: [IVSDevice]) {
    let deviceList = devices.map { ["id": $0.descriptor().uid, "name": $0.descriptor().name, "type": $0.type.rawValue] }
    sendEvent("deviceChanged", body: deviceList)
  }

  func broadcastSession(_ session: IVSBroadcastSession, didChangeBitrate bitrate: Int) {
    sendEvent("bitrateChanged", body: ["bitrate": bitrate])
  }

  func broadcastSession(_ session: IVSBroadcastSession, didChangeNetworkHealth health: Int) {
    sendEvent("networkQualityChanged", body: ["networkQuality": health])
  }

  func broadcastSessionDidStartReconnect(_ session: IVSBroadcastSession) {
    sendEvent("reconnecting", body: nil)
  }

  func broadcastSessionDidReconnect(_ session: IVSBroadcastSession) {
    sendEvent("reconnected", body: nil)
  }

  // MARK: - Yayın başlatma
  @objc(startBroadcast:withResolver:withRejecter:)
  func startBroadcast(options: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let rtmpsUrl = options["rtmpsUrl"] as? String,
          let streamKey = options["streamKey"] as? String else {
      reject("E_MISSING_PARAMS", "rtmpsUrl ve streamKey gereklidir", nil)
      return
    }
    do {
      let config = IVSPresets.configurations().standardLandscape()
      let devices = IVSPresets.devices().frontCamera()
      self.broadcastSession = try IVSBroadcastSession(configuration: config, descriptors: devices, delegate: self)
      try self.broadcastSession?.start(with: rtmpsUrl, streamKey: streamKey)
      resolve(nil)
    } catch let error {
      reject("E_BROADCAST_START", "Yayın başlatılamadı", error)
    }
  }

  // Yayını durdurma
  @objc(stopBroadcast:withRejecter:)
  func stopBroadcast(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let session = self.broadcastSession else {
      reject("E_NO_SESSION", "Aktif bir yayın yok", nil)
      return
    }
    session.stop()
    self.broadcastSession = nil
    resolve(nil)
  }

  // MARK: - Kamera/Mikrofon Yönetimi

  @objc(switchCamera:withRejecter:)
  func switchCamera(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let session = self.broadcastSession else {
      reject("E_NO_SESSION", "Aktif bir yayın yok", nil)
      return
    }
    let cameras = IVSBroadcastSession.listAvailableDevices().filter { $0.type == .camera }
    guard let current = self.currentCamera, let idx = cameras.firstIndex(where: { $0.descriptor().uid == current.descriptor().uid }) else {
      reject("E_NO_CAMERA", "Mevcut kamera bulunamadı", nil)
      return
    }
    let nextIdx = (idx + 1) % cameras.count
    let nextCamera = cameras[nextIdx]
    session.exchangeOldDevice(current, withNewDevice: nextCamera) { [weak self] newDevice, error in
      if let error = error {
        reject("E_SWITCH_CAMERA", "Kamera değiştirilemedi", error)
      } else {
        self?.currentCamera = newDevice
        resolve(nil)
      }
    }
  }

  @objc(muteMicrophone:withResolver:withRejecter:)
  func muteMicrophone(mute: Bool, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let session = self.broadcastSession else {
      reject("E_NO_SESSION", "Aktif bir yayın yok", nil)
      return
    }
    let mics = IVSBroadcastSession.listAvailableDevices().filter { $0.type == .microphone }
    guard let mic = mics.first else {
      reject("E_NO_MIC", "Mikrofon bulunamadı", nil)
      return
    }
    if let micDevice = mic as? IVSMicrophone {
      micDevice.setMuted(mute)
      resolve(nil)
    } else {
      reject("E_MIC_TYPE", "Mikrofon tipi hatalı", nil)
    }
  }

  @objc(getAvailableCameras:withRejecter:)
  func getAvailableCameras(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    let cameras = IVSBroadcastSession.listAvailableDevices().filter { $0.type == .camera }
    let result = cameras.map { ["id": $0.descriptor().uid, "name": $0.descriptor().name] }
    resolve(result)
  }

  @objc(getAvailableMicrophones:withRejecter:)
  func getAvailableMicrophones(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    let mics = IVSBroadcastSession.listAvailableDevices().filter { $0.type == .microphone }
    let result = mics.map { ["id": $0.descriptor().uid, "name": $0.descriptor().name] }
    resolve(result)
  }

  @objc(attachCamera:withResolver:withRejecter:)
  func attachCamera(deviceId: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let session = self.broadcastSession else {
      reject("E_NO_SESSION", "Aktif bir yayın yok", nil)
      return
    }
    let cameras = IVSBroadcastSession.listAvailableDevices().filter { $0.type == .camera }
    guard let camera = cameras.first(where: { $0.descriptor().uid == deviceId }) else {
      reject("E_CAMERA_NOT_FOUND", "Kamera bulunamadı", nil)
      return
    }
    session.attach(camera, toSlotWithName: "camera") { [weak self] device, error in
      if let error = error {
        reject("E_ATTACH_CAMERA", "Kamera eklenemedi", error)
      } else {
        self?.currentCamera = device
        resolve(nil)
      }
    }
  }

  @objc(attachMicrophone:withResolver:withRejecter:)
  func attachMicrophone(deviceId: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let session = self.broadcastSession else {
      reject("E_NO_SESSION", "Aktif bir yayın yok", nil)
      return
    }
    let mics = IVSBroadcastSession.listAvailableDevices().filter { $0.type == .microphone }
    guard let mic = mics.first(where: { $0.descriptor().uid == deviceId }) else {
      reject("E_MIC_NOT_FOUND", "Mikrofon bulunamadı", nil)
      return
    }
    session.attach(mic, toSlotWithName: "microphone") { [weak self] device, error in
      if let error = error {
        reject("E_ATTACH_MIC", "Mikrofon eklenemedi", error)
      } else {
        self?.currentMicrophone = device
        resolve(nil)
      }
    }
  }

  @objc(setVideoConfig:withResolver:withRejecter:)
  func setVideoConfig(config: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let session = self.broadcastSession else {
      reject("E_NO_SESSION", "Aktif bir yayın yok", nil)
      return
    }
    do {
      let videoConfig = session.configuration.video
      if let bitrate = config["bitrate"] as? NSNumber {
        try videoConfig.setInitialBitrate(bitrate.intValue)
      }
      if let width = config["width"] as? NSNumber, let height = config["height"] as? NSNumber {
        try videoConfig.setSize(CGSize(width: width.intValue, height: height.intValue))
      }
      if let framerate = config["framerate"] as? NSNumber {
        try videoConfig.setTargetFramerate(framerate.intValue)
      }
      resolve(nil)
    } catch let error {
      reject("E_SET_VIDEO_CONFIG", "Video ayarları güncellenemedi", error)
    }
  }

  @objc(setAudioConfig:withResolver:withRejecter:)
  func setAudioConfig(config: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let session = self.broadcastSession else {
      reject("E_NO_SESSION", "Aktif bir yayın yok", nil)
      return
    }
    do {
      let audioConfig = session.configuration.audio
      if let bitrate = config["bitrate"] as? NSNumber {
        try audioConfig.setBitrate(bitrate.intValue)
      }
      resolve(nil)
    } catch let error {
      reject("E_SET_AUDIO_CONFIG", "Audio ayarları güncellenemedi", error)
    }
  }

  @objc(setMixerLayout:withResolver:withRejecter:)
  func setMixerLayout(layout: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let session = self.broadcastSession else {
      reject("E_NO_SESSION", "Aktif bir yayın yok", nil)
      return
    }
    do {
      // Gelişmiş: slot isimleri, pozisyon ve boyut
      if let slots = layout["slots"] as? [[String: Any]] {
        var mixerSlots: [IVSMixerSlotConfiguration] = []
        for slot in slots {
          let slotConfig = IVSMixerSlotConfiguration()
          if let name = slot["name"] as? String {
            try slotConfig.setName(name)
          }
          if let zIndex = slot["zIndex"] as? NSNumber {
            slotConfig.zIndex = zIndex.intValue
          }
          if let x = slot["x"] as? NSNumber {
            slotConfig.x = x.intValue
          }
          if let y = slot["y"] as? NSNumber {
            slotConfig.y = y.intValue
          }
          if let width = slot["width"] as? NSNumber {
            slotConfig.width = width.intValue
          }
          if let height = slot["height"] as? NSNumber {
            slotConfig.height = height.intValue
          }
          mixerSlots.append(slotConfig)
        }
        session.configuration.mixer.slots = mixerSlots
      }
      resolve(nil)
    } catch let error {
      let nsError = error as NSError
      reject("E_SET_MIXER_LAYOUT", "Mixer layout güncellenemedi: \(error.localizedDescription) [\(nsError.domain):\(nsError.code)]", error)
    }
  }

  @objc(startScreenCapture:withRejecter:)
  func startScreenCapture(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    // iOS'ta ekran paylaşımı için ReplayKit entegrasyonu gerekir. Burada iskelet bırakıyorum.
    // Gerçek uygulamada, ReplayKit extension ile native entegrasyon yapılmalı.
    reject("E_NOT_IMPLEMENTED", "iOS'ta ekran paylaşımı için ReplayKit entegrasyonu gereklidir.", nil)
  }

  @objc(stopScreenCapture:withRejecter:)
  func stopScreenCapture(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    // iOS'ta ekran paylaşımı için ReplayKit entegrasyonu gerekir. Burada iskelet bırakıyorum.
    reject("E_NOT_IMPLEMENTED", "iOS'ta ekran paylaşımı için ReplayKit entegrasyonu gereklidir.", nil)
  }

  struct CustomImageBuffer: Codable {
    let data: Data
    let width: Int
    let height: Int
    let pixelFormat: String
  }

  struct CustomAudioBuffer: Codable {
    let data: Data
    let sampleRate: Int
    let channels: Int
  }

  @objc(setCustomImageSource:withResolver:withRejecter:)
  func setCustomImageSource(buffer: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    // Custom video kaynağı eklemek için iskelet fonksiyon.
    // Parametreyi CustomImageBuffer'a decode etmeye çalış
    reject("E_NOT_IMPLEMENTED", "Custom image source için native entegrasyon gereklidir.", nil)
  }

  @objc(setCustomAudioSource:withResolver:withRejecter:)
  func setCustomAudioSource(buffer: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    // Custom audio kaynağı eklemek için iskelet fonksiyon.
    // Parametreyi CustomAudioBuffer'a decode etmeye çalış
    reject("E_NOT_IMPLEMENTED", "Custom audio source için native entegrasyon gereklidir.", nil)
  }

  @objc(runBroadcastTest:withResolver:withRejecter:)
  func runBroadcastTest(options: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    // iOS SDK'da broadcast test için iskelet fonksiyon.
    reject("E_NOT_IMPLEMENTED", "Broadcast test için native entegrasyon gereklidir.", nil)
  }

  @objc(getSessionId:withRejecter:)
  func getSessionId(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let session = self.broadcastSession else {
      reject("E_NO_SESSION", "Aktif bir yayın yok", nil)
      return
    }
    resolve(session.sessionId)
  }

  @objc(setSimulcastConfig:withResolver:withRejecter:)
  func setSimulcastConfig(config: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    // Simulcast ayarları için iskelet fonksiyon.
    reject("E_NOT_IMPLEMENTED", "Simulcast için native entegrasyon gereklidir.", nil)
  }

  @objc(setAutoReconnect:withResolver:withRejecter:)
  func setAutoReconnect(enabled: Bool, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    // Auto-reconnect ayarı için iskelet fonksiyon.
    reject("E_NOT_IMPLEMENTED", "Auto-reconnect için native entegrasyon gereklidir.", nil)
  }

  // Event desteği için gerekli fonksiyonlar ve delegate implementasyonu ileride eklenecek.
} 