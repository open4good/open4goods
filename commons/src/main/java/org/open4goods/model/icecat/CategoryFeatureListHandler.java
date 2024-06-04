package org.open4goods.model.icecat;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CategoryFeatureListHandler extends DefaultHandler {

    private List<IcecatCategory> categories = new ArrayList<>();
    private IcecatCategory currentCategory = null;
    private IcecatCategoryFeatureGroup currentCategoryFeatureGroup = null;
    private IcecatFeature currentFeature = null;
    private IcecatMeasure currentMeasure = null;
    private IcecatSign currentSign = null;

    private boolean inFeature = false;
    private boolean inMeasure = false;

    public List<IcecatCategory> getCategories() {
        return categories;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (qName) {
            case "Category":
                currentCategory = new IcecatCategory();
                currentCategory.setId(attributes.getValue("ID"));
                currentCategory.setLowPic(attributes.getValue("LowPic"));
                currentCategory.setUncatId(attributes.getValue("UNCATID"));
                currentCategory.setUpdated(attributes.getValue("Updated"));
                break;
            case "CategoryFeatureGroup":
                currentCategoryFeatureGroup = new IcecatCategoryFeatureGroup();
                currentCategoryFeatureGroup.setId(attributes.getValue("ID"));
                currentCategoryFeatureGroup.setNo(attributes.getValue("No"));
                break;
            case "Feature":
                currentFeature = new IcecatFeature();
                currentFeature.setID(attributes.getValue("ID"));
                inFeature = true;
                break;
            case "Measure":
                currentMeasure = new IcecatMeasure();
                currentMeasure.setID(attributes.getValue("ID"));
                currentMeasure.setSign(attributes.getValue("Sign"));
                currentMeasure.setUpdated(attributes.getValue("Updated"));
                inMeasure = true;
                break;
            case "Sign":
                currentSign = new IcecatSign();
                currentSign.setID(attributes.getValue("ID"));
                currentSign.setLangid(attributes.getValue("langid"));
                currentSign.setUpdated(attributes.getValue("Updated"));
                break;
            case "Name":
                if (inFeature) {
                    
                  	IcecatName n = new IcecatName();
                	n.setValue(attributes.getValue("Value"));
                	n.setId(Integer.valueOf(attributes.getValue("id")));
                	n.setLangId(Integer.valueOf(attributes.getValue("langid")));
                	n.setUpdated(attributes.getValue("Updated"));
                	
                	currentFeature.getNames().getNames().add(n);
                }
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "Category":
                categories.add(currentCategory);
                currentCategory = null;
                break;
            case "CategoryFeatureGroup":
                if (currentCategory != null) {
                    currentCategory.getCategoryFeatureGroups().add(currentCategoryFeatureGroup);
                }
                currentCategoryFeatureGroup = null;
                break;
            case "Feature":
                if (currentCategoryFeatureGroup != null) {
                    currentCategoryFeatureGroup.getFeatures().add(currentFeature);
                }
                currentFeature = null;
                inFeature = false;
                break;
            case "Measure":
                currentFeature.setMeasure(currentMeasure);
                currentMeasure = null;
                inMeasure = false;
                break;
            case "Sign":
                if (inMeasure && currentMeasure != null) {
                	
                	if (null == currentMeasure.getSigns()) {
                		currentMeasure.setSigns(new IcecatSigns());
                	}
                	if (null == currentMeasure.getSigns().getSigns()) {
                		currentMeasure.getSigns().setSigns(new ArrayList<>());
                	}
                	
                    currentMeasure.getSigns().getSigns().add(currentSign);
                }
                currentSign = null;
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (currentSign != null) {
            currentSign.setValue(new String(ch, start, length).trim());
        }
    }
}
