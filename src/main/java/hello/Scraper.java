package hello;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.annotation.Transformer;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
