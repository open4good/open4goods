package org.open4goods.icecat.services.loader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.open4goods.icecat.model.IcecatCatalogueCategory;
import org.open4goods.icecat.util.IcecatConstants;

/**
 * Streams an Icecat reference-catalogue export (CategoriesList, FeaturesList,
 * FeatureGroupsList, LanguageList, or the Full-account CategoryFeaturesList) with a StAX cursor
 * reader, counting elements as they are visited instead of unmarshalling the file into a DOM or
 * JAXB object graph.
 *
 * <p>Icecat's manual documents CategoryFeaturesList.xml as larger than 10 GB for an Open Icecat
 * account; every method here holds at most one in-flight element's attributes in memory, so
 * reading it costs O(1) memory regardless of file size.
 */
public final class IcecatReferenceCatalogueReader {

    private static final XMLInputFactory XML_INPUT_FACTORY = createFactory();

    private IcecatReferenceCatalogueReader() {
    }

    private static XMLInputFactory createFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Icecat exports carry no DTD; disabling external entity resolution avoids XXE.
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory;
    }

    /**
     * Streams the {@code Category} elements found as direct children of the given list element
     * (e.g. {@code CategoriesList} or {@code CategoryFeaturesList}), capturing only each
     * category's own identity: ID, English name, parent ID and score. Nested feature and
     * feature-group content (which can be the bulk of a CategoryFeaturesList export) is walked
     * token-by-token but never captured.
     *
     * @param in            the reference-file content; the caller owns closing it
     * @param listElementName the enclosing list element's local name
     */
    public static List<IcecatCatalogueCategory> readCategories(InputStream in, String listElementName)
            throws XMLStreamException {
        List<IcecatCatalogueCategory> categories = new ArrayList<>();
        XMLStreamReader reader = XML_INPUT_FACTORY.createXMLStreamReader(in);
        try {
            int depth = 0;
            int listDepth = -1;
            int itemDepth = -1;
            Integer id = null;
            Integer parentId = null;
            Integer score = null;
            String englishName = null;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    depth++;
                    String local = reader.getLocalName();
                    if (listDepth == -1 && listElementName.equals(local)) {
                        listDepth = depth;
                    } else if (listDepth != -1 && itemDepth == -1 && depth == listDepth + 1 && "Category".equals(local)) {
                        itemDepth = depth;
                        id = parseInt(reader.getAttributeValue(null, "ID"));
                        score = parseInt(reader.getAttributeValue(null, "Score"));
                        parentId = null;
                        englishName = null;
                    } else if (itemDepth != -1 && depth == itemDepth + 1) {
                        if ("Name".equals(local)) {
                            Integer langId = parseInt(reader.getAttributeValue(null, "langid"));
                            String value = reader.getAttributeValue(null, "Value");
                            String text = reader.getElementText();
                            depth--; // getElementText already consumed this element's END_ELEMENT
                            if (langId != null && langId == IcecatConstants.LANG_ID_ENGLISH) {
                                englishName = value != null ? value : text;
                            }
                        } else if ("ParentCategory".equals(local)) {
                            parentId = parseInt(reader.getAttributeValue(null, "ID"));
                        }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if (itemDepth != -1 && depth == itemDepth) {
                        categories.add(new IcecatCatalogueCategory(id, englishName, parentId, score));
                        itemDepth = -1;
                    }
                    depth--;
                }
            }
        } finally {
            reader.close();
        }
        return categories;
    }

    /** Counts {@code Feature} elements directly under {@code FeaturesList}. */
    public static int countFeatures(InputStream in) throws XMLStreamException {
        return countDirectChildren(in, "FeaturesList", "Feature");
    }

    /** Counts {@code FeatureGroup} elements directly under {@code FeatureGroupsList}. */
    public static int countFeatureGroups(InputStream in) throws XMLStreamException {
        return countDirectChildren(in, "FeatureGroupsList", "FeatureGroup");
    }

    /** Counts {@code Language} elements directly under {@code LanguageList}. */
    public static int countLanguages(InputStream in) throws XMLStreamException {
        return countDirectChildren(in, "LanguageList", "Language");
    }

    private static int countDirectChildren(InputStream in, String listElementName, String itemElementName)
            throws XMLStreamException {
        XMLStreamReader reader = XML_INPUT_FACTORY.createXMLStreamReader(in);
        try {
            int depth = 0;
            int listDepth = -1;
            int count = 0;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    depth++;
                    String local = reader.getLocalName();
                    if (listDepth == -1 && listElementName.equals(local)) {
                        listDepth = depth;
                    } else if (listDepth != -1 && depth == listDepth + 1 && itemElementName.equals(local)) {
                        count++;
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    depth--;
                }
            }
            return count;
        } finally {
            reader.close();
        }
    }

    private static Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
