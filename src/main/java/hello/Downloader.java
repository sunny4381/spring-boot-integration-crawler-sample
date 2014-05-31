package hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Poller;
import org.springframework.web.client.RestTemplate;

@MessageEndpoint
public class Downloader {
	@Autowired
	private CrawlerConfig config;

	@Autowired
	private RestTemplate template;

	@InboundChannelAdapter(value = "channel1", poller = @Poller("downloadTrigger"))
	public ResponseEntity<String> download() {
		String url = config.getUrl();
		ResponseEntity<String> entity = template.getForEntity(url, String.class);
		return entity;
	}
}
