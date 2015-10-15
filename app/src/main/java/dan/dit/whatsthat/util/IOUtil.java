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

package dan.dit.whatsthat.util;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import dan.dit.whatsthat.preferences.User;

/**
 * This is an utility class which offers specialized IO methods and file
 * system support like unzipping a zip archive to a folder or getting all files
 * within a directory.
 * @author Daniel
 *
 */
public final class IOUtil {
	
	private IOUtil() {
	}
	
	/**
     * Unzips the given zip archive to the given target location, keeping the folder structure
     * of the zip archive.
     * @param zipArchive The zip archive to be extracted.
     * @param target The target location the archive should be extracted to.
     * @return <code>true</code> only if every file and directory was successfully extracted and no
     * IOException occured. If this returns <code>false</code> it is possible that several files were
     * extracted successfully.
     */
	public static boolean unzip(File zipArchive, File target) {
		if (zipArchive == null || target == null || !zipArchive.exists() || !target.exists()) {
			Log.e("HomeStuff", "Unzip failed at 0: " + zipArchive + " target " + target);
			return false;
		}
		ZipInputStream zipinputstream;
		try {
			zipinputstream = new ZipInputStream(new FileInputStream(zipArchive));
		} catch (FileNotFoundException e) {
            Log.e("HomeStuff", "Unzip failed at 1: " + e);
			return false;
		}

		ZipEntry zipentry;
		final int bufferSize = 1024;
		byte[] buf = new byte[bufferSize];
		boolean result = true;
		do {
			// for each entry to be extracted
			try {
				zipentry = zipinputstream.getNextEntry();
			} catch (IOException ioe) {
				// critical, especially if next would be a directory which contains files
                Log.e("HomeStuff", "Unzip failed at 2: " + ioe);
				return false; 
			}
			if (zipentry != null) {
				// entry was successfully opened
				String entryName = zipentry.getName();
				
	            File tempExtracted = new File(target, entryName);
	            if (zipentry.isDirectory()) {
	            	if (!tempExtracted.isDirectory() && !tempExtracted.mkdirs()) {
                        Log.e("HomeStuff", "Unzip problem at 3.5.");
                        result = false;
                    }
	            } else {
	            	// not a directory
		            try {
		            	// unzip the current entry to the specified directory
		            	int n;
		            	FileOutputStream fileoutputstream = new FileOutputStream(tempExtracted);             
						while ((n = zipinputstream.read(buf, 0, bufferSize)) > 0) {
						    fileoutputstream.write(buf, 0, n);
						}
                        if (n == 0) {
                            // this can happen when zip archive is corrupt, closeEntry will never return
                            fileoutputstream.close();
                            Log.e("HomeStuff", "Unzip failed at 3: Zip archive corrupt.");
                            return false;
                        }
						fileoutputstream.close();
						zipinputstream.closeEntry();
					} catch (IOException e) {
                        Log.e("HomeStuff", "Unzip problem at 4: " + e);
						result = false;
					}
	            }
			}
		} while (zipentry != null);
        if (!result) {
            Log.e("HomeStuff", "Unzip failed at 5.");
        }
		return result;
	}
	
	/**
	 * Recursivly adds the containing files of the given folder to the given list.
	 * Helper method for getFiles(folder).
	 * @param folder The folder that is searched for files or subdirectories.
	 * @param list The list to add the files to.
	 */
    private static void addContainingFiles(File folder, LinkedList<File> list) {
    	if (folder == null || !folder.isDirectory() || list == null) {
    		return;
    	}
    	
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                // recursivly search for sub files, do not add the directory to the list
                IOUtil.addContainingFiles(file, list);
            } else {
                list.add(file);
            }
        }
    }
    
	/**
	 * Recursivly returns all files that are not a directory within the given directory (so
	 * this includes files in subdirectories). Does nothing if given folder is <code>null</code> or
	 * not a directory.
	 * @param folder The folder the tree search starts.
	 * @return A list containing all files containted in the given directory and all subdirectories.
	 * The list will not contain directories, only files. Returned list will never be <code>null</code>.
	 */
    public static LinkedList<File> getFiles(File folder) {
    	LinkedList<File> filesInFolder = new LinkedList<>();
    	IOUtil.addContainingFiles(folder, filesInFolder);
    	return filesInFolder;
    }

	public static boolean zip(List<File> toZipInsideTempDirectory, File targetZip) throws IOException {
		FileOutputStream os = new FileOutputStream(targetZip);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));
		try {
			for (int i = 0; i < toZipInsideTempDirectory.size(); ++i) {
				File file = toZipInsideTempDirectory.get(i);
				String filename = User.extractRelativePathInsideTempDirectory(file);
                if (filename == null) {
                    filename = file.getName();
                }
				ZipEntry entry = new ZipEntry(filename);
				zos.putNextEntry(entry);
                int n;
                byte[] buf = new byte[1024];
                InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                while ((n = inputStream.read(buf, 0, buf.length)) > 0) {
                    zos.write(buf, 0, n);
                }
                inputStream.close();
				zos.closeEntry();
			}
            return true;
		} finally {
			zos.close();
		}
	}
}
