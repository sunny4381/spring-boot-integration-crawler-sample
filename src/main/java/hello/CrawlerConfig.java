package hello;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConfigurationProperties
public class CrawlerConfig {
	private static final String DEFAULT_URL = "http://dumps.wikimedia.org/backup-index.html";
	private static final long DEFAULT_DOWNLOAD_INTERVAL = TimeUnit.HOURS.toMillis(1);
	private String url = DEFAULT_URL;
	private long downloadInterval = DEFAULT_DOWNLOAD_INTERVAL;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getDownloadInterval() {
		return downloadInterval;
	}

	public void setDownloadInterval(long downloadInterval) {
		this.downloadInterval = downloadInterval;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		CrawlerConfig that = (CrawlerConfig) o;

		if (downloadInterval != that.downloadInterval) return false;
		if (!url.equals(that.url)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = url.hashCode();
		result = 31 * result + (int) (downloadInterval ^ (downloadInterval >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "CrawlerConfig{" +
				"url='" + url + '\'' +
				", downloadInterval=" + downloadInterval +
				'}';
	}
}
