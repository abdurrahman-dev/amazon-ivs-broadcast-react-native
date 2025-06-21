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
    return IVSPreviewView()
  }

  @objc override func constantsToExport() -> [AnyHashable : Any]! {
    return [:]
  }
} 