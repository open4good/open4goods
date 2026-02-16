package org.open4goods.icecat.services;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.AttributesFeatureGroups;
import org.open4goods.icecat.model.IcecatCategory;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatLanguageHandler;
import org.open4goods.icecat.model.IcecatName;
import org.open4goods.icecat.services.loader.CategoryLoader;
import org.open4goods.icecat.services.loader.FeatureLoader;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.FeatureGroup;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;


/**
 * This service preloads some open4goods with (great) icecat data.
 * Brands are injected in BrandService
 * FeatureGroups are injected in verticals
 *
 *   It also provides endpoints to access icecat features, languages, and so on....
 * TODO : all Long to Integer
 */
public class IcecatService {

                private static final Logger LOGGER = LoggerFactory.getLogger(IcecatService.class);

                private final RemoteFileCachingService fileCachingService;
                private final String remoteCachingFolder;
                private final XmlMapper xmlMapper;
                private final IcecatConfiguration iceCatConfig;
                private final FeatureLoader featureLoader;
                private final CategoryLoader categoryLoader;

                // For language
                private Map<String, String> codeByLanguage;
                private Map<String, String> languageByCode;



		/**
		 * Constructor
		 */
        public IcecatService(XmlMapper xmlMapper,
                             IcecatConfiguration iceCatConfig,
                             RemoteFileCachingService fileCachingService,
                             String remoteCacheFolder,
                             FeatureLoader featureLoader,
                             CategoryLoader categoryLoader) {
                this.xmlMapper = xmlMapper;
                this.iceCatConfig = iceCatConfig;
                this.fileCachingService = fileCachingService;
                this.remoteCachingFolder = remoteCacheFolder;
                this.featureLoader = featureLoader;
                this.categoryLoader = categoryLoader;

		try {
			icecatInit();
		} catch (TechnicalException e) {
			LOGGER.error("Error while initializing Icecat", e);
		}
	}


	public void icecatInit () throws TechnicalException {

		// Orders matters
		// For names on feature groups
                featureLoader.loadFeatureGroups();
                loadLanguages();
                featureLoader.loadBrands();

                categoryLoader.loadCategories();


                featureLoader.loadFeatures();
                categoryLoader.loadCategoryFeatureList();

		LOGGER.info("Icecat up and running");
	}

	/**
	 * Load features from the IceCat XML file.
	 * @throws TechnicalException
	 * TODO : Should be done in a separate thread
	 */



	/**
	 * Load features Groups from the IceCat XML file.
	 * @throws TechnicalException
	 * TODO : Should be done in a separate thread
	 */


	public void loadLanguages() throws TechnicalException {

		// 1 - Download the file with basic auth

		// Unzip it
		if (null == iceCatConfig.getLanguageListFileUri()) {
			LOGGER.error("No features list file uri configured");
			return;
		}
		LOGGER.info("Getting file from {}", iceCatConfig.getLanguageListFileUri());
		File icecatFile = getCachedFile(iceCatConfig.getLanguageListFileUri(), iceCatConfig.getUser(), iceCatConfig.getPassword());

		 try {

                        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
                        xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
                        xmlReader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                        xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

                        // Créez une instance de votre gestionnaire
			 IcecatLanguageHandler handler = new IcecatLanguageHandler();

			 // Configurez le XMLReader pour utiliser votre gestionnaire
			 xmlReader.setContentHandler(handler);

			 // Parse le fichier XML
			 FileInputStream inputStream = new FileInputStream(icecatFile);
			 xmlReader.parse(new InputSource(inputStream));

			 // Récupérez et affichez la map des languages
			 this.languageByCode  = handler.getLanguageByCode();
			 this.codeByLanguage  = handler.getCodeBylanguage();

		 } catch (Exception e) {
			LOGGER.error("Error while loading languages", e);
		}
		 LOGGER.info("End loading of features from {}", iceCatConfig.getFeaturesListFileUri());
	}







