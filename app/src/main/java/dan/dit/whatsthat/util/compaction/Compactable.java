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

package dan.dit.whatsthat.util.compaction;

/**
 * An interface for an arbitrary object that is lightweight enough
 * to simply compress it to a string and later load it from a string.
 * Implementing classes should also provide a constructor which takes a single
 * string value (the compressed data) to rebuilt an object or better: take a single Compacter 
 * object.
 * @author Daniel
 *
 */
public interface Compactable {
	/**
	 * Compresses all important data ('important' as defined by the compressible object)
	 * to a single String. The object should be able to rebuilt itself (completely) from this string.
	 * @return A string that contains the data of this object.
	 */
	String compact();
	
	/**
	 * Loads the compacted data from the Compacter, initializing this objects state and all data
	 * previously saved by the compact method. The object can ignore this method if already invoked or
	 * already in another initialized state.
	 * @param compactedData The compacted data.
	 * @throws CompactedDataCorruptException If too little data or unexpected format or invalid.
	 */
	void unloadData(Compacter compactedData) throws CompactedDataCorruptException;
	
}
