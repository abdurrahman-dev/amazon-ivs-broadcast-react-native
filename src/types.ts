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
  /** Slot'un sol üst köşe x koordinatı (pixel) */
  x?: number;
  /** Slot'un sol üst köşe y koordinatı (pixel) */
  y?: number;
  /** Slot'un genişliği (pixel) */
  width?: number;
  /** Slot'un yüksekliği (pixel) */
  height?: number;
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

/** Broadcast test sonucu önerisi */
export type BroadcastTestRecommendation = {
  type: string;
  message: string;
  severity?: "info" | "warning" | "error";
};

export type BroadcastTestResult = {
  status: string;
  recommendations: BroadcastTestRecommendation[];
};

/** Custom video kaynağı için buffer tipi */
export type CustomImageBuffer = {
  data: Uint8Array;
  width: number;
  height: number;
  pixelFormat: string;
};

/** Custom audio kaynağı için buffer tipi */
export type CustomAudioBuffer = {
  data: Uint8Array;
  sampleRate: number;
  channels: number;
};

export type IVSEvent =
  | "stateChanged"
  | "error"
  | "deviceChanged"
  | "bitrateChanged"
  | "networkQualityChanged"
  | "reconnecting"
  | "reconnected";

/** Simulcast ayarları için tip (ileride genişletilebilir) */
export interface SimulcastConfig {
  // ör: enabled?: boolean;
}
