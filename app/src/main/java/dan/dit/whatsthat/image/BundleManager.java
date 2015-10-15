/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dan.dit.whatsthat.image;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.preferences.User;
import dan.dit.whatsthat.system.ImageDataDownload;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.sortiment.ShopArticleDownload;
import dan.dit.whatsthat.util.BuildException;
import dan.dit.whatsthat.util.image.ExternalStorage;

/**
 * Created by daniel on 09.09.15.
 */
public class BundleManager {
    public static final String BUNDLES_DIRECTORY_NAME = ".bundles";
    public static final String BUNDLE_EXTENSION = ".wtb";
    private static final String BUNDLE_PREFERENCES = "dan.dit.whatsthat.bundle_preferences";
    private static final String PREFERENCE_ALL_ORIGIN_AND_NAMES_KEY = "dan.dit.whatsthat.origin_and_names";

    private final Activity mActivity;
    private final BundlesAdapter mAdapter;
    private static File sBundlesDir;
    private ExpandableListView mView;
    private SortedSet<ImageBundle> mBundles;

    public BundleManager(Activity activity) {
        mActivity = activity;
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = (ExpandableListView) inflater.inflate(R.layout.workshop_bundle_manager, null);
        sBundlesDir = ensureBundleDirectory();
        loadBundles();
        mAdapter = new BundlesAdapter(activity);
        mView.setAdapter(mAdapter);
    }

    public void refresh() {
        loadBundles();
        mAdapter.notifyDataSetChanged();
    }

    public static File ensureBundleDirectory() {
        if (sBundlesDir != null) {
            return sBundlesDir;
        }
        boolean exists = false;
        File bundlesDir = null;
        String path = ExternalStorage.getExternalStoragePathIfMounted(BUNDLES_DIRECTORY_NAME);
        if (path != null) {
            bundlesDir = new File(path);
            if (bundlesDir.mkdirs()  || bundlesDir.isDirectory()) {
                exists = true;
                sBundlesDir = bundlesDir;
            }
        }
        return exists ? bundlesDir : null;
    }

    public static File makeBundleFile(File dir, String bundleOrigin, String bundleName) {
        return new File(dir, bundleOrigin + "_" + bundleName + BundleManager.BUNDLE_EXTENSION);
    }

    public static String[] getOriginAndNameOfBundleFile(File file) {
        if (file == null) {
            return null;
        }
        String name = file.getName();
        if (!name.endsWith(BUNDLE_EXTENSION)) {
            return null;
        }
        return name.substring(0, name.length() - BUNDLE_EXTENSION.length()).split("_");
    }

    public View getView() {
        return mView;
    }

    public static void onBundleCreated(Context context, String origin, String bundleName, int count, int sizeMB, boolean synced, String downloadArticleKey) {
        ImageBundle created = null;
        try {
            created = new ImageBundle(ensureBundleDirectory(), origin, bundleName, count, sizeMB, synced, downloadArticleKey);
        } catch(BuildException e) {
            Log.e("Image", "Failed building image bundle onBundleCreated: " + e);
        }
        if (created != null) {
            created.saveBundle(context);
        }
    }

    private void loadBundles() {
        ensureBundleDirectory();
        if (sBundlesDir == null) {
            return;
        }
        mBundles = new TreeSet<>();
        SharedPreferences prefs = mActivity.getSharedPreferences(BUNDLE_PREFERENCES, Context.MODE_PRIVATE);
        Set<String> originAndNames = prefs.getStringSet(PREFERENCE_ALL_ORIGIN_AND_NAMES_KEY, new HashSet<String>());
        for (String originAndName : originAndNames) {
            String[] split = originAndName.split("_");
            if (split.length >= 2) {
                ImageBundle curr = null;
                try {
                    curr = new ImageBundle(sBundlesDir, split[0], split[1],
                            prefs.getInt(originAndName + "_count", -1),
                            prefs.getInt(originAndName + "_sizeMB", -1),
                            prefs.getBoolean(originAndName + "_synced", false),
                            prefs.getString(originAndName + "_download", null));
                } catch (BuildException e) {
                    Log.e("Image", "Error building image bundle in manager when loading bundles." + e);
                }
                if (curr != null) {
                    mBundles.add(curr);
                }
            }
        }
        // next check files in bundles directory, maybe some bundle appeared there
        for (File file : sBundlesDir.listFiles()) {
            if (file.getName().endsWith(BUNDLE_EXTENSION) && !file.isDirectory()) {
                ImageBundle curr = null;
                try {
                    curr = new ImageBundle(file);
                } catch (BuildException be) {
                    Log.e("Image", "Error building image bundle from file " + file + ": " + be);
                }
                if (curr != null) {
                    mBundles.add(curr);
                }
            }
        }
    }

