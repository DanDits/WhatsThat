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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.preferences.Language;
import dan.dit.whatsthat.preferences.Tongue;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.util.BuildException;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.Dimension;
import dan.dit.whatsthat.util.image.ExternalStorage;
import dan.dit.whatsthat.util.image.FixedRandom;
import dan.dit.whatsthat.util.image.ImageUtil;

public class ImageObfuscator {
    public static final int IS_OBFUSCATED_HINT = 0x00000001; // not stored in pixel, do not start with FF
	private static final double BRIGHTNESS_THRESHOLD = 0.5; // threshold when a pixel is considered to be bright
    private static final int VERSION_1 = 0xFF000001;
	private static final int VERSION_NUMBER = VERSION_1; // Version number the hidden image was created with, stored in pixel, start with FF!!
	private static final int HIDDEN_IMAGE_IDENTIFIER_ID = 0xFFFDCDAD; // random identifier to tell if this (probably) was a valid hidden image, stored in pixel, start with FF!!
    private static final int RESULT_REGISTRATION_FAILED_INVALID_ID = -1;
    private static final int RESULT_REGISTRATION_FAILED_NOT_RESTORABLE = -2;
    private static final int RESULT_REGISTRATION_FAILED_COULDNT_SAVE_IMAGE_1 = -3;
    private static final int RESULT_REGISTRATION_FAILED_COULDNT_SAVE_IMAGE_2 = -4;
    private static final int RESULT_REGISTRATION_FAILED_COULDNT_SAVE_IMAGE_3 = -5;
    private static final int RESULT_REGISTRATION_FAILED_IMAGE_BUILDING = -6;
    private static final int RESULT_REGISTRATION_FAILED = -7;
    private static final int RESULT_REGISTRATION_FAILED_COULDNT_SAVE_IMAGE = -8;
    public static final int RESULT_REGISTRATION_SUCCESS_NO_RIDDLE = 0;
    public static final int RESULT_REGISTRATION_SUCCESS_WITH_RIDDLE = 1;
    public static final String FILE_EXTENSION = ".wte.png"; // needs also to be changed in manifest, but better never change


    private static void addMetadataToImage(PracticalRiddleType preferredType, int[][] raster) {
		// corner pixels are unused and can be used to store some data, use full alpha so there is no bit
        // of information lost for the color bits!
		
		//Version number
		raster[0][0] = VERSION_NUMBER;
		
		//id to identify it 'really' is a hidden image created by this program
		raster[raster.length-1][raster[0].length-1] = HIDDEN_IMAGE_IDENTIFIER_ID;

        // if available, the preferred type to play this image with
        if (preferredType != null) {
            raster[0][raster[0].length - 1] = 0xFF000000 | preferredType.getId();
        } else {
            raster[0][raster[0].length - 1] = 0xFF000000 | PracticalRiddleType.NO_ID;
        }
	}

    public static String toHex(String arg) {
        return String.format("%040x", new BigInteger(1, arg.getBytes()));
    }

    public static String convertHexToString(String hex) {
        return new String(new BigInteger(hex, 16).toByteArray());
    }

    private static String getHexFromRaster(int[][] raster, int startRow, int lastRow, int startColumn, int lastColumn) {
        String result = ""; // cannot use a string builder as we need to add letters before current string
        final int count = (lastRow - startRow + 1) * (lastColumn - startColumn + 1);
        int row = startRow;
        int column = startColumn;
        for (int i = 0; i < count; i++) {
            int number = raster[row][column];
            String asHex = Integer.toHexString(number & 0xFFFFFF);
            for (int j = asHex.length(); j < 3; j++) {
                asHex = "0" + asHex; // leading zeros might have been cut, so add them before
            }
            result = asHex + result;
            if (column < lastColumn) {
                column++;
            } else {
                column = startColumn;
                row++;
            }
        }
        return result;
    }

    private static boolean addHexToRaster(String hex, int[][] raster, int startRow, int lastRow, int startColumn, int lastColumn) {
        int currIndex = hex.length();

        final int count = (lastRow - startRow + 1) * (lastColumn - startColumn + 1);
        int row = startRow;
        int column = startColumn;
        for (int i = 0; i < count; i++) {
            int currNumber = 0xFF000000;
            raster[row][column] = currNumber; // clear pixel
            if (currIndex > 0) {
                int endIndex = currIndex;
                currIndex = Math.max(endIndex - 3, 0);
                // at most 24 bits that fit into the RGB part of the raster pixels
                currNumber |= Integer.parseInt(hex.substring(currIndex, endIndex), 16);
                raster[row][column] = currNumber;
                if (column < lastColumn) {
                    column++;
                } else {
                    column = startColumn;
                    row++;
                }
            }
        }
        if (currIndex > 0) {
            return false; // hash longer than image raster, we cannot fit the hash into the pixels
        }
        return true;
    }

