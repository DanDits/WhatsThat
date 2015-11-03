package dan.dit.whatsthat.util.webPhotoSharing;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import java.util.List;
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
    
    /**
     * If the album that gets created for each user once will be public or not.
     */
    public static final boolean IS_ALBUM_PUBLIC_DEFAULT = true;

    private static @Nullable HttpURLConnection makeOpenConnection(@Nullable String urlString) {
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

    private static ResponseMap obtainResponse(String requestMethod, String urlString) {
        HttpURLConnection urlConnection = makeOpenConnection(urlString);
        if (urlConnection == null) {
            return null;
        }
        ResponseMap response = null;
        try {
            urlConnection.setRequestMethod(requestMethod);
            urlConnection.setRequestProperty("Accept", ACCEPT_HEADER);

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            response = new ResponseMap(in);

        } catch (IOException ioe) {
            Log.e("DumpYourPhoto", "Error with opened url connection for obtaining response: " +
                    ioe);
        } finally {
            urlConnection.disconnect();
        }
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

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            response = new ResponseMap(in);

        } catch (IOException ioe) {
            Log.e("DumpYourPhoto", "Error with opened url connection for posting/putting data: " +
                    ioe);
        } finally {
            urlConnection.disconnect();
        }
        return response;
    }

    private static ResponseMap postBitmapDataObtainResponse(String urlString,
                                                            File bitmapFile) {
        HttpURLConnection urlConnection = makeOpenConnection(urlString);
        if (urlConnection == null || bitmapFile == null || !bitmapFile.exists()) {
            return null; // no sense in trying
        }
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
        return obtainResponse("DELETE", BASE_URL + "albums/" + albumHash);
    }

    private static ResponseMap getAlbums() {
        return obtainResponse("GET", BASE_URL + "albums" + "?" + API_KEY_PARAMETER);
    }

    /**
     * Creates a new album on the website dumpyourphoto.com. Returns the album's hash
     * to identify it. If creation fails for some reasons, null is returned.
     * @param albumName The non empty album name to use to create the new album. Should identify
     *                  the user.
     * @return Null if creation fails for some reason or the hash of the newly created empty album.
     */
    public static String makeAlbum(String albumName) {
        ResponseMap map = makeAlbumExecute(albumName);
        if (map == null) {
            return null;
        }
        return map.getEntry("hash", 0, null);
    }

    /**
     * Updates the album with the given hash to the new name and new public state.
     * @param albumHash The album to update. The hash that got returned when creating the album.
     *                  Must be non empty to actually update the album.
     * @param newAlbumName The new album's name. Must be non empty to actually update the album.
     * @param newIsPublic If the album should become public or private.
     * @return The album's hash if everything went fine, else null.
     */
    public static String updateAlbum(String albumHash, String newAlbumName, boolean newIsPublic) {
        ResponseMap map = updateAlbumExecute(albumHash, newAlbumName, newIsPublic);
        if (map == null) {
            return null;
        }
        return map.getEntry("hash", 0, null);
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

    /**
     * Uploadds the given bitmap file to the given album.
     * @param albumHash The album's hash. Album needs to be already created, must be non empty.
     * @param bitmapFile The valid file to an image. Can be any valid supported image format.
     * @return The hash of the uploaded photo inside the given album together with the photo's
     * server filename. This information is required to downloading the photo from the server.
     * Format: photohash/photofilename. Can be null if something goes wrong. Photohash or
     * photofilename can be empty if some very unexpected server error happens.
     */
    public static @Nullable String uploadPhotoToAlbum(String albumHash, File bitmapFile) {
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

    /**
     * Recreates the download link from the given photoUploadLink that was returned when the
     * photo was successfully uploaded to the server.
     * @param photoUploadLink The upload link of the photo, returned by uploadPhotoToAlbum().
     * @return The download link to use to retrieve the uploaded image.
     */
    private static @NonNull String makeDownloadLinkExecute(@NonNull String photoUploadLink) {
        return BASE_DOWNLOAD_URL + (photoUploadLink.startsWith("/") ? photoUploadLink
                .substring(1) : photoUploadLink);
    }

    public static @Nullable URL makeShareLink(@NonNull String photoLink) {
        try {
            return new URL(makeDownloadLinkExecute(photoLink));
        } catch (MalformedURLException e) {
            Log.e("HomeStuff", "Error creating share link from " + photoLink + " : " + e);
            return null;
        }
    }

    public static @Nullable URL makeDownloadLink(@NonNull Uri shared) {
        List<String> segments = shared.getPathSegments();
        if (segments != null && segments.size() > 1) {
            try {
                return new URL(DumpYourPhotoController.makeDownloadLinkExecute(segments.get(segments.size() - 2)
                        + "/"
                        + segments.get(segments.size() - 1)));
            } catch (MalformedURLException e) {
                Log.e("HomeStuff", "Illegal url for making download link of shared: " + shared +
                        " to " + e);
                return null;
            }
        }
        return null;
    }

    /**
     * Helper class for parsing and storing the server's response to a request on a http connection.
     */
    private static class ResponseMap {
        private Map<String, String[]> mData;

        /**
         * Creates a new ResponseMap reading the given response stream and closing it afterwards.
         * @param response The response to read from the server.
         * @throws IOException If some error happens while reading the response.
         */
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

        /**
         * Returns the entry for the given key at the given index. Index starts with 0 for the
         * first entry.
         * @param key The entry's key.
         * @param index The entry's index.
         * @param defaultValue The default value if the index is out of bounds or no data is
         *                     available for the given key.
         * @return The requested mapped data or the given defaultValue if not mapped or nothing
         * mapped for given index.
         */
        public String getEntry(String key, int index, String defaultValue) {
            String[] data = mData.get(key);
            if (data == null || index < 0 || index >= data.length) {
                return defaultValue;
            }
            return data[index];
        }
    }
}
