package dan.dit.whatsthat.util;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
			return false;
		}
		ZipInputStream zipinputstream;
		try {
			zipinputstream = new ZipInputStream(new FileInputStream(zipArchive));
		} catch (FileNotFoundException e) {
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
				return false; 
			}
			if (zipentry != null) {
				// entry was successfully opened
				String entryName = zipentry.getName();
				
	            File tempExtracted = new File(target, entryName);
	            if (zipentry.isDirectory()) {
	            	if (!tempExtracted.mkdirs()) {
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
                            return false;
                        }
						fileoutputstream.close();
						zipinputstream.closeEntry();
					} catch (IOException e) {
                        Log.e("Image", "Exception during write of unzip: " + e);
						result = false;
					}
	            }
			}
		} while (zipentry != null);
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
    	LinkedList<File> filesInFolder = new LinkedList<File>();
    	IOUtil.addContainingFiles(folder, filesInFolder);
    	return filesInFolder;
    }
}