    private static boolean addImagedataToImage(Image image, int[][] raster) {

        // 1. HASH:
        String hash = image.getHash(); // very important info as this identifies the image (if already known to recipient)
        if (hash == null) {
            return false; // illegal image
        }
        Log.d("Image", "Adding imagedata for image with hash " + hash + " and raster size " + raster.length + " on " + raster[0].length);
        if (!addHexToRaster(hash, raster, 1, raster.length - 2, 0, 0)) {
            Log.d("Image", "Failed adding hash to raster: " + hash);
            return false;
        }

        // 2. SOLUTION WORD IN USER LANGUAGE (or any available language):
        Tongue tongue = Language.getInstance().getTongue();
        Solution solution = image.getSolution(tongue);
        Compacter wordsCmp = new Compacter();
        wordsCmp.appendData(tongue.getShortcut());
        for (String word : solution.getWords()) {
            wordsCmp.appendData(word);
        }
        String words = wordsCmp.compact();
        String wordsHex = toHex(words);
        if (!addHexToRaster(wordsHex, raster, 1, raster.length - 2, raster[0].length - 1, raster[0].length - 1)) {
            Log.d("Image", "Failed adding wordsHex to raster: " + wordsHex);
            return false;
        }

        // 3. IMAGE AUTHOR (at least the name, maybe source)
        ImageAuthor author = image.getAuthor();
        String authorHex = toHex(author.compact());
        if (!addHexToRaster(authorHex, raster, 0, 0, 1, raster[0].length - 2)) {
            Log.d("Image", "Failed adding authorHex to raster: " + authorHex);
            // but we do not fail in this case
        }

        // 4. IMAGE ORIGIN
        String originHex = toHex(image.getOrigin());
        if (!addHexToRaster(originHex, raster, raster.length - 1, raster.length - 1, 1, raster[0].length - 2)) {
            Log.d("Image", "Failed adding originHex to raster: " + originHex);
            // but we do not fail in this case
        }

        return true;
    }

	private static int extractMetadataFromImagePreferredRiddle(int[][] raster) {
        return raster[0][raster[0].length - 1] & 0xFFFFFF;
    }

	private static int extractMetadataFromImageVersionNumber(int[][] raster) {
		return raster[0][0];
	}
	
	private static int extractMetadataFromImageIdentifierId(int[][] raster) {
		return raster[raster.length-1][raster[0].length-1]; // fixed indices
	}

    public static int registerObfuscated(Context context, Bitmap obfuscated, String obfuscatedFileName) {
        if (context == null || obfuscated == null) {
            return RESULT_REGISTRATION_FAILED;
        }
        int[][] raster = new int[obfuscated.getHeight()][obfuscated.getWidth()];
        for (int x = 0; x < obfuscated.getWidth(); x++) {
            for (int y = 0; y < obfuscated.getHeight(); y++) {
                raster[y][x] = obfuscated.getPixel(x, y);
            }
        }
        int id = extractMetadataFromImageIdentifierId(raster);
        if (id != HIDDEN_IMAGE_IDENTIFIER_ID) {
            return RESULT_REGISTRATION_FAILED_INVALID_ID;
        }
        if (TextUtils.isEmpty(obfuscatedFileName)) {
            obfuscatedFileName = "-_" + System.currentTimeMillis();
        }
        int fileNameSepIndex = obfuscatedFileName.indexOf('_');
        String riddleOrigin;
        if (fileNameSepIndex != -1) {
            riddleOrigin = obfuscatedFileName.substring(0, fileNameSepIndex);
        } else {
            riddleOrigin = "-";
        }
        String imageOrigin = convertHexToString(getHexFromRaster(raster, raster.length - 1, raster.length - 1, 1, raster[0].length - 2));

        int preferredRiddleId = extractMetadataFromImagePreferredRiddle(raster);
        PracticalRiddleType preferredType = null;
        for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
            if (type.getId() == preferredRiddleId) {
                preferredType = type;
            }
        }
        String path = ExternalStorage.getExternalStoragePathIfMounted(Image.IMAGES_DIRECTORY_NAME);
        if (path == null) {
            return RESULT_REGISTRATION_FAILED_COULDNT_SAVE_IMAGE_1;
        }
        File file = new File(path + File.separator + imageOrigin);
        if (!file.isDirectory() && !file.mkdirs()) {
            Log.e("Image", "Not a directory and couldnt make it to one: " + file);
            return RESULT_REGISTRATION_FAILED_COULDNT_SAVE_IMAGE_2;
        }
        file = new File(file, obfuscatedFileName);
        if (!ImageUtil.saveToFile(obfuscated, file, Bitmap.CompressFormat.PNG, 100)) {
            Log.e("Image", "Could not save to file: " + file + " obfuscated: " + obfuscated);
            return RESULT_REGISTRATION_FAILED_COULDNT_SAVE_IMAGE_3;
        }