    private static class ImageBundle implements Comparable<ImageBundle> {
        private final String mOrigin;
        private final String mName;
        private final File mPath;
        private String mDownloadArticleKey;
        private int mEstimatedSizeMB;
        private int mEstimatedImagesCount;
        private boolean mSynced;

        public ImageBundle(File bundlesDir, String origin, String name, int count, int sizeMB, boolean synced, String downloadArticleKey) throws BuildException {
            mOrigin = origin;
            mName = name;
            mEstimatedImagesCount = count;
            mEstimatedSizeMB = sizeMB;
            mPath = makeBundleFile(bundlesDir, origin, name);
            mDownloadArticleKey = downloadArticleKey;
            mSynced = synced;
            checkOriginAndName();
        }

        public ImageBundle setSynced(Context context) {
            if (!mSynced) {
                mSynced = true;
                saveBundle(context);
            }
            return this;
        }

        public ImageBundle(File file) throws BuildException {
            mPath = file;
            String[] originAndName = getOriginAndNameOfBundleFile(mPath);
            if (originAndName == null || originAndName.length < 2) {
                throw new BuildException("No valid bundle file given.");
            }
            mOrigin = originAndName[0];
            mName = originAndName[1];
            mEstimatedImagesCount = -1;
            mEstimatedSizeMB = -1;
            checkOriginAndName();
        }

        public void saveBundle(Context context) {
            SharedPreferences prefs = context.getSharedPreferences(BUNDLE_PREFERENCES, Context.MODE_PRIVATE);
            String key = mOrigin + "_" + mName;
            Set<String> allOriginNames = prefs.getStringSet(PREFERENCE_ALL_ORIGIN_AND_NAMES_KEY, new HashSet<String>());
            allOriginNames.add(key);
            SharedPreferences.Editor editor = prefs.edit().putInt(key + "_count", mEstimatedImagesCount)
                    .putInt(key + "_sizeMB", mEstimatedSizeMB)
                    .putBoolean(key + "_synced", mSynced)
                    .putStringSet(PREFERENCE_ALL_ORIGIN_AND_NAMES_KEY, allOriginNames);
            if (!TextUtils.isEmpty(mDownloadArticleKey)) {
                editor.putString(key + "_download", mDownloadArticleKey);
            } else {
                editor.remove(key + "_download");
            }
            editor.apply();
        }

        public void removeBundle(Context context) {
            SharedPreferences prefs = context.getSharedPreferences(BUNDLE_PREFERENCES, Context.MODE_PRIVATE);
            String key = mOrigin + "_" + mName;
            Set<String> allOriginNames = prefs.getStringSet(PREFERENCE_ALL_ORIGIN_AND_NAMES_KEY, new HashSet<String>());
            allOriginNames.remove(key);

            prefs.edit().remove(key + "_count")
                    .remove(key + "_sizeMB")
                    .remove(key + "_synced")
                    .putStringSet(PREFERENCE_ALL_ORIGIN_AND_NAMES_KEY, allOriginNames)
                    .remove(key + "_download").apply();
        }

        private void checkOriginAndName() throws BuildException {
            if (TextUtils.isEmpty(mOrigin)) {
                throw new BuildException().setMissingData("ImageBundle", "Origin");
            }
            if (TextUtils.isEmpty(mName)) {
                throw new BuildException().setMissingData("ImageBundle", "Name");
            }
        }

        @Override
        public boolean equals(Object other) {
            return other == this || (other instanceof ImageBundle
                    && mName.equals(((ImageBundle) other).mName) && mOrigin.equals(((ImageBundle) other).mOrigin));
        }

        @Override
        public int hashCode() {
            return mName.hashCode() + 32 * mOrigin.hashCode();
        }

