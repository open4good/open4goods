package org.open4goods.model.icecat;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class IcecatLanguageHandler extends DefaultHandler {


	private Map<String, String> languageByCode = new HashMap<>();
	private Map<String, String> codeBylanguage = new HashMap<>();
    
    private boolean isLanguage = false;
    private String currentCode = null;
    private String currentId = null;



    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("Language".equals(qName)) {
            currentCode = attributes.getValue("ShortCode");
            currentId = attributes.getValue("ID");
            isLanguage = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("Language".equals(qName) && isLanguage) {
        	String code = currentCode.toLowerCase();
            languageByCode.put(code, currentId);
            codeBylanguage.put(currentId, code);
            isLanguage = false;
        }
    }
    
    public Map<String, String> getLanguageByCode() {
        return languageByCode;
    }

	public Map<String, String> getCodeBylanguage() {
		return codeBylanguage;
	}

	public void setCodeBylanguage(Map<String, String> codeBylanguage) {
		this.codeBylanguage = codeBylanguage;
	}
    
    
    
}