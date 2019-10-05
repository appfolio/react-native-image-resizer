package com.appfolio.react.imageutils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.util.Base64;

import com.facebook.react.modules.network.OkHttpClientProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Provide method to resize image
 */
final class ImageResizer {
    private final static String IMAGE_JPEG = "image/jpeg";
    private final static String IMAGE_PNG = "image/png";
    private final static String SCHEME_DATA = "data";
    private final static String SCHEME_HTTP = "http";
    private final static String SCHEME_HTTPS = "https";

    /**
     * Resize the specified bitmap, keeping its aspect ratio.
     */
    @NonNull
    private static Bitmap resizeImage(@NonNull final Bitmap sourceImage, final int maxWidth, final int maxHeight) {
        Bitmap newImage = sourceImage;

        if (maxHeight > 0 && maxWidth > 0) {
            final float width = sourceImage.getWidth();
            final float height = sourceImage.getHeight();

            final float ratio = Math.min((float)maxWidth / width, (float)maxHeight / height);

            final int finalWidth = (int) (width * ratio);
            final int finalHeight = (int) (height * ratio);
            try {
                newImage = Bitmap.createScaledBitmap(sourceImage, finalWidth, finalHeight, true);
            } catch(OutOfMemoryError e) {
                throw new OutOfMemoryError("Could not resize image: " + e.getMessage());
            }
        }

        return newImage;
    }

