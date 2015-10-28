package dan.dit.whatsthat.util.webPhotoSharing;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementing requests for dumpyourphoto.com API as specified by
 * https://github.com/DumpYourPhoto/API-Documentation
 * Created by daniel on 27.10.15.
 */
public class DumpYourPhotoController {

    /**
     * The accepted format of the response, specified in the header. Do not change. API supports
     * html, json, csv, xml.
     * The output for the url field is buggy for csv since this would be a map (an array) of urls.
     *
     */
    private static final String ACCEPT_HEADER = "application/csv";

    // though this should not be public, I trust people to not mess with my account and this key
    private static final String API_KEY_PARAMETER =
            "api_key=kfxCwdBY5OpxfxGerTKFM9MEOQYwxSFYdHSeCHEQ1zxXKaTopnImFDETk2pTdRSjP6umuAAWuXVPX4GFVtaaw99ukbFbxIvt6iVY";

    private static final String BASE_DOWNLOAD_URL = "https://static.dyp.im/";
    private static final String BASE_URL = "https://api.dumpyourphoto.com/v1/";
    private static final String DEFAULT_DOWNLOAD_PHOTO_TYPE = "full"; // must be full to get regular
    // uploaded photo. Possible values by API are small,medium, large, full.
    // . Size marks different links to download a photo
    
    /**
     * If the album that gets created for each user once will be public or not.
     */
    public static final boolean IS_ALBUM_PUBLIC_DEFAULT = true;

