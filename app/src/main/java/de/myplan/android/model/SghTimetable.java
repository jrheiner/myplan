package de.myplan.android.model;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class SghTimetable {
    private final Document document;

    public SghTimetable(Document[] sources) {
        document = new Document("http://localhost/");
        Element root = new Element("html");
        root.appendChild(new Element("head"));
        root.appendChild(new Element("body")
                .attr("style", "background-color: $BACKGROUND_COLOR"));
        document.appendChild(root);
        // Restore header containing CSS
        document.head().appendChild(sources[0].head().selectFirst("meta"));
        document.head().appendChild(sources[0].head().selectFirst("style"));
        // Create body root element
        Element center = new Element("center");
        loadContent(center, sources);
        document.body().appendChild(center);
    }

    public Document getDocument() {
        return document;
    }

    private void loadContent(Element root, Document[] sources) {
        String lastDate = "";
        for (Document source : sources) {
            // Get date
            String titleText = source.body().selectFirst("div.mon_title").text();
            String date = titleText.substring(0, titleText.indexOf(' '));

            if (!date.equals(lastDate)) { // New day with own table
                root.appendChild(new Element("br"));
                root.appendChild(new Element("h3")
                        .attr("style", "color: $TEXT_COLOR;")
                        .text(titleText.replaceAll("\\(.*\\)", "")));
                loadInfoTable(root, source);
                lastDate = date;
            } else { // Append last day

            }
        }
    }

    private void loadInfoTable(Element root, Document source) {
        // Parse info table
        Element infoTable = source.body().selectFirst("table.info");
        for (Element tr : infoTable.select("tr.info")) {
            String text = tr.text();
            if (text.contains("Nachrichten zum Tag")
                    || text.contains("Abwesende Lehrer")
                    || text.contains("Abwesende Klassen")) {
                tr.remove();
            }
        }
        if (!infoTable.getElementsByTag("tr").isEmpty()) {
            root.appendChild(infoTable);
            root.appendChild(new Element("p"));
        }
    }
}
