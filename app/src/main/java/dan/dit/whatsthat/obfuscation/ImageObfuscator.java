package dan.dit.whatsthat.obfuscation;

import android.graphics.Bitmap;
import android.graphics.Color;

import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.FixedRandom;

public class ImageObfuscator {
    public static final int IS_OBFUSCATED_HINT = 0x00000001; // not stored in pixel, do not start with FF
	private static final double BRIGHTNESS_THRESHOLD = 0.5; // threshold when a pixel is considered to be bright
    private static final int VERSION_1 = 0xFF000001;
	private static final int VERSION_NUMBER = VERSION_1; // Version number the hidden image was created with, stored in pixel, start with FF!!
	private static final int HIDDEN_IMAGE_IDENTIFIER_ID = 0xFFFDCDAD; // random identifier to tell if this (probably) was a valid hidden image, stored in pixel, start with FF!!


	
	private static void addMetadataToImage(int[][] raster) {
		// corner pixels are unused and can be used to store some data, use full alpha so there is no bit
        // of information lost for the color bits!
		
		//Version number
		raster[0][0]=VERSION_NUMBER;
		
		//id to identify it 'really' is a hidden image created by this program
		raster[raster.length-1][raster[0].length-1]=HIDDEN_IMAGE_IDENTIFIER_ID;

	}
	
	private static int extractMetadataFromImageVersionNumber(int[][] raster) {
		return raster[0][0];
	}
	
	private static int extractMetadataFromImageIdentifierId(int[][] raster) {
		return raster[raster.length-1][raster[0].length-1];
	}

    /**
     * Creates an obfuscated image from the original bitmap including the given logo.
     * The original image will not be humanly readable, humans will only be able to recognize the
     * logo.
     * Keep it mind that the restored image is only almost equal to the original image and the hash
     * of restored and original will not match.
     * @param original Image to obfuscate.
     * @param logoSource The required logo to include.
     * @return An obfuscated version of the original image with only the logo visible. Can be restored
     * to be almost equal to the original image.
     */
	public static Bitmap makeHidden(Bitmap original, Logo logoSource) {
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

				raster[y][x] = rgb = ColorAnalysisUtil.toRGB(red, green, blue, alpha);				
			}
		}
		
		// end transformation
		
		// include the metadata
		addMetadataToImage(raster);

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

				raster[y][x]=rgb=ColorAnalysisUtil.toRGB(red, green, blue, alpha);
				
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

}