        @Override
        public int compareTo(@NonNull ImageBundle another) {
            int originDiff = mOrigin.compareToIgnoreCase(another.mOrigin);
            if (originDiff == 0) {
                return mName.compareToIgnoreCase(another.mName);
            }
            return originDiff;
        }
    }

    private class BundlesAdapter extends BaseExpandableListAdapter {
        private final LayoutInflater mInflater;
        private Context mContext;

        public BundlesAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        }

        @Override
        public int getGroupCount() {
            int count = 0;
            String lastOrigin = null;
            for (ImageBundle bundle : mBundles) {
                if (lastOrigin == null || !lastOrigin.equalsIgnoreCase(bundle.mOrigin)) {
                    lastOrigin = bundle.mOrigin;
                    count++;
                }
            }
            return count;
        }

        @Override
        public int getChildrenCount(int i) {
            int count = 0;
            int currOriginIndex = -1;
            String lastOrigin = null;
            for (ImageBundle bundle : mBundles) {
                if (lastOrigin == null || !lastOrigin.equalsIgnoreCase(bundle.mOrigin)) {
                    lastOrigin = bundle.mOrigin;
                    currOriginIndex++;
                    if (currOriginIndex > i) {
                        break;
                    }

                }
                if (currOriginIndex == i) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public Object getGroup(int i) {
            SortedSet<ImageBundle> group = new TreeSet<>();
            int currOriginIndex = -1;
            String lastOrigin = null;
            for (ImageBundle bundle : mBundles) {
                if (lastOrigin == null || !lastOrigin.equalsIgnoreCase(bundle.mOrigin)) {
                    lastOrigin = bundle.mOrigin;
                    currOriginIndex++;
                    if (currOriginIndex > i) {
                        break;
                    }

                }
                if (currOriginIndex == i) {
                    group.add(bundle);
                }
            }
            return group;
        }

        @Override
        public Object getChild(int i, int i1) {
            int currOriginIndex = -1;
            String lastOrigin = null;
            int currChildIndex = -1;
            for (ImageBundle bundle : mBundles) {
                if (lastOrigin == null || !lastOrigin.equalsIgnoreCase(bundle.mOrigin)) {
                    lastOrigin = bundle.mOrigin;
                    currOriginIndex++;
                    if (currOriginIndex > i) {
                        break;
                    }

                }
                if (currOriginIndex == i) {
                    currChildIndex++;
                    if (currChildIndex == i1) {
                        return bundle;
                    }
                }
            }
            return null;
        }

        @Override
        public long getGroupId(int i) {
            return i + 1;
        }

        @Override
        public long getChildId(int i, int i1) {
            return i1 + 1;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.workshop_bundle_manager_category, null);
            }
            SortedSet<ImageBundle> group = (SortedSet<ImageBundle>) getGroup(groupPosition);
            int imageCount = 0;
            int sizeMB = 0;
            for (ImageBundle bundle : group) {
                if (bundle.mEstimatedImagesCount > 0) {
                    imageCount += bundle.mEstimatedImagesCount;
                }
                if (bundle.mEstimatedSizeMB > 0) {
                    sizeMB += bundle.mEstimatedSizeMB;
                }
            }
            sizeMB = Math.max(1, sizeMB);
            TextView imageCountView = (TextView) convertView.findViewById(R.id.image_count);
            if (imageCount > 0) {
                imageCountView.setText(imageCountView.getResources().getQuantityString(R.plurals.images_in_bundle, imageCount, imageCount));
                imageCountView.setVisibility(View.VISIBLE);
            } else {
                imageCountView.setVisibility(View.INVISIBLE);
            }

            TextView sizeMBView = (TextView) convertView.findViewById(R.id.image_size_mb);
            sizeMBView.setText("~" + String.valueOf(sizeMB) + "MB");

            if (!group.isEmpty()) {
                ((TextView) convertView.findViewById(R.id.bundle_origin)).setText(group.first().mOrigin);
            } else {
                convertView.findViewById(R.id.bundle_origin).setVisibility(View.INVISIBLE); // shouldn't be the case
            }
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.workshop_bundle_manager_bundle, null);
            }
            final ImageBundle bundle = (ImageBundle) getChild(groupPosition, childPosition);
            ((TextView) convertView.findViewById(R.id.bundle_name)).setText(bundle.mName);

            TextView imageCountView = (TextView) convertView.findViewById(R.id.image_count);
            if (bundle.mEstimatedImagesCount > 0) {
                imageCountView.setText(imageCountView.getResources().getQuantityString(R.plurals.images_in_bundle, bundle.mEstimatedImagesCount, bundle.mEstimatedImagesCount));
                imageCountView.setVisibility(View.VISIBLE);
            } else {
                imageCountView.setVisibility(View.INVISIBLE);
            }

            TextView sizeMBView = (TextView) convertView.findViewById(R.id.image_size_mb);
            sizeMBView.setText("~" + String.valueOf(Math.max(1, bundle.mEstimatedSizeMB)) + "MB");

            ImageButton download = (ImageButton) convertView.findViewById(R.id.download);
            if (TextUtils.isEmpty(bundle.mDownloadArticleKey)) {
                download.setVisibility(View.GONE);
            } else {
                final ShopArticleDownload downloadArticle = (ShopArticleDownload) TestSubject.getInstance().getShopSortiment().getArticle(bundle.mDownloadArticleKey);
                if (downloadArticle == null) {
                    download.setVisibility(View.GONE);
                } else {
                    download.setVisibility(View.VISIBLE);
                    download.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            v.setEnabled(false);
                            downloadArticle.start();
                        }
                    });
                }
            }

            final ImageButton syncUnsync = (ImageButton) convertView.findViewById(R.id.sync_unsync);
            if (bundle.mPath != null && bundle.mPath.exists()) {
                // currently unsync option not available, and sync only if permission is granted
                if (User.getInstance().hasPermission(User.PERMISSION_BUNDLE_SYNC_ALLOWED)) {
                    syncUnsync.setVisibility(View.VISIBLE);
                    syncUnsync.setEnabled(true);

                    syncUnsync.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            v.setEnabled(false);
                            ImageDataDownload includer = new ImageDataDownload(mContext, bundle.mOrigin, bundle.mName, bundle.mEstimatedSizeMB, null, new ImageDataDownload.Feedback() {
                                @Override
                                public void setIsWorking(boolean isWorking) {

                                }

                                @Override
                                public void onError(int messageResId, int errorCode) {
                                    Toast.makeText(mContext, mContext.getResources().getString(messageResId, errorCode), Toast.LENGTH_SHORT).show();
                                    syncUnsync.setEnabled(true);
                                }

                                @Override
                                public void onDownloadComplete() {

                                }

                                @Override
                                public void onComplete() {
                                    Toast.makeText(mContext, R.string.download_article_toast_complete, Toast.LENGTH_SHORT).show();
                                    bundle.setSynced(mContext);
                                    mAdapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onProgressUpdate(int progress) {

                                }
                            });
                            if (!bundle.mOrigin.equalsIgnoreCase(Image.ORIGIN_IS_THE_APP)) {
                                includer.setKeepBundleAfterSync();
                            }
                            includer.start();
                        }
                    });
                } else {
                    syncUnsync.setVisibility(View.GONE);
                }
            } else {
                syncUnsync.setVisibility(View.GONE);
            }

            ImageButton share = (ImageButton) convertView.findViewById(R.id.share);
            if (bundle.mPath != null && bundle.mPath.exists()) {
                share.setVisibility(View.VISIBLE);
                share.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("text/plain");
                            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(bundle.mPath));
                            mContext.startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(mContext, R.string.share_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                share.setVisibility(View.GONE);
            }

            ImageButton delete = (ImageButton) convertView.findViewById(R.id.delete);
            if (bundle.mPath != null && bundle.mPath.exists()) {
                delete.setVisibility(View.VISIBLE);
                // currently only deleting of bundle file possible, not deleting images from images directory (and therefore unsyncing from database)
                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (bundle.mPath.delete()) {
                            if (!bundle.mSynced) {
                                bundle.removeBundle(mContext);
                            }
                            refresh();
                        }
                    }
                });
            } else {
                delete.setVisibility(View.GONE);
            }
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int i, int childPosition) {
            return false;
        }
    }
}
