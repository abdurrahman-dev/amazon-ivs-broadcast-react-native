package com.amazonivs;

import android.content.Context;
import android.view.View;

import com.amazonaws.ivs.broadcast.ImagePreviewView;
import com.amazonaws.ivs.broadcast.BroadcastConfiguration.AspectMode;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.common.MapBuilder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

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
            previewView.attachBroadcastSession(AmazonIVSBroadcastModule.sharedSession);
            previewView.setMirrored(true); // Ön kamera için varsayılan olarak aynalama aktif
        }
        return previewView;
    }

    @ReactProp(name = "aspectMode")
    public void setAspectMode(ImagePreviewView view, String mode) {
        switch (mode) {
            case "fit":
                view.setAspectMode(AspectMode.FIT);
                break;
            case "fill":
                view.setAspectMode(AspectMode.FILL);
                break;
            default:
                view.setAspectMode(AspectMode.FIT);
        }
    }

    @ReactProp(name = "mirrored")
    public void setMirrored(ImagePreviewView view, boolean mirrored) {
        view.setMirrored(mirrored);
    }

    @Nullable
    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String, Object>builder()
            .put("onErrorOccurred", MapBuilder.of("registrationName", "onErrorOccurred"))
            .build();
    }

    @Nullable
    @Override
    public Map<String, Object> getExportedViewConstants() {
        return MapBuilder.<String, Object>of(
            "AspectMode", MapBuilder.of(
                "fit", "fit",
                "fill", "fill"
            )
        );
    }
} 