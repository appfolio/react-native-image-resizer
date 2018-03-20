package com.appfolio.react.imageutils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import java.io.File;
import java.io.IOException;

public class AEImageUtilsModule extends ReactContextBaseJavaModule {
    private Context context;

    public AEImageUtilsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.context = reactContext;
    }

    @Override
    public String getName() {
        return "AEImageUtils";
    }

    @ReactMethod
    public void createResizedImage(String imagePath, int newWidth, int newHeight, String compressFormat,
                                   int quality, int rotation, String outputPath, final Callback successCb, final Callback failureCb) {
        try {
            createResizedImageWithExceptions(imagePath, newWidth, newHeight, compressFormat, quality,
                    rotation, outputPath, successCb, failureCb);
        } catch (IOException | OutOfMemoryError e) {
            failureCb.invoke(e.getMessage());
        }
    }

    private void createResizedImageWithExceptions(String imagePath, int newWidth, int newHeight,
                                                  String compressFormatString, int quality, int rotation, String outputPath,
                                                  final Callback successCb, final Callback failureCb) throws IOException {
        Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.valueOf(compressFormatString);
        Uri imageUri = Uri.parse(imagePath).normalizeScheme();

        File resizedImage = ImageResizer.createResizedImage(this.context, imageUri, newWidth,
                newHeight, compressFormat, quality, rotation, outputPath);

        // If resizedImagePath is empty and this wasn't caught earlier, throw.
        if (resizedImage.isFile()) {
            WritableMap response = Arguments.createMap();
            response.putString("path", resizedImage.getAbsolutePath());
            response.putString("uri", Uri.fromFile(resizedImage).toString());
            response.putString("name", resizedImage.getName());
            response.putDouble("size", resizedImage.length());
            // Invoke success
            successCb.invoke(response);
        } else {
            failureCb.invoke("Error getting resized image path");
        }
    }
}
