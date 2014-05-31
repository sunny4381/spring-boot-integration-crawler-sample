# Spring Boot と Spring Integration を使用したクローラ

Spring Integration 4 から XML が不要になりました。
Spring Boot と Spring Integration を使用して XML を使用せずに Java Config のみを使って単純なクローラを作ってみます。

## 必須

以下のソフトウェアが必要です。あらかじめインストールしておいてください。

* JDK 6 or later
* Maven 3.0 or later

## 概要

開発するクローラーは、[Wikipedia のダンプリスト](http://dumps.wikimedia.org/backup-index.html) をスクレ―ピングして、
次のような情報を取得します。

timestamp           | id           | ref                   | status
--------------------|--------------|-----------------------|-----------------
2014-05-31 01:57:31 | nowiki       | nowiki/20140530       | Dump in progress
2014-05-31 01:57:32 | trwiki       | trwiki/20140530       | Dump in progress
2014-05-30 14:38:08 | anwiki       | anwiki/20140530       | Dump complete
2014-05-30 14:30:29 | viwiktionary | viwiktionary/20140530 | Dump complete
2014-05-30 13:02:57 | ckbwiki      | ckbwiki/20140530      | Dump complete
...                 | ...          | ...                   | ...

## pom.xml

```xml:pom.xml
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>1.1.0.M2</version>
        <relativePath/>
    </parent>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-integration</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jsoup</groupId>
            <artifactId>jsoup</artifactId>
            <version>1.7.3</version>
        </dependency>
    </dependencies>

    <repositories>
        <repository>
            <id>spring-milestones</id>
            <url>http://repo.spring.io/milestone/</url>
        </repository>
    </repositories>
```

スクレ―ピングする際に [jsoup](http://jsoup.org/) を使用するので pom.xml に組み込んでいます。

## Downloader

`Downloader` は、定期的に指定された URL から HTML をダウンロードします。

```java:Downloader.java
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
```

ダウンロードした HTML は、`ResponseEntity<String>` クラスのインスタンスで、 `channel1` に送ります。
どんな間隔でダウンロードするかは、外部の `downloadTrigger` bean で定義します。

## Scraper

`Scraper` は、HTML から目的の部分を抽出します。

```java:Scraper.java
@MessageEndpoint
public class Scraper {
	private final Pattern patter = Pattern.compile("^<li>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} \\S+");

	@Splitter(inputChannel = "channel1", outputChannel = "channel2")
	public List<Element> scrape(ResponseEntity<String> payload) {
		String html = payload.getBody();
		final Document htmlDoc = Jsoup.parse(html);
		final Elements anchorNodes = htmlDoc.select("body").select("ul").select("li");

		final List<Element> anchorList = new ArrayList<Element>();
		anchorNodes.traverse(new NodeVisitor() {
			@Override
			public void head(org.jsoup.nodes.Node node, int depth) {
				if (node instanceof org.jsoup.nodes.Element) {
					Element e = (Element)node;
					anchorList.add(e);
				}
			}

			@Override
			public void tail(Node node, int depth) {
			}
		});

		return anchorList;
	}

	@Filter(inputChannel = "channel2", outputChannel = "channel3")
	public boolean filter(Element payload) {
		Matcher m = patter.matcher(payload.toString());
		return m.find();
	}

	@Transformer(inputChannel = "channel3", outputChannel = "channel4")
	public DumpEntry convert(Element payload) throws ParseException {
		String dateStr = payload.ownText().substring(0, 19);

		DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));

		Date timestamp = format.parse(dateStr);

		Elements list = payload.select("a");
		String id;
		String ref;
		if (list.size() > 0) {
			Element a = list.get(0);
			id = a.ownText();
			ref = a.attr("href");
		} else {
			id = "private data";
			ref = null;
		}

		Element span = payload.select("span").get(0);
		String status = span.ownText();

		return new DumpEntry(timestamp, id, ref, status);
	}
}
```

`channel1` から受け取った HTML から body/ul/li 要素を抽出し、必要な li 要素を選別し、li 要素を `DompEntry` に変換し、
channel4 に送ります。

## DompEntry

`DompEntry` は、目的の部分を表すエンティティです。

```java:DumpEntry
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
```

## CrawlerConfig

クローラの設定を表します。

```java:CrawlerConfig
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
```

既定では、1時間ごとに "http://dumps.wikimedia.org/backup-index.html" から HTML をダウンロードします。

## CrawlerApp

最後にアプリケーションクラスです。

```java:CrawlerApp
@Configuration
@ComponentScan
@EnableAutoConfiguration
@EnableConfigurationProperties
public class CrawlerApp {
	private static Logger LOG = LoggerFactory.getLogger(CrawlerApp.class);

	public static void main(String[] args) throws Exception {
		ApplicationContext ctx = SpringApplication.run(CrawlerApp.class, args);
		System.in.read();
		Runtime.getRuntime().exit(SpringApplication.exit(ctx));
	}

	@Autowired
	private CrawlerConfig config;

	@PostConstruct
	public void postConstruct() {
		LOG.info("starting crawler with config={}", config);
	}

	@MessageEndpoint
	public static class Endpoint {
		@ServiceActivator(inputChannel="channel4")
		public void log(DumpEntry payload) {
			LOG.info("entry={}", payload);
		}
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public PollerMetadata downloadTrigger() {
		PeriodicTrigger trigger = new PeriodicTrigger(config.getDownloadInterval());
		trigger.setFixedRate(true);
		PollerMetadata pollerMetadata = new PollerMetadata();
		pollerMetadata.setTrigger(trigger);
		pollerMetadata.setMaxMessagesPerPoll(1);
		return pollerMetadata;
	}

	@Bean
	public MessageChannel channel1() {
		return new QueueChannel(10);
	}

	@Bean
	public MessageChannel channel2() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel channel3() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel channel4() {
		return new QueueChannel(10);
	}

	// <int:poller id="poller" default="true" fixed-rate="10"/>
	@Bean(name = PollerMetadata.DEFAULT_POLLER)
	public PollerMetadata poller() {
		PeriodicTrigger trigger = new PeriodicTrigger(10);
		trigger.setFixedRate(true);
		PollerMetadata pollerMetadata = new PollerMetadata();
		pollerMetadata.setTrigger(trigger);
		return pollerMetadata;
	}
}
```

`CrawlerApp` では `downloadTrigger` にダウンロード間隔を定義したり、
`channel1` から `channel4` の各チャンネルを定義しています。

また、`CrawlerApp` では、`channel4` から受信した `DumpEntry` をログに出力しています。
実際にはファイルに書いたり、DB に保存したり、MQ に送るなどして外部のシステムと連携します。

## 実行方法＆実行例

次のように実行します。

```実行例
mvn package
java -jar target/spring-boot-integration-crawler-sample-1.0.jar
```
## Complete Source Code

```
git clone https://github.com/sunny4381/spring-boot-integration-crawler-sample.git
```
