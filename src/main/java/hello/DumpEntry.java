/*
 * Copyright 2014 NAKANO Hideo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hello;

import java.io.Serializable;
import java.util.Date;

public class DumpEntry implements Serializable {
	private Date timestamp;
	private String id;
	private String ref;
	private String status;

	public DumpEntry(Date timestamp, String id, String ref, String status) {
		this.timestamp = timestamp;
		this.id = id;
		this.ref = ref;
		this.status = status;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public String getId() {
		return id;
	}

	public String getRef() {
		return ref;
	}

	public String getStatus() {
		return status;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof DumpEntry)) return false;

		DumpEntry dumpEntry = (DumpEntry) o;

		if (!id.equals(dumpEntry.id)) return false;
		if (!ref.equals(dumpEntry.ref)) return false;
		if (!status.equals(dumpEntry.status)) return false;
		if (!timestamp.equals(dumpEntry.timestamp)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = timestamp.hashCode();
		result = 31 * result + id.hashCode();
		result = 31 * result + ref.hashCode();
		result = 31 * result + status.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "DumpEntry{" +
				"timestamp=" + timestamp +
				", id='" + id + '\'' +
				", ref='" + ref + '\'' +
				", status='" + status + '\'' +
				'}';
	}
}
