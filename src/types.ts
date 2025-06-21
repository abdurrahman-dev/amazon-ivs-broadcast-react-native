export type CameraDevice = { id: string; name: string };
export type MicrophoneDevice = { id: string; name: string };

export type VideoConfig = {
  bitrate?: number;
  width?: number;
  height?: number;
  framerate?: number;
};

export type AudioConfig = {
  bitrate?: number;
};

export type MixerSlotConfig = {
  name: string;
  zIndex?: number;
};

export type MixerLayoutConfig = {
  slots: MixerSlotConfig[];
};

export type BroadcastOptions = {
  rtmpsUrl: string;
  streamKey: string;
};

export type BroadcastTestOptions = {
  rtmpsUrl: string;
  streamKey: string;
};

export type BroadcastTestResult = {
  status: string;
  recommendations: any[];
};

export type IVSEvent =
  | "stateChanged"
  | "error"
  | "deviceChanged"
  | "bitrateChanged"
  | "networkQualityChanged"
  | "reconnecting"
  | "reconnected";
