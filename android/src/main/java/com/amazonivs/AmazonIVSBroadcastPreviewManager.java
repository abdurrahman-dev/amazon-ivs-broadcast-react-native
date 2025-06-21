package com.amazonivs;

import android.content.Context;
import android.view.View;

import com.amazonaws.ivs.broadcast.ImagePreviewView;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import androidx.annotation.NonNull;

public class AmazonIVSBroadcastPreviewManager extends SimpleViewManager<ImagePreviewView> {
    public static final String REACT_CLASS = "AmazonIVSBroadcastPreview";

    @NonNull
    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @NonNull
    @Override
    protected ImagePreviewView createViewInstance(@NonNull ThemedReactContext reactContext) {
        ImagePreviewView previewView = new ImagePreviewView(reactContext);
        if (AmazonIVSBroadcastModule.sharedSession != null) {
            previewView.setSession(AmazonIVSBroadcastModule.sharedSession);
            previewView.setMirrored(true); // Ön kamera için varsayılan olarak aynalama aktif
        }
        return previewView;
    }
} 