package com.example.blog.client;

import com.example.blog.client.dto.RssFeedItem;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * RSS 抓取客户端。
 */
@Component
public class RssFeedClient {

    @Resource(name = "rssRestTemplate")
    private RestTemplate rssRestTemplate;

    /**
     * 抓取单个 RSS Feed。
     *
     * @param feedUrl Feed 地址
     * @return RSS 条目列表
     */
    public List<RssFeedItem> fetchFeed(String feedUrl) {
        try {
            String xml = rssRestTemplate.getForObject(feedUrl, String.class);
            return parseFeed(feedUrl, xml);
        } catch (RestClientException exception) {
            throw new IllegalStateException("抓取 RSS 失败: " + exception.getMessage(), exception);
        }
    }

    /**
     * 解析 RSS 或 Atom 内容。
     *
     * @param feedUrl Feed 地址
     * @param xml 原始 XML
     * @return RSS 条目列表
     */
    private List<RssFeedItem> parseFeed(String feedUrl, String xml) {
        if (!StringUtils.hasText(xml)) {
            return List.of();
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            document.getDocumentElement().normalize();

            NodeList itemNodes = document.getElementsByTagName("item");
            if (itemNodes.getLength() > 0) {
                return parseRssItems(feedUrl, itemNodes);
            }

            NodeList entryNodes = document.getElementsByTagName("entry");
            if (entryNodes.getLength() > 0) {
                return parseAtomEntries(feedUrl, entryNodes);
            }
            return List.of();
        } catch (Exception exception) {
            throw new IllegalStateException("解析 RSS 失败: " + exception.getMessage(), exception);
        }
    }

    /**
     * 解析 RSS item 列表。
     *
     * @param feedUrl Feed 地址
     * @param itemNodes 节点列表
     * @return RSS 条目列表
     */
    private List<RssFeedItem> parseRssItems(String feedUrl, NodeList itemNodes) {
        List<RssFeedItem> items = new ArrayList<>();
        for (int index = 0; index < itemNodes.getLength(); index++) {
            Node node = itemNodes.item(index);
            if (!(node instanceof Element element)) {
                continue;
            }
            RssFeedItem item = new RssFeedItem();
            item.setFeedUrl(feedUrl);
            item.setTitle(readChildText(element, "title"));
            item.setSummary(readChildText(element, "description"));
            item.setUrl(readChildText(element, "link"));
            item.setAuthor(readChildText(element, "author"));
            item.setPublishedAt(parseDateTime(readChildText(element, "pubDate")));
            items.add(item);
        }
        return items;
    }

    /**
     * 解析 Atom entry 列表。
     *
     * @param feedUrl Feed 地址
     * @param entryNodes 节点列表
     * @return RSS 条目列表
     */
    private List<RssFeedItem> parseAtomEntries(String feedUrl, NodeList entryNodes) {
        List<RssFeedItem> items = new ArrayList<>();
        for (int index = 0; index < entryNodes.getLength(); index++) {
            Node node = entryNodes.item(index);
            if (!(node instanceof Element element)) {
                continue;
            }
            RssFeedItem item = new RssFeedItem();
            item.setFeedUrl(feedUrl);
            item.setTitle(readChildText(element, "title"));
            item.setSummary(readChildText(element, "summary"));
            item.setUrl(readAtomLink(element));
            item.setAuthor(readChildText(element, "name"));
            item.setPublishedAt(parseDateTime(readChildText(element, "published")));
            if (item.getPublishedAt() == null) {
                item.setPublishedAt(parseDateTime(readChildText(element, "updated")));
            }
            items.add(item);
        }
        return items;
    }

    /**
     * 读取子节点文本。
     *
     * @param parent 父节点
     * @param tagName 标签名
     * @return 子节点文本
     */
    private String readChildText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) {
            return null;
        }
        Node node = nodeList.item(0);
        return node == null ? null : node.getTextContent();
    }

    /**
     * 读取 Atom 的链接地址。
     *
     * @param element entry 节点
     * @return 链接地址
     */
    private String readAtomLink(Element element) {
        NodeList nodeList = element.getElementsByTagName("link");
        for (int index = 0; index < nodeList.getLength(); index++) {
            Node node = nodeList.item(index);
            if (!(node instanceof Element linkElement)) {
                continue;
            }
            String href = linkElement.getAttribute("href");
            if (StringUtils.hasText(href)) {
                return href;
            }
        }
        return null;
    }

    /**
     * 解析发布时间。
     *
     * @param value 原始时间文本
     * @return 发布时间
     */
    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
        } catch (Exception ignored) {
        }
        return null;
    }
}
