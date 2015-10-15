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

package dan.dit.whatsthat.util.mosaic.matching;

import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;
/**
 * This class models a special iterator over a treemap. The treemap is divided
 * in a head and tail map at a certain marker key. The next method returns the next closest to
 * this marker alternatingly searching the head and tail map.
 * @author Daniel
 *
 * @param <K> The key type which must be comparable.
 * @param <T> The type of the entries in the treemap.
 */
class DividedTreeMapIterator<K extends Comparable<K>, T> {
	private Iterator<K> ascendingIt;
	private Iterator<K> descendingIt;
	private NavigableMap<K, T> tailMap;
	private NavigableMap<K, T> headMap;
	private boolean searchUp;
	private boolean searchDown;
	private boolean preferedUp;
	
	/**
	 * Creates a new DividedTreeMapIterator dividing the given map at the given key.
	 * @param map The map to divide and iterate through.
	 * @param divideNearby All entries greater than or equal to this key will be in the tail map
	 * and the rest in the head map.
	 */
	public DividedTreeMapIterator(TreeMap<K, T> map, K divideNearby) {
		this.tailMap = map.tailMap(divideNearby, true);
		this.ascendingIt = this.tailMap.navigableKeySet().iterator();
		this.headMap = map.headMap(divideNearby, false);
		this.descendingIt = this.headMap.navigableKeySet().descendingIterator();
		this.preferedUp = true;
		this.searchDown = true;
		this.searchUp = true;
	}
	
	/**
	 * Returns the next key, which is either in the head or tail map and the closest to
	 * the dividing marker which was not yet returned by next (inside the given head or tailmap).
	 * If there is no more key in limit in one part map, the other part map will be used for iteration.
	 * Being in limit means: The key that would be returned is smaller than the upper limit if it is
	 * in the tail map or it is greater than the lower limit if it is in the head map.
	 * @param lowerLimit The lower limit that is checked if the next key is in the head map.
	 * @param upperLimit The higher limit that is checked if the next key is in the tail map.
	 * @return The next element's key of the iteration. If there is no next element <code>null</code> is
	 * returned. After <code>null</code> is being returned, the outcome of this method may yield
	 * unexpected results as one or two entries will be skipped. This iterator is considered to have
	 * no 'next' entry if the remaining head and tail map are empty or if the next key is not in limit.
	 */
	public K next(K lowerLimit, K upperLimit) {
		boolean hasUp = ascendingIt.hasNext();
		boolean hasDown = descendingIt.hasNext();
		K up;
		K down;
		
		// works similiar to a state machine with the three states being
		// SEARCH:UP&DOWN, fails if no higher in limit exists and no lower in limit exists, 
		// changes to SEARCH:UP/DOWN, alternates between preferred up and down search
		// SEARCH:UP, fails if no higher in limit exists
		// SEARCH:DOWN, fails if no lower in limit exists
		if (searchUp && searchDown) {
			if (this.preferedUp) {
				if (hasUp) {
					up = ascendingIt.next();
					if (up.compareTo(upperLimit) <= 0) {
						// has up in limit
						this.preferedUp = !this.preferedUp;
						return up;
					} 
				}
				// has no up or has up not in limit
				if (hasDown) {
					down = descendingIt.next();
					if (down.compareTo(lowerLimit) >= 0) {
						// has no up (in limit) and has down in limit
						searchUp = false;
						return down;
					} else {
						return null; // has no up (in limit) and down out of limit
					}
				} else {
					return null; // has up out of limit or no up and no down
				}
			} else {
				// prefers down
				if (hasDown) {
					down = descendingIt.next();
					if (down.compareTo(lowerLimit) >= 0) {
						// has down in limit
						this.preferedUp = !this.preferedUp;
						return down;
					} 
				}
				// has no down (in limit)
				if (hasUp) {
					up = ascendingIt.next();
					if (up.compareTo(upperLimit) <= 0) {
						// has no down (in limit) and has up in limit
						searchDown = false;
						return up;
					} else {
						return null; // has no down (in limit) and up out of limit
					}
				} else {
					return null; // has down out of limit or no down and no up
				}
			}
		}
		if (searchUp) { // and not searchUp
			if (hasUp) {
				up = ascendingIt.next();
				if (up.compareTo(upperLimit) <= 0) {
					return up;
				} else {
					return null; // has up but not in limit
				}
			} else {
				return null; // has no up
			}
		}
		if (searchDown) {//and not search up
			if (hasDown) {
				down = descendingIt.next();
				if (down.compareTo(lowerLimit) >= 0) {
					return down;
				} else {
					return null; // has down but not in limit
				}
			} else {
				return null; // has no down
			}
		}
		return null; // will not happen
	}
	
	/**
	 * Returns the next entry of the iteration. This uses the next() method and returns
	 * the entry belonging to the returned key.
	 * @param lowerLimit The lower limit.
	 * @param upperLimit The upper limit.
	 * @return The next entry or <code>null</code> if next() returned null.
	 */
	public T nextEntry(K lowerLimit, K upperLimit) {
		K nextKey = this.next(lowerLimit, upperLimit);
		if (nextKey != null) {
			return (this.headMap.containsKey(nextKey) ? this.headMap.get(nextKey) : this.tailMap.get(nextKey));
		} else {
			return null;
		}
	}
}
