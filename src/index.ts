// Amazon IVS Broadcast React Native köprü modülünün giriş noktası

// @ts-ignore
import {
  NativeModules,
  NativeEventEmitter,
  Platform,
  requireNativeComponent,
  ViewProps,
} from "react-native";
import type {
  Device,
  VideoConfig,
  AudioConfig,
  MixerConfig,
  BroadcastConfig,
  BroadcastOptions,
  TransmissionStats,
  PreviewProps,
  IVSEvent,
  ErrorEvent,
  DeviceChangedEvent,
  BitrateChangedEvent,
  NetworkHealthChangedEvent,
  StreamHealthEvent,
  ResourceLimitEvent,
  BroadcastState,
  BroadcastMetrics,
  AudioSessionConfiguration,
} from "./types";

const LINKING_ERROR =
  `The package 'amazon-ivs-broadcast-react-native' doesn't seem to be linked.\n` +
  "Make sure: \n" +
  Platform.select({ ios: "\u2022 You have run 'pod install'", default: "" }) +
  "\n\u2022 You rebuilt the app after installing the package\n" +
  "\u2022 You are not using Expo Go app (bare workflow only)";

const AmazonIVSBroadcastModule = NativeModules.AmazonIVSBroadcastModule
  ? NativeModules.AmazonIVSBroadcastModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const emitter = NativeModules.AmazonIVSBroadcastModule
  ? new NativeEventEmitter(NativeModules.AmazonIVSBroadcastModule)
  : undefined;

// Yayın Yönetimi
export function startBroadcast(options: BroadcastOptions): Promise<void> {
  return AmazonIVSBroadcastModule.startBroadcast(options);
}

export function stopBroadcast(): Promise<void> {
  return AmazonIVSBroadcastModule.stopBroadcast();
}

// Cihaz Yönetimi
export function getAvailableDevices(): Promise<Device[]> {
  return AmazonIVSBroadcastModule.getAvailableDevices();
}

export function switchCamera(): Promise<void> {
  return AmazonIVSBroadcastModule.switchCamera();
}

export function attachCamera(deviceId: string): Promise<void> {
  return AmazonIVSBroadcastModule.attachCamera(deviceId);
}

export function attachMicrophone(deviceId: string): Promise<void> {
  return AmazonIVSBroadcastModule.attachMicrophone(deviceId);
}

export function muteMicrophone(muted: boolean): Promise<void> {
  return AmazonIVSBroadcastModule.muteMicrophone(muted);
}

// Konfigürasyon
export function setVideoConfig(config: VideoConfig): Promise<void> {
  return AmazonIVSBroadcastModule.setVideoConfig(config);
}

export function setAudioConfig(config: AudioConfig): Promise<void> {
  return AmazonIVSBroadcastModule.setAudioConfig(config);
}

export function setMixerConfig(config: MixerConfig): Promise<void> {
  return AmazonIVSBroadcastModule.setMixerConfig(config);
}

// İstatistikler ve Metrikler
export function getTransmissionStats(): Promise<TransmissionStats> {
  return AmazonIVSBroadcastModule.getTransmissionStats();
}

export function getStreamMetrics(): Promise<BroadcastMetrics> {
  return AmazonIVSBroadcastModule.getStreamMetrics();
}

// Audio Session Yönetimi
export function configureAudioSession(
  config: AudioSessionConfiguration
): Promise<void> {
  return AmazonIVSBroadcastModule.configureAudioSession(config);
}

// Event Yönetimi
export function addListener(
  eventType: IVSEvent,
  listener: (
    event:
      | { state: BroadcastState }
      | ErrorEvent
      | DeviceChangedEvent
      | BitrateChangedEvent
      | NetworkHealthChangedEvent
      | StreamHealthEvent
      | ResourceLimitEvent
      | null
  ) => void
) {
  return emitter?.addListener(eventType, listener);
}

export function removeAllListeners(
  eventType: IVSEvent,
  listener: (event: any) => void
) {
  emitter?.removeAllListeners(eventType);
}

// Preview Bileşeni
export const IVSBroadcastPreview = requireNativeComponent<PreviewProps>(
  "AmazonIVSBroadcastPreview"
);
