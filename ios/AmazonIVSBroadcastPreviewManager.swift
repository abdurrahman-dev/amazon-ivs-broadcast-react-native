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
    let preview = IVSImagePreviewView()
    if let session = AmazonIVSBroadcastModule.sharedSession {
      preview.session = session
    }
    return preview
  }

  @objc override func constantsToExport() -> [AnyHashable : Any]! {
    return [:]
  }
} 