        Bitmap restored = restoreImage(obfuscated);
        if (restored == null) {
            return RESULT_REGISTRATION_FAILED_NOT_RESTORABLE;
        }
        Image.Builder builder = new Image.Builder();
        builder.calculateHashAndPreferences(new BitmapUtil.ByteBufferHolder(), restored);
        if (preferredType != null) {
            builder.addPreferredRiddleType(preferredType);
        }
        try {
            Compacter cmp = new Compacter(convertHexToString(getHexFromRaster(raster, 0, 0, 1, raster[0].length - 2)));
            for (int i = cmp.getSize(); i < 5; i++) {
                cmp.appendData("-");
            }
            builder.setAuthor(new ImageAuthor(cmp));
        } catch (CompactedDataCorruptException e) {
            Log.e("Image", "Compacted author data from raster corrupt: " + e);
        }

        Compacter cmp = new Compacter(convertHexToString(getHexFromRaster(raster, 1, raster.length - 2, raster[0].length - 1, raster[0].length - 1)));
        if (cmp.getSize() == 0) {
            cmp.appendData(Tongue.ENGLISH.getShortcut());
        }
        if (cmp.getSize() == 1) {
            cmp.appendData("?");
        }
        Tongue tongue = Tongue.getByShortcut(cmp.getData(0));
        List<String> words = new ArrayList<>(cmp.getSize() - 1);
        for (int i = 1; i < cmp.getSize(); i++) {
            words.add(cmp.getData(i));
        }
        builder.addSolution(new Solution(tongue, words));

        int obfuscationVersionId = extractMetadataFromImageVersionNumber(raster);
        builder.setObfuscation(obfuscationVersionId);
        builder.setRelativeImagePath(obfuscatedFileName);
        builder.setOrigin(imageOrigin);

