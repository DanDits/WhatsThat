package dan.dit.whatsthat.system.store;

import android.view.LayoutInflater;
import android.view.View;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 02.09.15.
 */
public class BundleCreator {

    private final View mView;

    public BundleCreator(LayoutInflater inflater) {
        mView = inflater.inflate(R.layout.workshop_bundle_creator, null);
    }

    //TODO for workshop:
    // STEP 0: requires origin name (saved as global preference) and bundle name
    // STEP 1: select image files in gallery
    // STEP 2: for each image:
    //              convert to png if necessary, scale to realistic dimension if over 1000/1000 keeping ratio
    //              set solution (multiple languages, at least in English) with potential multiple words
    //              set image author information (name, source, image title, license, extras/changes)
    //              calculate hash, prefs,...async (best on the fly for each image not at end)
    //              set relative paths, zip images and bundle_name.xml together, save as file name that gets auto recognized by app
    public View getView() {
        return mView;
    }
}
