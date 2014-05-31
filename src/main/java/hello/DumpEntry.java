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
