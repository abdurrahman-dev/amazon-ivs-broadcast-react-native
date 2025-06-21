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

  static var sharedSession: IVSBroadcastSession?

  private var broadcastSession: IVSBroadcastSession?
  private var currentCamera: IVSDevice?
  private var currentMicrophone: IVSDevice?
  private var hasListeners = false

  // MARK: - EventEmitter
  override func supportedEvents() -> [String]! {
    return [
      "stateChanged",
      "error",
      "deviceChanged",
      "bitrateChanged",
      "networkHealthChanged",
      "networkQualityChanged",
      "audioSessionInterrupted",
      "audioSessionResumed",
      "cameraError",
      "microphoneError",
      "reconnecting",
      "reconnected"
    ]
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
    let isFatal = (error as NSError).userInfo[IVSBroadcastErrorIsFatalKey] as? Bool ?? false
    sendEvent("error", body: [
      "message": error.localizedDescription,
      "code": nsError.domain + ":" + String(nsError.code),
      "isFatal": isFatal
    ])
  }

  func broadcastSession(_ session: IVSBroadcastSession, didChange devices: [IVSDevice]) {
    let deviceList = devices.map { device -> [String: Any] in
      return [
        "id": device.descriptor().uid,
        "name": device.descriptor().name,
        "position": device.descriptor().position?.rawValue ?? "",
        "type": device.type.rawValue
      ]
    }
    sendEvent("deviceChanged", body: deviceList)
  }

  func broadcastSession(_ session: IVSBroadcastSession, didChangeBitrate bitrate: Int) {
    sendEvent("bitrateChanged", body: ["bitrate": bitrate])
  }

  func broadcastSession(_ session: IVSBroadcastSession, didChangeNetworkHealth health: IVSTransmissionStatistics.NetworkHealth) {
    sendEvent("networkHealthChanged", body: ["networkHealth": health.rawValue])
  }

  func broadcastSession(_ session: IVSBroadcastSession, audioSessionWasInterrupted interrupted: Bool) {
    sendEvent(interrupted ? "audioSessionInterrupted" : "audioSessionResumed", body: nil)
  }

  func broadcastSessionDidStartReconnect(_ session: IVSBroadcastSession) {
    sendEvent("reconnecting", body: nil)
  }

  func broadcastSessionDidReconnect(_ session: IVSBroadcastSession) {
    sendEvent("reconnected", body: nil)
  }

  // MARK: - Yayın Yönetimi
  @objc(startBroadcast:withResolver:withRejecter:)
  func startBroadcast(options: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let rtmpsUrl = options["rtmpsUrl"] as? String,
          let streamKey = options["streamKey"] as? String else {
      reject("E_MISSING_PARAMS", "rtmpsUrl ve streamKey gereklidir", nil)
      return
    }

    do {
      let config = try createBroadcastConfig(from: options)
      let devices = try createDeviceDescriptors(from: options)
      
      self.broadcastSession = try IVSBroadcastSession(
        configuration: config,
        descriptors: devices,
        delegate: self
      )
      
      AmazonIVSBroadcastModule.sharedSession = self.broadcastSession
      
      try self.broadcastSession?.start(with: rtmpsUrl, streamKey: streamKey)
      resolve(nil)
    } catch let error {
      reject("E_BROADCAST_START", "Yayın başlatılamadı: \(error.localizedDescription)", error)
    }
  }

  private func createBroadcastConfig(from options: NSDictionary) throws -> IVSBroadcastConfiguration {
    let config = IVSPresets.configurations().standardLandscape()
    
    // Video ayarları
    if let videoConfig = options["video"] as? [String: Any] {
      if let width = videoConfig["width"] as? Int,
         let height = videoConfig["height"] as? Int {
        try config.video.setSize(CGSize(width: width, height: height))
      }
      if let bitrate = videoConfig["bitrate"] as? Int {
        try config.video.setInitialBitrate(bitrate)
      }
      if let targetFramerate = videoConfig["targetFramerate"] as? Int {
        try config.video.setTargetFramerate(targetFramerate)
      }
      if let keyframeInterval = videoConfig["keyframeInterval"] as? Int {
        try config.video.setKeyframeInterval(keyframeInterval)
      }
    }
    
    // Audio ayarları
    if let audioConfig = options["audio"] as? [String: Any] {
      if let bitrate = audioConfig["bitrate"] as? Int {
        try config.audio.setBitrate(bitrate)
      }
      if let channels = audioConfig["channels"] as? Int {
        try config.audio.setChannels(channels)
      }
    }
    
    // Mixer ayarları
    if let mixerConfig = options["mixer"] as? [String: Any] {
      if let canvasWidth = mixerConfig["canvasWidth"] as? Int,
         let canvasHeight = mixerConfig["canvasHeight"] as? Int {
        config.mixer.setCanvasSize(CGSize(width: canvasWidth, height: canvasHeight))
      }
    }

    return config
  }

  private func createDeviceDescriptors(from options: NSDictionary) -> [IVSDeviceDescriptor] {
    var descriptors: [IVSDeviceDescriptor] = []
    
    // Kamera ayarları
    if let cameraPosition = options["cameraPosition"] as? String {
      let position: IVSDeviceDescriptor.Position = cameraPosition == "back" ? .back : .front
      descriptors.append(contentsOf: IVSPresets.devices().camera(position: position))
    } else {
      descriptors.append(contentsOf: IVSPresets.devices().frontCamera())
    }
    
    // Mikrofon ayarları
    descriptors.append(contentsOf: IVSPresets.devices().microphone())
    
    return descriptors
  }

  @objc(stopBroadcast:withRejecter:)
  func stopBroadcast(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let session = self.broadcastSession else {
      reject("E_NO_SESSION", "Aktif bir yayın yok", nil)
      return
    }
    
    session.stop()
    self.broadcastSession = nil
    AmazonIVSBroadcastModule.sharedSession = nil
    resolve(nil)
  }

  // MARK: - Cihaz Yönetimi
  @objc(switchCamera:withRejecter:)
  func switchCamera(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let session = self.broadcastSession else {
      reject("E_NO_SESSION", "Aktif bir yayın yok", nil)
      return
    }

    let cameras = session.listAvailableDevices().filter { $0.type == .camera }
    guard let current = self.currentCamera,
          let currentIndex = cameras.firstIndex(where: { $0.descriptor().uid == current.descriptor().uid }) else {
      reject("E_NO_CAMERA", "Mevcut kamera bulunamadı", nil)
      return
    }

    let nextIndex = (currentIndex + 1) % cameras.count
    let nextCamera = cameras[nextIndex]

    session.exchangeOldDevice(current, withNewDevice: nextCamera) { [weak self] newDevice, error in
      if let error = error {
        reject("E_SWITCH_CAMERA", "Kamera değiştirilemedi: \(error.localizedDescription)", error)
      } else if let device = newDevice {
        self?.currentCamera = device
        resolve(["position": device.descriptor().position?.rawValue ?? "unknown"])
      }
    }
  }

  @objc(setMicrophoneMuted:withResolver:withRejecter:)
  func setMicrophoneMuted(muted: Bool, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let session = self.broadcastSession else {
      reject("E_NO_SESSION", "Aktif bir yayın yok", nil)
      return
    }

    guard let mic = currentMicrophone as? IVSMicrophone else {
      reject("E_NO_MIC", "Aktif mikrofon bulunamadı", nil)
      return
    }

    mic.setMuted(muted)
    resolve(nil)
  }

  // MARK: - Kalite ve İstatistikler
  @objc(getTransmissionStatistics:withRejecter:)
  func getTransmissionStatistics(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let session = self.broadcastSession else {
      reject("E_NO_SESSION", "Aktif bir yayın yok", nil)
      return
    }

    let stats = session.transmissionStatistics
    resolve([
      "currentBitrate": stats.currentBitrate,
      "recommendedBitrate": stats.recommendedBitrate,
      "roundTripTime": stats.roundTripTime,
      "networkHealth": stats.networkHealth.rawValue,
      "broadcastQuality": stats.broadcastQuality.rawValue
    ])
  }
} 