    /**
     * Rotate the specified bitmap with the given angle, in degrees.
     */
    @NonNull
    private static Bitmap rotateImage(@NonNull final Bitmap sourceImage, final float angle) {
        Bitmap newImage;

        final Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        try {
            newImage = Bitmap.createBitmap(sourceImage, 0, 0, sourceImage.getWidth(), sourceImage.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            throw new OutOfMemoryError("Could not rotate image: " + e.getMessage());
        }

        return newImage;
    }

    /**
     * Save the given bitmap in a directory. Extension is automatically generated using the bitmap format.
     *
     * Suppress the try-with-resources warning since Android Studio 3.0 extends support to all API levels.
     * See https://developer.android.com/studio/write/java8-support.html#supported_features
     */
    @SuppressLint("NewApi")
    private static File saveImage(@NonNull final Bitmap bitmap, @NonNull  final File saveDirectory,
                                  @NonNull final String fileName, @NonNull final Bitmap.CompressFormat compressFormat,
                                  final int quality)
            throws IOException {
        final File newFile = new File(saveDirectory, fileName + "." + compressFormat.name());
        if (!newFile.createNewFile()) {
            throw new IOException("The file already exists");
        }

        try (final FileOutputStream output = new FileOutputStream(newFile)) {
            bitmap.compress(compressFormat, quality, output);

            output.flush();
            output.close();
        }

        return newFile;
    }

    /**
     * Get orientation by reading Image metadata
     *
     * Suppress the try-with-resources warning since Android Studio 3.0 extends support to all API levels.
     * See https://developer.android.com/studio/write/java8-support.html#supported_features
     */
    @SuppressLint("NewApi")
    private static int getOrientation(@NonNull final Context context, @NonNull final Uri uri) {
        try {
            final ContentResolver cr = context.getContentResolver();
            try (final InputStream input = cr.openInputStream(uri)) {
                if (input != null) {
                    final ExifInterface ei = new ExifInterface(input);
                    return getOrientation(ei);
                }
            }
        } catch (Exception ignored) {
            // if there's an issue reading the file or content, we'll just assume no rotation
            // this will definitely fail if given a data URI
        }

        return 0;
    }

    /**
     * Convert metadata to degrees
     */
    private static int getOrientation(@NonNull final ExifInterface exif) {
        final int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    /**
     * Compute the inSampleSize value to use to load a bitmap.
     * Adapted from https://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     */
    private static int calculateInSampleSize(@NonNull final BitmapFactory.Options options,
                                             final int reqWidth, final int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Load a Bitmap using the {@link ContentResolver} of the current
     * {@link Context} (for real files or gallery images for example).
     *
     * Note that, when options.inJustDecodeBounds = true, we actually expect sourceImage to remain
     * as null (see https://developer.android.com/training/displaying-bitmaps/load-bitmap.html), so
     * getting null sourceImage at the completion of this method is not always worthy of an error.
     *
     * Suppress the try-with-resources warning since Android Studio 3.0 extends support to all API levels.
     * See https://developer.android.com/studio/write/java8-support.html#supported_features
     */
    @SuppressLint("NewApi")
    @Nullable
    private static Bitmap loadBitmapFromFile(@NonNull final Context context, @NonNull final Uri imageUri,
                                             @NonNull final BitmapFactory.Options options) throws IOException {
        Bitmap sourceImage = null;
        final ContentResolver cr = context.getContentResolver();
        try (final InputStream input = cr.openInputStream(imageUri)) {
            if (input != null) {
                sourceImage = BitmapFactory.decodeStream(input, null, options);
                input.close();
            }
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("Unable to load image into Bitmap: " + e.getMessage());
        }

        return sourceImage;
    }

    /**
     * Load a Bitmap from a Base64 encoded jpg or png.
     * Format is as such:
     * png: 'data:image/png;base64,iVBORw0KGgoAA...'
     * jpg: 'data:image/jpeg;base64,/9j/4AAQSkZJ...'
     */
    @Nullable
    private static Bitmap loadBitmapFromBase64(@NonNull final Uri imageUri,
                                               @NonNull final BitmapFactory.Options options) {
        Bitmap sourceImage = null;
        final String imagePath = imageUri.getSchemeSpecificPart();
        final int commaLocation = imagePath.indexOf(',');
        if (commaLocation != -1) {
            final String mimeType = imagePath.substring(0, commaLocation).replace('\\','/').toLowerCase();
            final boolean isJpeg = mimeType.startsWith(IMAGE_JPEG);
            final boolean isPng = !isJpeg && mimeType.startsWith(IMAGE_PNG);

            if (isJpeg || isPng) {
                // base64 image. Convert to a bitmap.
                final String encodedImage = imagePath.substring(commaLocation + 1);
                final byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                sourceImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length, options);
            }
        }

        return sourceImage;
    }

    /**
     * Load a Bitmap from an http or https URL.
     */
    @Nullable
    public static Bitmap loadBitmapFromURL(@NonNull final Uri imageUri,
                                           @NonNull final BitmapFactory.Options options) throws IOException {
        Bitmap sourceImage = null;
        URL url = null;

        try {
            url = new URL(imageUri.toString());
        } catch (MalformedURLException e) {
            throw new MalformedURLException("Unable to load image from a malformed URL: " + e.getMessage());
        }

        OkHttpClient client = OkHttpClientProvider.getOkHttpClient();
        Request request = new Request.Builder()
            .url(url)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response.code());
      
            InputStream input = response.body().byteStream();
            sourceImage = BitmapFactory.decodeStream(input);
            input.close();
        } catch (IOException e) {
            throw new IOException("Unable to download image", e);
        }

        return sourceImage;
    }

    /**
     * Load a Bitmap from a Uri which can either be Base64 encoded or a path to a file or content scheme
     */
    @Nullable
    private static Bitmap loadBitmap(@NonNull final Context context, @NonNull final Uri imageUri,
                                     @NonNull final BitmapFactory.Options options) throws IOException {
        Bitmap sourceImage = null;
        final String imageUriScheme = imageUri.getScheme();
        if (imageUriScheme == null || ContentResolver.SCHEME_CONTENT.equals(imageUriScheme) || ContentResolver.SCHEME_FILE.equals(imageUriScheme)) {
            sourceImage = loadBitmapFromFile(context, imageUri, options);
        } else if (SCHEME_DATA.equals(imageUriScheme)) {
            sourceImage = loadBitmapFromBase64(imageUri, options);
        } else if (SCHEME_HTTP.equals(imageUriScheme) || SCHEME_HTTPS.equals(imageUriScheme)) {
            sourceImage = loadBitmapFromURL(imageUri, options);
        }

        return sourceImage;
    }

    /**
     * Load a Bitmap with sane decoding options based on the requested size
     */
    @Nullable
    private static Bitmap loadBitmap(@NonNull final Context context, @NonNull final Uri imageUri,
                                     final int newWidth, final int newHeight) throws IOException  {
        // Decode the image bounds to find the size of the source image.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inDither = true; // known to improve picture quality at low cost
        loadBitmap(context, imageUri, options);

        // Set a sample size according to the image size to lower memory usage.
        options.inSampleSize = calculateInSampleSize(options, newWidth, newHeight);
        options.inJustDecodeBounds = false;
        return loadBitmap(context, imageUri, options);
    }

    /**
     * Create a resized version of the given image.
     */
    static File createResizedImage(@NonNull final Context context, @NonNull final Uri imageUri,
                                   final int newWidth, final int newHeight, @NonNull final Bitmap.CompressFormat compressFormat,
                                   final int quality, int rotation, @Nullable final String outputPath) throws IOException  {
        final Bitmap sourceImage = loadBitmap(context, imageUri, newWidth, newHeight);

        if (sourceImage == null) {
            throw new IOException("Unable to load source image");
        }

        // Scale it first so there are fewer pixels to transform in the rotation
        Bitmap scaledImage = null;
        try {
            scaledImage = ImageResizer.resizeImage(sourceImage, newWidth, newHeight);
        } finally {
            if (sourceImage != scaledImage) {
                sourceImage.recycle();
            }
        }

        // Rotate if necessary
        Bitmap rotatedImage = null;
        int orientation = getOrientation(context, imageUri);
        rotation = orientation + rotation;
        try {
            rotatedImage = ImageResizer.rotateImage(scaledImage, rotation);
        } finally {
            if (scaledImage != rotatedImage) {
                scaledImage.recycle();
            }
        }

        // Save the resulting image
        final File newFile;
        try {
            File path = context.getCacheDir();
            if (outputPath != null) {
                path = new File(outputPath);
            }

            newFile = ImageResizer.saveImage(rotatedImage, path,
                    Long.toString(new Date().getTime()), compressFormat, quality);
        } finally {
            // Clean up remaining image
            rotatedImage.recycle();
        }

        return newFile;
    }
}
