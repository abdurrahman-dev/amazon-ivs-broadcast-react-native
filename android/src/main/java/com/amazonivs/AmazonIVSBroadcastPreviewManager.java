package com.amazonivs;

import android.content.Context;
import android.view.View;

import com.amazonaws.ivs.broadcast.PreviewView;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import androidx.annotation.NonNull;

public class AmazonIVSBroadcastPreviewManager extends SimpleViewManager<PreviewView> {
    public static final String REACT_CLASS = "AmazonIVSBroadcastPreview";

    @NonNull
    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @NonNull
    @Override
    protected PreviewView createViewInstance(@NonNull ThemedReactContext reactContext) {
        return new PreviewView(reactContext);
    }
} 