    private static HttpURLConnection makeOpenConnection(String urlString) {
        if (TextUtils.isEmpty(urlString)) {
            return null;
        }
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            Log.e("DumpYourPhoto", "Error creating url for dump your photo: " + e);
            return null;
        }
        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException ioe) {
            Log.e("DumpYourPhoto", "Error opening connection: " + ioe);
            return null;
        }
        return urlConnection;
    }

    private static ResponseMap obtainResponse(String urlString) {
        HttpURLConnection urlConnection = makeOpenConnection(urlString);
        if (urlConnection == null) {
            return null;
        }
        ResponseMap response = null;
        try {
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Accept", ACCEPT_HEADER);

            Log.d("DumpYourPhoto", "Starting obtaining response: " + urlConnection);

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            response = new ResponseMap(in);

        } catch (IOException ioe) {
            Log.e("DumpYourPhoto", "Error with opened url connection for obtaining response: " +
                    ioe);
        } finally {
            urlConnection.disconnect();
        }
        Log.d("DumpYourPhoto", "Got response: " + response);
        return response;
    }

    private static ResponseMap postOrPutDataObtainResponse(boolean post, HttpURLConnection
                                                           urlConnection) {
        if (urlConnection == null) {
            return null;
        }
        ResponseMap response = null;
        try {
            urlConnection.setRequestMethod(post ? "POST" : "PUT");
            urlConnection.setRequestProperty("Accept", ACCEPT_HEADER);

            Log.d("DumpYourPhoto", "Starting posting/putting data: " + post + ": " + urlConnection);
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            response = new ResponseMap(in);

        } catch (IOException ioe) {
            Log.e("DumpYourPhoto", "Error with opened url connection for posting/putting data: " +
                    ioe);
        } finally {
            urlConnection.disconnect();
        }
        Log.d("DumpYourPhoto", "Got response: " + response);
        return response;
    }

    private static ResponseMap postBitmapDataObtainResponse(String urlString,
                                                            File bitmapFile) {
        HttpURLConnection urlConnection = makeOpenConnection(urlString);
        if (urlConnection == null || bitmapFile == null || !bitmapFile.exists()) {
            return null; // no sense in trying
        }
        Log.d("DumpYourPhoto", "Trying to post bitmap : " + bitmapFile + " to" +
                " url " + urlString);
        ResponseMap response = null;
        try {
            urlConnection.setUseCaches(false);
            urlConnection.setDoOutput(true);

            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Accept", ACCEPT_HEADER);
            urlConnection.setRequestProperty("Connection", "Keep-Alive"); // required?
            urlConnection.setRequestProperty("Cache-Control", "no-cache");

            final String crlf = "\r\n";
            final String twoHypens ="--";
            final String boundary = "WebKitFormBoundary7MA4YWxkTrZu0gW";
            urlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" +
                    boundary);

            // start content wrapper
            urlConnection.connect();
            OutputStream requestbase = urlConnection.getOutputStream();
            OutputStream request = new BufferedOutputStream(requestbase);
            request.write((twoHypens + boundary + crlf).getBytes());
            request.write(("Content-Disposition:form-data; name=\"files\""
                    + "; filename=\"" + bitmapFile.getName() + "\""
                    + crlf + "Content-Type: image/png" + crlf + crlf).getBytes
                    ());

            // send bitmap
            /* //For sending raw bitmap:
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)) {
                Log.e("DumpYourPhoto", "Failed compressing bitmap to stream.");
                return null;
            }
            request.write(bos.toByteArray());*/
            // For sending bitmap as file
            FileInputStream fileStream = new FileInputStream(bitmapFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = fileStream.read(buffer)) != -1) {
                request.write(buffer, 0, read);
            }

            // end content wrapper
            request.write((crlf + twoHypens + boundary + twoHypens + crlf).getBytes());

            request.flush();
            request.close();

            final int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_CREATED && responseCode != HttpURLConnection
                    .HTTP_OK) {
                Log.e("DumpYourPhoto", "Response when uploading not ok or created: " + urlConnection
                        .getResponseCode() + " for url " + urlString + " response=" +
                        urlConnection.getResponseMessage());
                return null;
            }
            // get response
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            response = new ResponseMap(in);

        } catch (IOException e) {
            Log.e("DumpYourPhoto", "Error posting bitmap data : " + e);
        } finally {
            urlConnection.disconnect();
        }
        return response;
    }

    private static ResponseMap deleteAlbum(String albumHash) {
        if (TextUtils.isEmpty(albumHash)) {
            return null;
        }
        return obtainResponse(BASE_URL + "albums/" + albumHash);
    }

    private static ResponseMap getAlbums() {
        return obtainResponse(BASE_URL + "albums" + "?" + API_KEY_PARAMETER);
    }

    public static String makeAlbum(String albumName) {
        ResponseMap map = makeAlbumExecute(albumName);
        if (map == null) {
            return null;
        }
        return map.getEntry("hash", 0, null);
    }

    public static String updateAlbum(String albumHash, String newAlbumName, boolean newIsPublic) {
        ResponseMap map = updateAlbumExecute(albumHash, newAlbumName, newIsPublic);
        if (map == null) {
            return null;
        }
        return map.getEntry("hash", 0, albumHash);
    }

    private static ResponseMap updateAlbumExecute(String albumHash, String newAlbumName, boolean
                                                  newIsPublic) {
        if (TextUtils.isEmpty(albumHash)) {
            return null;
        }
        if (newAlbumName == null) {
            return null;
        }
        StringBuilder url = new StringBuilder();
        url.append(BASE_URL)
                .append("albums/")
                .append(albumHash)
                .append("?")
                .append(API_KEY_PARAMETER);
        try {
            String encoded = URLEncoder.encode(newAlbumName, "UTF-8");
            url.append('&')
                    .append("name=")
                    .append(encoded);
        } catch (UnsupportedEncodingException e) {
            Log.e("DumpYourPhoto", "Error encoding url: " + url + " : " + e);
            return null;
        }
        url.append('&')
                .append("public=")
                .append(newIsPublic ? '1' : '0');

        HttpURLConnection urlConnection = makeOpenConnection(url.toString());
        if (urlConnection == null) {
            return null;
        }
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        // Returns album with keys: id, name, hash, public, photos.
        ResponseMap response = postOrPutDataObtainResponse(false, urlConnection);
        Log.d("DumpYourPhoto", "Album created response: " + response);
        return response;
    }

    private static ResponseMap makeAlbumExecute(String albumName) {
        if (albumName == null) {
            return null;
        }
        StringBuilder url = new StringBuilder();
        url.append(BASE_URL)
                .append("albums")
                .append("?")
                .append(API_KEY_PARAMETER);
        try {
            String encoded = URLEncoder.encode(albumName, "UTF-8");
            url.append('&')
                    .append("name=")
                    .append(encoded);
        } catch (UnsupportedEncodingException e) {
            Log.e("DumpYourPhoto", "Error encoding url: " + url + " : " + e);
            return null;
        }
        url.append('&')
                .append("public=")
                .append(IS_ALBUM_PUBLIC_DEFAULT ? '1' : '0');

        HttpURLConnection urlConnection = makeOpenConnection(url.toString());
        if (urlConnection == null) {
            return null;
        }
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        // Returns album with keys: id, name, hash, public, photos.
        ResponseMap response = postOrPutDataObtainResponse(true, urlConnection);
        Log.d("DumpYourPhoto", "Album created response: " + response);
        return response;
    }

    public static String uploadPhotoToAlbum(String albumHash, File bitmapFile) {
        ResponseMap map = uploadPhotoToAlbumExecute(albumHash, bitmapFile);
        if (map == null) {
            return null;
        }
        return map.getEntry("hash", 0, "") + "/" + map.getEntry("file_name", 0, "");
    }

    private static ResponseMap uploadPhotoToAlbumExecute(String albumHash,  File
            bitmapFile) {
        if (TextUtils.isEmpty(albumHash) || bitmapFile == null || !bitmapFile.exists()) {
            Log.e("DumpYourPhoto", "No need to try uploading photo " + bitmapFile + " to album "+
                    albumHash);
            return null; // no need to try
        }
        Log.d("DumpYourPhoto", "Uploading to album file name " + bitmapFile.getName());
        StringBuilder url = new StringBuilder();
        url.append(BASE_URL)
            .append("albums/")
                .append(albumHash)
                .append("/photos?")
                .append(API_KEY_PARAMETER);
        ResponseMap response = postBitmapDataObtainResponse(url.toString(), bitmapFile);
        Log.d("DumpYourPhoto", "Uploaded photo to album, response: " + response);
        return response;
    }

    public static String makeDownloadLinkExecute(String photoUploadLink) {
        return BASE_DOWNLOAD_URL + (photoUploadLink.startsWith("/") ? photoUploadLink
                .substring(1) : photoUploadLink);
    }

    private static class ResponseMap {
        private Map<String, String[]> mData;

        public ResponseMap(InputStream response) throws IOException {
            InputStreamReader reader = new InputStreamReader(response);
            StringBuilder totalResult = new StringBuilder();
            int read;
            char[] buffer = new char[1024];
            try {
                while ((read = reader.read(buffer)) > 0) {
                    totalResult.append(buffer, 0, read);
                }
            } catch (IOException ioe) {
                Log.e("DumpYourPhoto", "Error during creating response map: " + ioe);
            }
            String[] keysAndValues = totalResult.toString().split("\n");
            if (keysAndValues.length < 2) {
                throw new IOException("Illegal response, too little data to form response map: "
                        + totalResult.toString());
            }
            String[] keys = keysAndValues[0].split(",");
            mData = new HashMap<>(keys.length);
            for (int valuesIndex = 1; valuesIndex < keysAndValues.length; valuesIndex++) {
                String[] values = keysAndValues[valuesIndex].split(",");
                for (int i = 0; i < Math.min(keys.length, values.length); i++) {
                    String currKey = cropQuotationMarks(keys[i]);
                    String[] currValues = mData.get(currKey);
                    if (currValues == null) {
                        currValues = new String[keysAndValues.length - 1];
                        mData.put(currKey, currValues);
                    }
                    currValues[valuesIndex - 1] = (cropQuotationMarks(values[i]));
                }
            }
            response.close();
            Log.d("DumpYourPhoto", "Received data: " + totalResult.toString());
        }

        private static String cropQuotationMarks(String toCrop) {
            if (toCrop.length() == 0) {
                return toCrop;
            }
            final char QUOTATION_MARK = '\"';
            if (toCrop.length() == 1) {
                return toCrop.charAt(0) == QUOTATION_MARK ? "" : toCrop;
            }
            // string has at least two characters
            if (toCrop.charAt(0) == QUOTATION_MARK) {
                toCrop = toCrop.substring(1);
            }
            if (toCrop.charAt(toCrop.length() - 1) == QUOTATION_MARK) {
                toCrop = toCrop.substring(0, toCrop.length() - 1);
            }
            return toCrop;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('{');
            boolean addKeySeparator = false;
            for (String key : mData.keySet()) {
                if (addKeySeparator) {
                    builder.append(", ");
                }
                addKeySeparator = true;
                builder.append(key)
                        .append("=[");
                boolean addComma = false;
                for (String value : mData.get(key)) {
                    if (addComma) {
                        builder.append(", ");
                    }
                    addComma = true;
                    builder.append(value);
                }
                builder.append("]");
            }
            builder.append('}');
            return builder.toString();
        }

        public String getEntry(String key, int index, String defaultValue) {
            String[] data = mData.get(key);
            if (data == null || index < 0 || index >= data.length) {
                return defaultValue;
            }
            return data[index];
        }
    }
}
