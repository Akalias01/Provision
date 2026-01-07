package com.rezon.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * File picker plugin for selecting audiobooks and documents.
 */
@CapacitorPlugin(name = "FilePicker")
public class FilePickerPlugin extends Plugin {

    private static final String[] AUDIO_MIME_TYPES = {
        "audio/*",
        "audio/mpeg",
        "audio/mp3",
        "audio/mp4",
        "audio/m4a",
        "audio/m4b",
        "audio/x-m4b",
        "audio/aac",
        "audio/ogg",
        "audio/flac",
        "audio/wav"
    };

    private static final String[] DOCUMENT_MIME_TYPES = {
        "application/pdf",
        "application/epub+zip",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    };

    private static final String[] ALL_MIME_TYPES = {
        "audio/*",
        "application/pdf",
        "application/epub+zip",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    };

    @PluginMethod
    public void pickFiles(PluginCall call) {
        String type = call.getString("type", "all");
        boolean multiple = call.getBoolean("multiple", true);

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple);

        String[] mimeTypes;
        switch (type) {
            case "audio":
                mimeTypes = AUDIO_MIME_TYPES;
                break;
            case "document":
                mimeTypes = DOCUMENT_MIME_TYPES;
                break;
            default:
                mimeTypes = ALL_MIME_TYPES;
        }

        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        startActivityForResult(call, intent, "pickFilesResult");
    }

    @ActivityCallback
    private void pickFilesResult(PluginCall call, ActivityResult result) {
        if (call == null) {
            return;
        }

        JSArray files = new JSArray();

        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Intent data = result.getData();

            if (data.getClipData() != null) {
                // Multiple files selected
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    JSObject fileInfo = getFileInfo(uri);
                    if (fileInfo != null) {
                        files.put(fileInfo);
                    }
                }
            } else if (data.getData() != null) {
                // Single file selected
                Uri uri = data.getData();
                JSObject fileInfo = getFileInfo(uri);
                if (fileInfo != null) {
                    files.put(fileInfo);
                }
            }
        }

        JSObject ret = new JSObject();
        ret.put("files", files);
        call.resolve(ret);
    }

    @PluginMethod
    public void readFile(PluginCall call) {
        String uriString = call.getString("uri");
        if (uriString == null) {
            call.reject("URI is required");
            return;
        }

        try {
            Uri uri = Uri.parse(uriString);

            // Take persistable permission
            getContext().getContentResolver().takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                call.reject("Could not open file");
                return;
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            inputStream.close();

            String base64Data = Base64.encodeToString(buffer.toByteArray(), Base64.DEFAULT);

            JSObject ret = new JSObject();
            ret.put("data", base64Data);
            ret.put("uri", uriString);
            call.resolve(ret);

        } catch (Exception e) {
            call.reject("Error reading file: " + e.getMessage());
        }
    }

    @PluginMethod
    public void getFileUrl(PluginCall call) {
        String uriString = call.getString("uri");
        if (uriString == null) {
            call.reject("URI is required");
            return;
        }

        try {
            Uri uri = Uri.parse(uriString);

            // Take persistable permission for long-term access
            try {
                getContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (SecurityException e) {
                // Permission might already be taken or not available
            }

            JSObject ret = new JSObject();
            ret.put("url", uri.toString());
            call.resolve(ret);

        } catch (Exception e) {
            call.reject("Error getting file URL: " + e.getMessage());
        }
    }

    private JSObject getFileInfo(Uri uri) {
        try {
            // Take persistable permission
            getContext().getContentResolver().takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            JSObject fileInfo = new JSObject();
            fileInfo.put("uri", uri.toString());

            // Get file name and size
            Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

                if (nameIndex >= 0) {
                    fileInfo.put("name", cursor.getString(nameIndex));
                }
                if (sizeIndex >= 0) {
                    fileInfo.put("size", cursor.getLong(sizeIndex));
                }
                cursor.close();
            }

            // Get MIME type
            String mimeType = getContext().getContentResolver().getType(uri);
            fileInfo.put("mimeType", mimeType != null ? mimeType : "application/octet-stream");

            // Get file extension
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension != null) {
                fileInfo.put("extension", extension);
            }

            return fileInfo;

        } catch (Exception e) {
            return null;
        }
    }
}
