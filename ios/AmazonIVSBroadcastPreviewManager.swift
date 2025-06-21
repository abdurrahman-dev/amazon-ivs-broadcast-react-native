import Foundation
import React
import AmazonIVSBroadcast
import UIKit

@objc(AmazonIVSBroadcastPreviewManager)
class AmazonIVSBroadcastPreviewManager: RCTViewManager {
  override static func requiresMainQueueSetup() -> Bool {
    return true
  }

  override func view() -> UIView! {
    let preview = IVSImagePreviewView(frame: .zero)
    if let session = AmazonIVSBroadcastModule.sharedSession {
      preview.session = session
      preview.setMirrored(true)
    }
    return preview
  }

  @objc func setAspectMode(_ preview: IVSImagePreviewView, mode: NSString) {
    switch mode as String {
    case "fit":
      preview.aspectMode = .fit
    case "fill":
      preview.aspectMode = .fill
    default:
      preview.aspectMode = .fit
    }
  }

  @objc func setMirrored(_ preview: IVSImagePreviewView, mirrored: Bool) {
    preview.setMirrored(mirrored)
  }

  override func constantsToExport() -> [AnyHashable : Any]! {
    return [
      "AspectMode": [
        "fit": "fit",
        "fill": "fill"
      ]
    ]
  }
} 