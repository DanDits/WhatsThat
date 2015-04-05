package dan.dit.whatsthat.util.image;

import android.graphics.Bitmap;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
/**
 * This class is a cache for Bitmaps, allowing the storage
 * of multiple images referenced by the same KeyType. As this cache
 * uses {@link java.lang.ref.SoftReference}s for storage, the given image is not
 * guaranteed to be available. So even miliseconds after adding
 * an image, it could be recycled by the garbage collector. This
 * class therefore helps avoiding an OutOfMemoryError and still
 * caching lots of images of different sizes.<br>
 * All methods do nothing if a parameter is <code>null</code>.
 * If a method returns a Bitmap or list of Bitmaps,
 * this will create new strong references! Therefore these images
 * will not be deleted as long as the references are not nulled and
 * the images will stay in cache! Free the strong references after usage
 * to let the cache work as intented afterwards.
 * @author Daniel
 *
 */
public class ImageMultiCache<KeyType> {
	private ReferenceQueue<Bitmap> queue;

    public static final ImageMultiCache<String> INSTANCE = new ImageMultiCache<String>(10);
	private HashMap<KeyType, LinkedList<SoftListValue<KeyType>>> cache;
	
	/**
	 * Creates a new empty cache.
	 * @param initialCapacity The initial capacity for the cache, handed over to the
	 * internal HashMap, check its policy for adjusting this value.
	 */
	private ImageMultiCache(int initialCapacity) {
		this.cache = new HashMap<KeyType, LinkedList<SoftListValue<KeyType>>>(initialCapacity);
		this.queue = new ReferenceQueue<Bitmap>();
	}
	
	/**
	 * Adds the given image references by the given image KeyType to the cache.
	 * The image will be added to the list of images for the KeyType.
	 * @param imageKeyType The image KeyType which is the key to the image.
	 * @param image The image to be stored by the cache.
	 */
	public void add(KeyType imageKeyType, Bitmap image) {
		this.emptyQueue(); 
		if (imageKeyType == null || image == null) {
			return;
		}
		LinkedList<SoftListValue<KeyType>> storedKeyTypeImages = this.cache.get(imageKeyType);
		if (storedKeyTypeImages == null) {
			storedKeyTypeImages = new LinkedList<SoftListValue<KeyType>>();
			storedKeyTypeImages.add(new SoftListValue<KeyType>(imageKeyType, image, this.queue));
			this.cache.put(imageKeyType, storedKeyTypeImages);
			return;
		}
		// insert new one in ascending order measured by pixel amount
		int imagePixel = image.getWidth() * image.getHeight();
		ListIterator<SoftListValue<KeyType>> it = storedKeyTypeImages.listIterator();
		while (it.hasNext()) {
			Bitmap next = it.next().get();
			if (next != null && next.getWidth() * next.getHeight() > imagePixel) {
				it.previous();
				it.add(new SoftListValue<KeyType>(imageKeyType, image, this.queue));
				return; // insertion finished, no need to iterate further
			}
		}
	}
	
	/**
	 * Returns a Bitmap referenced by the given image KeyType key which
	 * has the given width and height or -if there is no such image-the first image
	 * with greater size (measured by total pixels).
	 * @param imageKeyType The image KeyType key.
	 * @param width The width of the image.
	 * @param height The height of the image.
	 * @return A Bitmap referenced by the KeyType key which has exactly the
	 * given width and height or if no such image exists, the first image which
	 * has more than width*height pixels.
	 */
	public Bitmap get(KeyType imageKeyType, int width, int height) {
		this.emptyQueue(); 
		if (imageKeyType == null) {
			return null;
		}
		LinkedList<SoftListValue<KeyType>> storedKeyTypeImages = this.cache.get(imageKeyType);
		int wantedPixels = width * height;
		int currHeight;
		int currWidth;
		if (storedKeyTypeImages != null) {
			Iterator<SoftListValue<KeyType>> it = storedKeyTypeImages.iterator();
			while (it.hasNext()) {
				Bitmap currImage = it.next().get();
				if (currImage != null) {
					currHeight = currImage.getHeight();
					currWidth = currImage.getWidth();
					if (currHeight * currWidth > wantedPixels 
							|| (width == currWidth && height == currHeight)) {
						// if firstly exceeding or exactly matching, return the current
						return currImage;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns the biggest stored image for this image KeyType, measured in total pixels.
	 * @param imageKeyType The image KeyType referencing the image.
	 * @return The biggest stored image for this image KeyType, can be <code>null</code>
	 * in rare cases where the image gets deleted right between some lines of code. <code>null</code>
	 * if imageKeyType is <code>null</code> or if no images stored for this KeyType.
	 */
	public Bitmap getBiggest(KeyType imageKeyType) {
		this.emptyQueue(); 
		if (imageKeyType == null) {
			return null;
		}
		LinkedList<SoftListValue<KeyType>> storedKeyTypeImages = this.cache.get(imageKeyType);
		if (storedKeyTypeImages != null) {
			return storedKeyTypeImages.getLast().get();
		}
		return null;
	}
	
	/**
	 * Returns all images referenced by the given image KeyType key.
	 * @param imageKeyType The image KeyType key.
	 * @return All valid images in the cache referenced by the given image
	 * KeyType.
	 */
	public LinkedList<Bitmap> getAll(KeyType imageKeyType) {
		this.emptyQueue(); 
		if (imageKeyType == null) {
			return null;
		}
		LinkedList<SoftListValue<KeyType>> storedKeyTypeImages = this.cache.get(imageKeyType);
		if (storedKeyTypeImages != null) {
			Iterator<SoftListValue<KeyType>> it = storedKeyTypeImages.iterator();
			LinkedList<Bitmap> images = new LinkedList<Bitmap>();
			while (it.hasNext()) {
				Bitmap currImage = it.next().get();
				if (currImage != null) {
					images.add(currImage);
				}
			}
			return images;
		}
		return null;
	}
	
	/**
	 * Removes all images referenced by the given image KeyType.
	 * @param imageKeyType The image KeyType key.
	 * @return <code>true</code> if there were images referenced
	 * by the given image KeyType which were then removed from caching.
	 */
	public boolean remove(KeyType imageKeyType) {
		this.emptyQueue();
        return imageKeyType != null && this.cache.remove(imageKeyType) != null;
    }
	
	/**
	 * Empties the {@link java.lang.ref.ReferenceQueue} by removing
	 * values in the list for which the SoftReferences lost
	 * their value and removing the mapping for KeyType key's
	 * which do not reference any image anymore.
	 */
	@SuppressWarnings("unchecked")
	private void emptyQueue() {
		SoftListValue<KeyType> sv;
	    while ((sv = (SoftListValue<KeyType>) this.queue.poll()) != null) {
	    	LinkedList<SoftListValue<KeyType>> list = this.cache.get(sv.key);
	    	list.remove(sv);
	    	//if list is empty, remove the entry completely
			if (list.size() == 0) {
				this.cache.remove(sv.key);
			}
	    }
	}
	
	/**
	 * This class defines a SoftReference for Bitmaps
	 * which keeps track of the key for the mapping to easily remove
	 * the mapping and the element from the according list.
	 * @author Daniel
	 *
	 */
	private static class SoftListValue<KeyType> extends SoftReference<Bitmap> {
		private final KeyType key;
		
		public SoftListValue(KeyType key, Bitmap referent,
				ReferenceQueue<? super Bitmap> q) {
			super(referent, q);
			this.key = key;
		}
		
		@Override
		public String toString() {
			return "SR to " + this.key.toString();
		}
	}
}
