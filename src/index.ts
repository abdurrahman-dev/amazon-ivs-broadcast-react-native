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
  CameraDevice,
  MicrophoneDevice,
  VideoConfig,
  AudioConfig,
  MixerLayoutConfig,
  BroadcastOptions,
  BroadcastTestOptions,
  BroadcastTestResult,
  IVSEvent,
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

export function startBroadcast(options: BroadcastOptions): Promise<void> {
  return AmazonIVSBroadcastModule.startBroadcast(options);
}

export function stopBroadcast(): Promise<void> {
  return AmazonIVSBroadcastModule.stopBroadcast();
}

export function switchCamera(): Promise<void> {
  return AmazonIVSBroadcastModule.switchCamera();
}

export function muteMicrophone(mute: boolean): Promise<void> {
  return AmazonIVSBroadcastModule.muteMicrophone(mute);
}

export function getAvailableCameras(): Promise<CameraDevice[]> {
  return AmazonIVSBroadcastModule.getAvailableCameras();
}

export function getAvailableMicrophones(): Promise<MicrophoneDevice[]> {
  return AmazonIVSBroadcastModule.getAvailableMicrophones();
}

export function attachCamera(deviceId: string): Promise<void> {
  return AmazonIVSBroadcastModule.attachCamera(deviceId);
}

export function attachMicrophone(deviceId: string): Promise<void> {
  return AmazonIVSBroadcastModule.attachMicrophone(deviceId);
}

export function addListener(event: IVSEvent, callback: (data: any) => void) {
  if (!emitter) throw new Error(LINKING_ERROR);
  return emitter.addListener(event, callback);
}

export function setVideoConfig(config: VideoConfig): Promise<void> {
  return AmazonIVSBroadcastModule.setVideoConfig(config);
}

export function setAudioConfig(config: AudioConfig): Promise<void> {
  return AmazonIVSBroadcastModule.setAudioConfig(config);
}

export function setMixerLayout(layout: MixerLayoutConfig): Promise<void> {
  return AmazonIVSBroadcastModule.setMixerLayout(layout);
}

export function startScreenCapture(): Promise<void> {
  return AmazonIVSBroadcastModule.startScreenCapture();
}

export function stopScreenCapture(): Promise<void> {
  return AmazonIVSBroadcastModule.stopScreenCapture();
}

export function setCustomImageSource(buffer: any): Promise<void> {
  return AmazonIVSBroadcastModule.setCustomImageSource(buffer);
}

export function setCustomAudioSource(buffer: any): Promise<void> {
  return AmazonIVSBroadcastModule.setCustomAudioSource(buffer);
}

export function runBroadcastTest(
  options: BroadcastTestOptions
): Promise<BroadcastTestResult> {
  return AmazonIVSBroadcastModule.runBroadcastTest(options);
}

export function getSessionId(): Promise<string> {
  return AmazonIVSBroadcastModule.getSessionId();
}

export function setSimulcastConfig(config: any): Promise<void> {
  return AmazonIVSBroadcastModule.setSimulcastConfig(config);
}

export function setAutoReconnect(enabled: boolean): Promise<void> {
  return AmazonIVSBroadcastModule.setAutoReconnect(enabled);
}

interface IVSBroadcastPreviewProps extends ViewProps {}

export const IVSBroadcastPreview =
  requireNativeComponent<IVSBroadcastPreviewProps>("AmazonIVSBroadcastPreview");