	/**
	 * Resolve a feature name to one or more feature ID.
	 * @param featureName
	 * @return
	 */
        public Set<Integer> resolveFeatureName (String featureName) {
                String f = IdHelper.normalizeAttributeName(featureName);
                return featureLoader.getFeaturesByNames().get(f);
        }







	/**
	 * Donload feature file, unzip it and maintain the cached version
	 * TODO(design,p3) : Should defer in the remotefilecachingservice
	 * @param url
	 * @param user
	 * @param password
	 * @return
	 * @throws TechnicalException
	 */
	private File getCachedFile(String url, String user, String password) throws TechnicalException {

		LOGGER.info("Retrieving file : {}", url);
		File destFile = new File(remoteCachingFolder+File.separator+IdHelper.getHashedName(url));
		// Return the cached file if it exists
		if (destFile.exists()) {
			// TODO (p3, evolution): Have a refresh policy
			LOGGER.info("File {} already cached", url);
			return destFile;
		}

		File tmpFile = new File(remoteCachingFolder+File.separator+"tmp-"+IdHelper.getHashedName(url));
		try {
			LOGGER.info("Starting download : {}", url);
			fileCachingService.downloadTo(user, password, url, tmpFile);
			LOGGER.info("Uncompressing file : {}", tmpFile);
			fileCachingService.decompressGzipFile(tmpFile,destFile);
			LOGGER.info("File {} uncompressed", url);
			return destFile;
		} catch (Exception e) {
			throw new TechnicalException("Error retrieving resource",e);
		} finally {
			FileUtils.deleteQuietly(tmpFile);
		}
	}


	// TODO : Perf, caching
        public String getFeatureName(Integer featureID, String language) {

                IcecatFeature feature = featureLoader.getFeaturesById().get(featureID);
		Integer icecatLanguage = getIceCatLangId(language);
		if (null != feature) {

			List<IcecatName> names = feature.getNames().getNames();
			for (IcecatName name : names) {
				if (name.getLangId() == icecatLanguage.intValue()) {
					return name.getValue() == null ? name.getTextValue() : name.getValue();
				}
			}
		}
		return "Unsolved : " + featureID + ","+ icecatLanguage;
	}


	private Integer getIceCatLangId(String language) {
		// TODO : check default language is english
		if (null == languageByCode) {
			return 1;
		}
		return Integer.valueOf(languageByCode.getOrDefault(language, "1"));
	}



	/**
	 * Loads the FeatureGroups for a given product, according to Icecat taxonomy
	 * @param vertical
	 * @param language
	 * @param product
	 * @return
	 */
	// TODO : perf, quiet expensive with the "iteration" model. Must be fixed by proper injection
	// of icecat stuff to open4goods attribute model
	@io.micrometer.core.annotation.Timed(value = "icecat.features", description = "Time taken to resolve icecat features", extraTags = {"service", "icecat"})
	public List<AttributesFeatureGroups> features(VerticalConfig vertical, String language, Product product) {
		List<AttributesFeatureGroups> ret = new ArrayList<>();

		Integer icecatLanguage = getIceCatLangId(language);

		// Initial building
		if (null != vertical) {
			for (FeatureGroup fg : vertical.getFeatureGroups()) {
				AttributesFeatureGroups ufg = new AttributesFeatureGroups();
				ufg.setFeatureGroup(fg);
				ufg.setName(ufg.getFeatureGroup().getName().i18n(language));
				for (Integer fId : fg.getFeaturesId()) {
					ProductAttribute a = product.getAttributes().attributeByFeatureId(fId);
					if (null != a) {
						ufg.getAttributes().add(a);
						// Updating attribute name
                                                IcecatFeature f = featureLoader.getFeaturesById().get(fId);
						// TODO : Perf
						IcecatName i18nName = f.getNames().getNames().stream().filter(e->e.getLangId() == icecatLanguage).findFirst().orElse(null);
						if (null != i18nName) {
							a.setName(i18nName.getTextValue());
						}


						// Splitting on "," for multi values
						if (a.getValue() != null && a.getValue().contains(",")) {
							String[] values = a.getValue().split(",");
							if (values.length > 2)
							{
									// No real multi value
								StringBuilder sb =	new StringBuilder();
								sb.append("<ul>");
								for (String value : values) {
	                                sb.append("<li>").append(value).append("</li>");
	                            }
								sb.append("</ul>");
								a.setValue(sb.toString());
							}
						}
					}
				}

				if (ufg.getAttributes().size() > 0) {
					ret.add(ufg);
				}
			}
		}

		return ret;

	}

