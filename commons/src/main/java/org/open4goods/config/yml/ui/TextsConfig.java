package org.open4goods.config.yml.ui;

import java.util.List;

public class TextsConfig {

	
	  private String url;
      private String prefix;
      private List<String> attr;
      private List<String> h1Title;
      private List<String> metaTitle;
      private List<String> metaDescription;
      private String opengraphTitle;
      private String openGraphDescription;
      private String twitterTitle;
      private String twitterDescription;
      private String defaultTemplate;

      // Getters and setters

      public String getUrl() {
          return url;
      }

      public void setUrl(String url) {
          this.url = url;
      }

      public String getPrefix() {
          return prefix;
      }

      public void setPrefix(String prefix) {
          this.prefix = prefix;
      }

      public List<String> getAttr() {
          return attr;
      }

      public void setAttr(List<String> attr) {
          this.attr = attr;
      }

      public List<String> getH1Title() {
          return h1Title;
      }

      public void setH1Title(List<String> h1Title) {
          this.h1Title = h1Title;
      }

      public List<String> getMetaTitle() {
          return metaTitle;
      }

      public void setMetaTitle(List<String> metaTitle) {
          this.metaTitle = metaTitle;
      }

      public List<String> getMetaDescription() {
          return metaDescription;
      }

      public void setMetaDescription(List<String> metaDescription) {
          this.metaDescription = metaDescription;
      }

      public String getOpengraphTitle() {
          return opengraphTitle;
      }

      public void setOpengraphTitle(String opengraphTitle) {
          this.opengraphTitle = opengraphTitle;
      }

      public String getOpenGraphDescription() {
          return openGraphDescription;
      }

      public void setOpenGraphDescription(String openGraphDescription) {
          this.openGraphDescription = openGraphDescription;
      }

      public String getTwitterTitle() {
          return twitterTitle;
      }

      public void setTwitterTitle(String twitterTitle) {
          this.twitterTitle = twitterTitle;
      }

      public String getTwitterDescription() {
          return twitterDescription;
      }

      public void setTwitterDescription(String twitterDescription) {
          this.twitterDescription = twitterDescription;
      }

      public String getDefaultTemplate() {
          return defaultTemplate;
      }

      public void setDefaultTemplate(String defaultTemplate) {
          this.defaultTemplate = defaultTemplate;
      }
      
      
      
}