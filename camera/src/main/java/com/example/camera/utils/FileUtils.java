package com.example.camera.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "camera_app");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.i("camera_app", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }
        return mediaFile;
    }

    /**
     *
     * @param data
     */
    public static void saveTakePicture(byte[] data) {
        //save the picture
        File pictureFile = FileUtils.getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            Log.i(TAG, "Error creating media file, check storage permissions");
            return;
        }
        try {
            FileOutputStream os = new FileOutputStream(pictureFile);
            os.write(data);
            os.close();

            Log.i(TAG, "Save Success!!");
        } catch (FileNotFoundException e) {
            Log.i(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.i(TAG, "Error accessing file: " + e.getMessage());
        }
    }
}
