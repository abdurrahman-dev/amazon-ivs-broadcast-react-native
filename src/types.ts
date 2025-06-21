export type DevicePosition = "front" | "back" | "unknown";

export type Device = {
  id: string;
  name: string;
  position?: DevicePosition;
  type: "camera" | "microphone";
};

export type VideoConfig = {
  bitrate?: number;
  width?: number;
  height?: number;
  targetFramerate?: number;
  keyframeInterval?: number;
  maxBitrate?: number;
  minBitrate?: number;
  qualityOptimization?: "quality" | "latency";
  useH265?: boolean;
  enableTranscoding?: boolean;
};

export type AudioConfig = {
  bitrate?: number;
  channels?: number;
  sampleRate?: number;
  enableEchoCancellation?: boolean;
  enableNoiseSuppression?: boolean;
};

export type MixerConfig = {
  canvasWidth?: number;
  canvasHeight?: number;
  backgroundColor?: string;
};

export type BroadcastConfig = {
  video?: VideoConfig;
  audio?: AudioConfig;
  mixer?: MixerConfig;
  cameraPosition?: DevicePosition;
  initialBitrate?: number;
  targetBitrate?: number;
  useAutoBitrate?: boolean;
  autoMaxBitrate?: number;
  autoMinBitrate?: number;
  enableAutoReconnect?: boolean;
  autoReconnectMaxRetries?: number;
  autoReconnectRetryInterval?: number;
};

export type BroadcastOptions = {
  rtmpsUrl: string;
  streamKey: string;
} & BroadcastConfig;

export type NetworkHealth = "excellent" | "good" | "poor";
export type BroadcastQuality = "excellent" | "good" | "poor";

export type TransmissionStats = {
  currentBitrate: number;
  recommendedBitrate: number;
  roundTripTime: number;
  networkHealth: NetworkHealth;
  broadcastQuality: BroadcastQuality;
  fps: number;
  width: number;
  height: number;
  encoderName: string;
  audioChannels: number;
  audioSampleRate: number;
};

export type AspectMode = "fit" | "fill";

export type PreviewProps = {
  aspectMode?: AspectMode;
  mirrored?: boolean;
  onErrorOccurred?: (error: Error) => void;
  style?: any;
};

export type IVSEvent =
  | "stateChanged"
  | "error"
  | "deviceChanged"
  | "bitrateChanged"
  | "networkHealthChanged"
  | "audioSessionInterrupted"
  | "audioSessionResumed"
  | "cameraError"
  | "microphoneError"
  | "reconnecting"
  | "reconnected"
  | "streamHealthChanged"
  | "resourceLimitExceeded"
  | "streamInterrupted"
  | "streamResumed";

export type ErrorEvent = {
  message: string;
  code: string;
  isFatal: boolean;
};

export type DeviceChangedEvent = {
  devices: Device[];
};

export type BitrateChangedEvent = {
  bitrate: number;
  isAuto: boolean;
};

export type NetworkHealthChangedEvent = {
  networkHealth: NetworkHealth;
  quality: BroadcastQuality;
};

export type StreamHealthEvent = {
  health: number;
  reason?: string;
};

export type ResourceLimitEvent = {
  type: "cpu" | "memory" | "battery";
  value: number;
  threshold: number;
};

export type BroadcastState =
  | "disconnected"
  | "connecting"
  | "connected"
  | "disconnecting"
  | "error"
  | "invalid";

export type BroadcastMetrics = {
  cpu: number;
  memory: number;
  battery: number;
  temperature: number;
};

export type AudioSessionConfiguration = {
  category: "ambient" | "playback" | "record" | "playAndRecord";
  mode: "default" | "voiceChat" | "gameChat" | "videoChat";
  mixWithOthers: boolean;
};
