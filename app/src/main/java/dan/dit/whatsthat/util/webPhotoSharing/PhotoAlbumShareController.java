package dan.dit.whatsthat.util.webPhotoSharing;

import android.net.Uri;

import java.io.File;
import java.net.URL;

/**
 * Created by daniel on 26.11.15.
 */
public abstract class PhotoAlbumShareController {

    public static final boolean IS_ALBUM_PUBLIC_DEFAULT = false;

    public abstract String uploadPhotoToAlbum(String albumHash, File photo);

    public abstract URL makeShareLink(String photoLink);

    public abstract String updateAlbum(String albumHash, String albumName, boolean albumPublic);

    public abstract String makeAlbum(String albumName);

    public abstract URL makeDownloadLink(Uri shared);
}
