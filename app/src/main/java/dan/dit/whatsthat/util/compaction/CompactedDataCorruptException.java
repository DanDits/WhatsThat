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

public class CompactedDataCorruptException extends Exception {
	private static final long serialVersionUID = -5192264960343340894L;
	private String corruptData;
	
	public CompactedDataCorruptException() {
		super();
	}
	
	public CompactedDataCorruptException(String detailMessage) {
		super(detailMessage);
	}
	
	public CompactedDataCorruptException(Throwable cause) {
		super (cause);
	}

	public CompactedDataCorruptException(String message,
			Throwable cause) {
		super(message, cause);
	}

	private CompactedDataCorruptException setCorruptData(String corruptData) {
		this.corruptData = corruptData;
		return this;
	}
	
	public String getCorruptData() {
		return corruptData == null ? "" : corruptData;
	}

	public CompactedDataCorruptException setCorruptData(Compacter dataCompressor) {
		return setCorruptData(dataCompressor == null ? null : dataCompressor.compact());
	}
	
	public String toString() {
	    return super.getMessage() + " corrupt: " + corruptData;
	}
}
