package com.appfolio.react.imageutils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import java.io.File;
import java.io.IOException;

public class AEImageUtilsModule extends ReactContextBaseJavaModule {
    private static final String IO_ERROR = "EFILE";
    private static final String OUT_OF_MEMORY_ERROR = "ELOWMEMORY";

    private final Context mContext;

    public AEImageUtilsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.mContext = reactContext;
    }

    @Override
    public String getName() {
        return "AEImageUtils";
    }

    @ReactMethod
    public void createResizedImage(@NonNull final String imagePath, final int newWidth, final int newHeight,
                                   @NonNull final String compressFormatString, final int quality, final int rotation,
                                   @Nullable final String outputPath, @NonNull final Promise promise) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.valueOf(compressFormatString);
                    Uri imageUri = Uri.parse(imagePath).normalizeScheme();

                    File resizedImage = ImageResizer.createResizedImage(mContext, imageUri,
                            newWidth, newHeight, compressFormat, quality, rotation, outputPath);

                    if (resizedImage.isFile()) {
                        WritableMap response = Arguments.createMap();
                        response.putString("path", resizedImage.getAbsolutePath());
                        response.putString("uri", Uri.fromFile(resizedImage).toString());
                        response.putString("name", resizedImage.getName());
                        response.putDouble("size", resizedImage.length());
                        promise.resolve(response);
                    } else {
                        throw new IOException("Error getting resized image path: " + Uri.fromFile(resizedImage).toString());
                    }
                } catch (IOException e) {
                    promise.reject(IO_ERROR, e);
                } catch (OutOfMemoryError e) {
                    promise.reject(OUT_OF_MEMORY_ERROR, e);
                } catch (Exception e) {
                    promise.reject(e);
                }
            }
        });
    }
}