        Image image;
        try {
            image = builder.build();
        } catch (BuildException be) {
            Log.e("Image", "Failed registering obfuscated image when building: " + be);
            return RESULT_REGISTRATION_FAILED_IMAGE_BUILDING;
        }
        Log.d("Image", "Created image: " + image + " origin " + image.getOrigin() + " obfuscation " + image.getObfuscation()
            + " solutions " + image.getSolutions() + " author " + image.getAuthor());
        if (!image.saveToDatabase(context)) {
            return RESULT_REGISTRATION_FAILED_COULDNT_SAVE_IMAGE;
        }
        if (preferredType != null && riddleOrigin != null && !riddleOrigin.equalsIgnoreCase(Image.ORIGIN_IS_THE_APP)) {
            // create an unfinished riddle if there is a valid origin given which is not the app itself
            RiddleInitializer.INSTANCE.checkedIdsInit(context);
            Log.d("Image", "Creating riddle for loaded obfuscated image: " + image + " type: " + preferredType + " origin: " + riddleOrigin);
            Riddle riddle = new Riddle(image.getHash(), preferredType, riddleOrigin);
            if (riddle.saveToDatabase(context)) {
                Riddle.saveLastVisibleRiddleId(context, riddle.getId());
                Riddle.saveLastVisibleRiddleType(context, preferredType);
                if (!RiddleInitializer.INSTANCE.isNotInitialized()) {
                    RiddleInitializer.INSTANCE.getRiddleManager().onUnsolvedRiddle(riddle);
                }
                return RESULT_REGISTRATION_SUCCESS_WITH_RIDDLE;
            }
        }
        return RESULT_REGISTRATION_SUCCESS_NO_RIDDLE;
    }

    /**
     * Creates an obfuscated image from the original bitmap including the given logo.
     * The original image will not be humanly readable, humans will only be able to recognize the
     * logo.
     * Keep it mind that the restored image is only almost equal to the original image and the hash
     * of restored and original will not match.
     * @param image Image to obfuscate.
     * @param logoSource The required logo to include.
     * @return An obfuscated version of the original image with only the logo visible. Can be restored
     * to be almost equal to the original image. Can be null if loading the image's bitmap fails.
     */
	public static Bitmap makeHidden(Resources res, Image image, PracticalRiddleType prefferedType, Logo logoSource) {
		Bitmap original = image.loadBitmap(res, new Dimension(0, 0), true);
        if (original == null) {
            return null;
        }
        Log.d("Image", "Making hidden: " + image + " origin " + image.getOrigin() + " obfuscation " + image.getObfuscation()
                + " solutions " + image.getSolutions() + " author " + image.getAuthor());

        Bitmap logo = logoSource.getSized(original.getWidth(), original.getHeight());

		int hiddenHeight = original.getHeight() + 2;
        int hiddenWidth = original.getWidth() + 2;
		//extract image data for easier working
		int[][] raster = new int[hiddenHeight][hiddenWidth];
		for (int x=0;x<original.getWidth();x++) {
			for (int y=0;y<original.getHeight();y++) {
				raster[y + 1][x + 1]=original.getPixel(x, y);
			}
		}
		
		// do the transformation, we loose very little information and do not need to save a bigger picture for it
		// the extra lines are for the purpose of storing identifying and metadata if needed
		
		// Step1: Fixed permutation, one transposition for each pixel of the original image
		FixedRandom random = new FixedRandom();
		for (int y=1;y<raster.length - 1;y++) {
			for (int x=1;x<raster[y].length - 1;x++) {
				int currRGB = raster[y][x];
				int tarX=random.next(raster[y].length - 3) + 1; // x in [1,image width - 1]
				int tarY=random.next(raster.length - 3) + 1; // y in [1, image height - 1]
				raster[y][x]=raster[tarY][tarX];
				raster[tarY][tarX]=currRGB;
			}
		}	
		
		//Step2: Make all pixels not in the logo darker and the logo pixel brighter
		for (int y=1;y<raster.length - 1;y++) {
			for (int x=1;x<raster[y].length - 1;x++) {
				int rgb = raster[y][x];
				int logoRgb=logo.getPixel(x - 1, y - 1);
				int red = Color.red(rgb);
				int green = Color.green(rgb);
				int blue = Color.blue(rgb);
				int alpha = Color.alpha(rgb);
				
				//Make some assumptions to improve and allow us to manipulate the image without needing much extra memory.
				// We will lose the 4 least significant bits for alpha, we cannot use other colors since Bitmaps store these values
                // in premultiplied alpha format, which will make getPixel(setPixel(rgb)) != rgb because of rounding errors.
                // Additional bits can be sacrificed to get the alpha even higher or to adjust the greyness of the pixels, but this results
                // in restored obfuscated images with lots of different alpha to become significantly different from their original image.
				alpha = (alpha/8)*8+7; // assume alpha mod 8 == 7 (to leave 255 (no alpha) the same)!!

                 //increase alpha as much as possible with mostly having a delta of 4 to the original image and leaving alpha=255 as is
                // stores information in bits 1 and 2 of alpha
                int alphaFactor = 3-alpha/64; // 0, 1, 2 or 3
                alpha += alphaFactor * 64 - alphaFactor;
				
				/*//Move the color by the line 64/128/256 (arbitary multiples of 2) and maximize the greyness to get a better contrast
				// for images with pixels that have strong colors and little grey; stores information in bit4 of alpha
				int maxGreynessIndex = 0;
				double maxGreyness = Double.MAX_VALUE;
				for (int i=0; i < 2; i++) {
					double greyness = ColorAnalysisUtil.getGreyness(red + 64*i, green + 128*i, blue);
					if (greyness < maxGreyness) {
						maxGreynessIndex = i;
						maxGreyness = greyness;
					}
				}
				red+=(64*maxGreynessIndex)%256;red%=256;
				green+=(128*maxGreynessIndex)%256;green%=256;
				//blue+=(256*maxGreynessIndex)%256;blue%=256;
				alpha-= 8*maxGreynessIndex;
                */

                // make the logo by changing pixel brightness accordingly, storing this information in bit3 of alpha
				raster[y][x]=rgb=ColorAnalysisUtil.toRGB(red, green, blue, alpha);
				boolean pixelVeryBright = ColorAnalysisUtil.getBrightnessNoAlpha(rgb) > BRIGHTNESS_THRESHOLD;
				boolean insideLogo = ColorAnalysisUtil.getBrightnessWithAlpha(logoRgb) <= logoSource.getThreshold();
				if ((!pixelVeryBright && insideLogo)
						|| (pixelVeryBright && !insideLogo)) {
					// It is a pixel of the logo and it currently is too dark or
					// it is a pixel not in the logo and it is too bright
					red = 255 - red;
					green = 255 - green;
					blue = 255 - blue;
                    alpha -= 4;
				}

				raster[y][x] = ColorAnalysisUtil.toRGB(red, green, blue, alpha);
			}
		}
		
		// end transformation
		
		// include the metadata
		addMetadataToImage(prefferedType, raster);
        if (!addImagedataToImage(image, raster)) {
            return null;
        }
        Bitmap hidden = Bitmap.createBitmap(hiddenWidth, hiddenHeight, original.getConfig());
        hidden.setHasAlpha(true);

		// draw the raster in the new image
		for (int y=0;y<raster.length;y++) {
			for (int x=0;x<raster[y].length;x++) {
				hidden.setPixel(x, y, raster[y][x]);
			}
		}

		return hidden;
	}
	

	public static Bitmap restoreImage(Bitmap hidden) {
		if (hidden.getHeight() < 3 || hidden.getWidth() < 3) {
			return null; // not a valid hidden image
		}
        // must not use the logo! Then we can restore images from unknown logo sources and have the logo customizable

		//extract image data for easier working
		int[][] raster = new int[hidden.getHeight()][hidden.getWidth()];
		for (int x=0;x<hidden.getWidth();x++) {
			for (int y=0;y<hidden.getHeight();y++) {
				raster[y][x]=hidden.getPixel(x, y);
			}
		}

		if (extractMetadataFromImageIdentifierId(raster) != HIDDEN_IMAGE_IDENTIFIER_ID) {
			return null;
		}

        // Choose restoration algorithm by version number.
        if (extractMetadataFromImageVersionNumber(raster) != VERSION_1) {
            return null; // cannot restore that version
        }
		// Revert transformation
		
		// Revert Step2:
		//(Step2: Make all pixels not in the logo darker and the logo pixel brighter)

		for (int y=1;y<raster.length - 1;y++) {
			for (int x=1;x<raster[y].length - 1;x++) {
				int rgb = raster[y][x];
				int red = Color.red(rgb);
				int green = Color.green(rgb);
				int blue = Color.blue(rgb);
				int alpha = Color.alpha(rgb);
				
				if ((alpha&4)==0) {
					red = 255 - red;
					green = 255 - green;
					blue = 255 - blue;
                    alpha += 4;
                }
				
				/*int greynessIndex = 1 - (alpha&8)/8;
				red+=((256-64)*greynessIndex)%256;red%=256;
				green+=((256-128)*greynessIndex)%256;green%=256;
				//blue+=((256-256)*greynessIndex)%256;blue%=256;
                */

                int alphaFactor = 3 - alpha % 4;
                alpha -= alphaFactor * 64 - alphaFactor;

				raster[y][x]=ColorAnalysisUtil.toRGB(red, green, blue, alpha);
				
			}
		}
		
		//Revert Step1:
		// (Step1: Fixed permutation, one transposition for each pixel of the original image)
		FixedRandom random = new FixedRandom();
		int permutations = (raster.length - 2) * (raster[0].length - 2);
		int[] tarX = new int[permutations];
		int[] tarY = new int[permutations];
		for (int i = 0; i < permutations; i++) {
			tarX[i] = random.next(raster[0].length - 3) + 1; // x in [1,image width - 1]
			tarY[i] = random.next(raster.length - 3) + 1; // y in [1, image height - 1]
		}
		for (int i = permutations - 1; i >= 0; i--) {
			int x = i % (raster[0].length - 2) + 1;
			int y = i / (raster[0].length - 2) + 1;
			int currRGB = raster[y][x];
			int tarXCurr=tarX[i];
			int tarYCurr=tarY[i];
			raster[y][x]=raster[tarYCurr][tarXCurr];
			raster[tarYCurr][tarXCurr]=currRGB;
		}
		
		// End Revert transformation

        Bitmap original = Bitmap.createBitmap(hidden.getWidth() - 2, hidden.getHeight() - 2, hidden.getConfig());
        original.setHasAlpha(true);
		// draw the raster in the new image
		for (int y=1;y<raster.length - 1;y++) {
			for (int x=1;x<raster[y].length - 1;x++) {
				original.setPixel(x - 1, y - 1, raster[y][x]);
			}
		}

		return original;
	}

    public static boolean checkIfValidObfuscatedImage(Bitmap image) {
        if (image == null) {
            return false;
        }
        return image.getPixel(image.getWidth() - 1, image.getHeight() - 1) == HIDDEN_IMAGE_IDENTIFIER_ID;
    }
}