	public Set<Integer> featuresId(VerticalConfig vertical) {
		Set<Integer> ret = new HashSet<>();
		// Initial building
		if (null != vertical) {
			for (FeatureGroup fg : vertical.getFeatureGroups()) {
				for (Integer fId : fg.getFeaturesId()) {
					ret.add(fId);
				}

			}
		}

		return ret;

	}

	/**
	 * Loads the list of features, aggegated by UiFeatureGroup
	 * @param vertical
	 * @param language
	 * @param product
	 * @return
	 */
	// TODO  (P1, perf) : Caching of icecat stuff to open4goods attribute model
	public Map<String, String> types(VerticalConfig vertical) {
		Map<String,String> ret = new HashMap<>();

		// Initial building
		if (null != vertical) {
			for (FeatureGroup fg : vertical.getFeatureGroups()) {
				FeatureGroup ufg = new FeatureGroup();

				for (Integer fId : fg.getFeaturesId()) {
						// Updating attribute name
                                                IcecatFeature f = featureLoader.getFeaturesById().get(fId);

						// TODO : Perf
						IcecatName i18nName = f.getNames().getNames().stream().filter(e->e.getLangId() == 1).findFirst().orElse(null);

						if (null != i18nName) {
							ret.put(i18nName.getTextValue(), f.getType());
						} else {
							LOGGER.error("Name not found for feature {} - {}",fId, f);
						}
				}

			}
		}

		return ret;

	}


	/**
	 * Resolve the icecat features id, and apply the english name if an unconflicted match is found.
	 * The resolution is operated on the vertical matching features id if set, on all features id if not set
	 * @param name
	 * @return
	 */
	public String getOriginalEnglishName(String name, VerticalConfig vc) {

		Set<Integer> featuresId = resolveFeatureName(name);

		if (featuresId == null) {
			LOGGER.warn("No icecat name found for {}",name);
			return name;
		}

		if (vc != null && vc.getId() != null) {

			// Cloning, to not modify the original map
			featuresId = new HashSet<Integer>(featuresId);
			featuresId.retainAll(featuresId(vc));
			if (featuresId.size() == 0) {
				LOGGER.warn("No icecat featureID for {}, after filtering on id's for vertical {}",name, vc);
				return name;
			}
		}

	 if (featuresId.size() ==1) {
			String ret = getFeatureName(featuresId.stream().findFirst().orElse(null), "en");
			LOGGER.error("Resolved feature name : {}->{}",name, ret);
			return ret;

		} else {
			Set<String> attrNames = featuresId.stream().map(e-> e + ":"+getFeatureName(e, "en")).collect(Collectors.toSet());
			LOGGER.warn("Conflict ! attr {} can be resolved to {}", name, attrNames);
			return name;
		}
	}


        public Map<Integer, IcecatFeature> getFeaturesById() {
                return featureLoader.getFeaturesById();
        }


        public void setFeaturesById(Map<Integer, IcecatFeature> featuresById) {
                featureLoader.getFeaturesById().clear();
                featureLoader.getFeaturesById().putAll(featuresById);
        }


        public Map<Integer, IcecatCategory> getCategoriesById() {
                return categoryLoader.getCategoriesById();
        }


        public void setCategoriesById(Map<Integer, IcecatCategory> categoriesById) {
                categoryLoader.getCategoriesById().clear();
                categoryLoader.getCategoriesById().putAll(categoriesById);
        }




}
