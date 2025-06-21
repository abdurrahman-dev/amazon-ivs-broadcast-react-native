Pod::Spec.new do |s|
  s.name         = "amazon-ivs-broadcast-react-native"
  s.version      = "1.1.1"
  s.summary      = "Amazon IVS Broadcast için React Native kapsayıcı modül (iOS ve Android)"
  s.description  = <<-DESC
    Amazon IVS Broadcast için React Native kapsayıcı modül (iOS ve Android). Kamera, mikrofon, yayın yönetimi, gelişmiş IVS özellikleri.
  DESC
  s.homepage     = "https://github.com/abdurrahman-dev/amazon-ivs-broadcast-react-native"
  s.license      = { :type => "MIT" }
  s.author       = { "abdurrahman-dev" => "abdur.caglar@gmail.com" }
  s.platform     = :ios, "14.0"
  s.source       = { :git => "https://github.com/abdurrahman-dev/amazon-ivs-broadcast-react-native.git", :tag => "#{s.version}" }
  s.source_files  = "ios/**/*.{h,m,swift}"
  s.requires_arc = true
  s.dependency "React-Core"
  s.dependency "AmazonIVSBroadcast", "~> 1.31.0"
  s.swift_version = "5.0"
end 