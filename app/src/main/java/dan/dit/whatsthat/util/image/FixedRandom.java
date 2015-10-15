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

package dan.dit.whatsthat.util.image;

/**
 * This class implements a simple 'random' generator which will
 * always return the same fixed sequence of numbers.
 * This is done because I don't trust that a random number generator
 * will always give the same sequence of random numbers for a fixed seed
 * in all implementations and all times.
 * @author daniel
 *
 */
public class FixedRandom {
	private int x = 13371337;
	private int y = 424244244;
	private int z = 562031109;
	private int w = 26421976;
	
	private int next() {
        // algorithm by wikipedia.org
	    int t = x ^ (x << 11);
	    x = y; y = z; z = w;
	    return w = w ^ (w >> 19) ^ t ^ (t >> 8);
	}
	
	
	/**
	 * Returns the next fixed random number between 0 and max. 
	 * Invoking this method multiple times for different instances will always return the same sequence of numbers. 
	 * The parameter only changes the output but will not influence any future numbers.
	 * @param max The maximum number to return
	 * @return A random number.
	 */
	public int next(int max) {
		int res = next();
		res = res < 0 ? -res : res;
		res %= (max + 1);
		return res;
	}
}
