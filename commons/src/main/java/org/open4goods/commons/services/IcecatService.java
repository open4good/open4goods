package org.open4goods.commons.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.IcecatConfiguration;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.exceptions.TechnicalException;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.model.data.Brand;
import org.open4goods.commons.model.data.FeatureGroup;
import org.open4goods.commons.model.dto.AttributesFeatureGroups;
import org.open4goods.commons.model.icecat.IcecatCategory;
import org.open4goods.commons.model.icecat.IcecatCategoryFeatureGroup;
import org.open4goods.commons.model.icecat.IcecatFeature;
import org.open4goods.commons.model.icecat.IcecatFeatureGroup;
import org.open4goods.commons.model.icecat.IcecatLanguageHandler;
import org.open4goods.commons.model.icecat.IcecatModel;
import org.open4goods.commons.model.icecat.IcecatName;
import org.open4goods.commons.model.icecat.IcecatSupplier;
import org.open4goods.commons.model.product.AggregatedAttribute;
import org.open4goods.commons.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
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

		private  Logger LOGGER = LoggerFactory.getLogger(IcecatService.class);
		
		private BrandService brandService;
		private RemoteFileCachingService fileCachingService;
		private GoogleTaxonomyService googleTaxonomyService;
		private VerticalsConfigService verticalsConfigService;
		private String remoteCachingFolder;
		
		private XmlMapper xmlMapper;
		private IcecatConfiguration iceCatConfig;

		// For features
		private Map<Integer, IcecatFeature> featuresById = new HashMap<>();
		private Map<String, Set<Integer>> featuresByNames = new HashMap<>();
		
		// For language
		private Map<String, String> codeByLanguage;
		private Map<String, String> languageByCode;
		
		 // Créez un XMLReader
		private Map<Integer,List<FeatureGroup>> featureGroupsByCategoryId = new HashMap<>();
		private Map<Integer, IcecatCategory> categoriesById = new HashMap<>();
		private Map<Integer,IcecatFeatureGroup> featureGroupsById = new HashMap<>();
		
		
		
		/**
		 * Constructor
		 */
	public IcecatService(XmlMapper xmlMapper, IcecatConfiguration iceCatConfig, RemoteFileCachingService fileCachingService, String remoteCacheFolder, BrandService brandService, VerticalsConfigService verticalsConfigService) throws SAXException {
		super();
		this.xmlMapper = xmlMapper;
		this.iceCatConfig = iceCatConfig;
		this.fileCachingService = fileCachingService;
		this.remoteCachingFolder = remoteCacheFolder;
		this.brandService = brandService;
		this.verticalsConfigService = verticalsConfigService;

		try {
			icecatInit();
		} catch (TechnicalException e) {
			LOGGER.error("Error while initializing Icecat", e);
		}
	}


	public void icecatInit () throws TechnicalException {
		
		// Orders matters
		// For names on feature groups
		loadFeatureGroups();
		loadLanguages();
		loadBrands();

		loadCategories();
		
		
		loadFeatures();
		loadCategoryFeatureList();
		
		LOGGER.info("Icecat up and running");
	}
	
	/**
	 * Load features from the IceCat XML file.
	 * @throws TechnicalException 
	 * TODO : Should be done in a separate thread 
	 */

	public void loadFeatures() throws TechnicalException {
		
		// 1 - Download the file with basic auth
		
		// Unzip it
		if (null == iceCatConfig.getFeaturesListFileUri()) {
			LOGGER.error("No features list file uri configured");
			return;
		}
		LOGGER.info("Getting file from {}", iceCatConfig.getFeaturesListFileUri());
		File icecatFile = getCachedFile(iceCatConfig.getFeaturesListFileUri(), iceCatConfig.getUser(), iceCatConfig.getPassword());
		

		 try {
			 List<IcecatFeature> features = xmlMapper.readValue(icecatFile, IcecatModel.class).getResponse().getFeaturesList().getFeatures();
			
			features.forEach(feature -> {
				
				Integer id = Integer.valueOf(feature.getID());
				// Loading the by id map
				featuresById.put(id, feature);
				
				// Loading the by name map
				feature.getNames().getNames(). forEach(name -> {
                  
					String val = normalize(name.getTextValue());
					Set<Integer> fIds = featuresByNames.get(val);
                    if (fIds == null) {
                        fIds = new HashSet<>();
                    }
                    fIds.add(id);
                    featuresByNames.put(val, fIds);

//                    if (fIds.size() > 1) {
//                    	LOGGER.debug("Feature name {} map's multiple features ({}) ", name.getValue(), fIds);
//                    }

                });
			});
			
			
		} catch (Exception e) {
			LOGGER.error("Error while loading features", e);
		}
		 LOGGER.info("End loading of features from {}", iceCatConfig.getFeaturesListFileUri());
	}

	
	/**
	 * Load features Groups from the IceCat XML file.
	 * @throws TechnicalException 
	 * TODO : Should be done in a separate thread 
	 */

	public void loadFeatureGroups() throws TechnicalException {
		
		// 1 - Download the file with basic auth
		
		// Unzip it
		if (null == iceCatConfig.getFeatureGroupsFileUri()) {
			LOGGER.error("No features group list file uri configured");
			return;
		}
		LOGGER.info("Getting file from {}", iceCatConfig.getFeatureGroupsFileUri());
		File icecatFile = getCachedFile(iceCatConfig.getFeatureGroupsFileUri(), iceCatConfig.getUser(), iceCatConfig.getPassword());
		

		 try {
			 List<IcecatFeatureGroup> features = xmlMapper.readValue(icecatFile, IcecatModel.class).getResponse().getFeatureGroupsList().getFeatureGroups();
			
				for (IcecatFeatureGroup fg : features) {
					featureGroupsById.put(fg.getID(), fg);
				}

			
			
		} catch (Exception e) {
			LOGGER.error("Error while loading feature groups", e);
		}
		 LOGGER.info("End loading of features from {}", iceCatConfig.getFeaturesListFileUri());
	}

	
	/**
	 * Load features from the IceCat XML file.
	 * @throws TechnicalException 

	 */

	public void loadBrands() throws TechnicalException {
		
		// 1 - Download the file with basic auth
		
		// Unzip it
		if (null == iceCatConfig.getBrandsListFileUri()) {
			LOGGER.error("No brands list file uri configured");
			return;
		}
		LOGGER.info("Getting brands file from {}", iceCatConfig.getBrandsListFileUri());
		File icecatFile = getCachedFile(iceCatConfig.getBrandsListFileUri(), iceCatConfig.getUser(), iceCatConfig.getPassword());
		

		 try {
			 List<IcecatSupplier> suppliers = xmlMapper.readValue(icecatFile, IcecatModel.class).getResponse().getSuppliersList().getSuppliers();
			
				for (IcecatSupplier supplier : suppliers) {
					Brand brand = brandService.getBrandByName(supplier.getName());
					if (null == brand) {
						// The icecat brand has not been defined by YAML config, we add it
						brand = new Brand();
						brand.setName(supplier.getName());
						brand.setLogo(supplier.getLogoHighPic());
//						brand.setAka(supplier.getNames().stream().colect(Collectors.toSet()));
					} else {
						if (null == brand.getLogo()) {
							// We update the logo if none was defined
							brand.setLogo(supplier.getLogoHighPic());
						}						
					}
					
					// Updating the brand
					brandService.saveBrand(brand);
				}
			
			
		} catch (Exception e) {
			LOGGER.error("Error while loading features", e);
		}
		 LOGGER.info("End loading of features from {}", iceCatConfig.getFeaturesListFileUri());
	}
	
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

	

	public void loadCategories() throws TechnicalException {

		// 1 - Download the file with basic auth

		// Unzip it
		if (null == iceCatConfig.getCategoriesListFileUri()) {
			LOGGER.error("No categories list file uri configured");
			return;
		}
		LOGGER.info("Getting file from {}", iceCatConfig.getCategoriesListFileUri());
		File icecatFile = getCachedFile(iceCatConfig.getCategoriesListFileUri(), iceCatConfig.getUser(), iceCatConfig.getPassword());

		try {
			List<IcecatCategory> categories = xmlMapper.readValue(icecatFile, IcecatModel.class).getResponse().getCategoryList().getCategories();

			categories.forEach(category -> {
//				Integer gTaxonomyId = null;
//				if (null != category.getNames()) {					
//					for (IcecatName name : category.getNames()) {
//						
//						
//						// TODO : Google taxo resolution test
//						 gTaxonomyId = googleTaxonomyService.resolve(name.getValue());
//						if (null != gTaxonomyId) {
//							LOGGER.info("Google taxonomy id {} resolved for category {}", gTaxonomyId, name.getValue());
//							break;
//						} 
//					}
//				}
//				
//				if (null != gTaxonomyId) {
//					LOGGER.info("Google taxonomy id {} resolved for category {}", gTaxonomyId, category.getNames());
//				} else {
//					LOGGER.warn("No Google taxonomy id resolved for category {}", category.getNames());
//
//				}

				categoriesById.put(category.getID(), category);
			});

		} catch (Exception e) {
			LOGGER.error("Error while loading categories", e);
		}
		LOGGER.info("End loading of categories from {}", iceCatConfig.getCategoriesListFileUri());
	}
	
	
	public void loadCategoryFeatureList() throws TechnicalException {
		
		// 1 - Download the file with basic auth
		
		// Unzip it
		if (null == iceCatConfig.getCategoryFeatureListFileUri()) {
			LOGGER.error("No category features list file uri configured");
			return;
		}
		
		
		
		
		
		LOGGER.info("Getting file from {}", iceCatConfig.getCategoryFeatureListFileUri());
		
		// Tweak, we cache directly the minified version (7Mb vs 7Gb ;) 
		
		File icecatMimified = new File(remoteCachingFolder+File.separator+IdHelper.getHashedName(iceCatConfig.getCategoryFeatureListFileUri()+".min"));
		
		if (!icecatMimified.exists()) {
			LOGGER.info("Minified file not found, Downloading and generating mimified version.. Can take a long time !");	
			File icecatFile = new File(remoteCachingFolder+File.separator+IdHelper.getHashedName(iceCatConfig.getCategoryFeatureListFileUri()));
			icecatFile = getCachedFile(iceCatConfig.getCategoryFeatureListFileUri(), iceCatConfig.getUser(), iceCatConfig.getPassword());
	
			LOGGER.info("Start generating mimified version");
			icecatMimified.delete();

// Starting file minification
			
			AtomicBoolean inMeasure= new AtomicBoolean(false);
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(icecatMimified, true));
				
				Files.lines(icecatFile.toPath())
				.forEach(l -> {
					try {
						
						if (l.contains("<Measure ")) {
							inMeasure.set(true);
						}
						
						if (!inMeasure.get()) {
							if (!l.contains("<Name") && ! l.contains("<RestrictedValue"))  {
								
								writer.write(l);
								writer.newLine();
							}
						}
						
						
						if (l.contains("</Measure")) {
							inMeasure.set(false);
						}
						
						
					} catch (IOException e) {
						LOGGER.error("Error writing line", e);
					}
				});
				
				LOGGER.info("End generating mimified version : {} ", icecatMimified.getAbsolutePath());
				
				LOGGER.info("Cleaning up the uncompressed file");
				//TODO
//			icecatFile.delete();
				IOUtils.closeQuietly(writer);
			} catch (IOException e) {
				LOGGER.error("Error writing file", e);
			}
		}
		
		///////////////////////////
		// Loading models 
		//////////////////////////
		
		
		
//		Category 
//		    FeatureGroup
//			Feature		
		try {
			LOGGER.info("DOM Parsing of {}", icecatMimified);
			List<IcecatCategory> features = xmlMapper.readValue(icecatMimified, IcecatModel.class).getResponse().getCategoryFeaturesList().getCategories();

			for (IcecatCategory category : features) {

				// The ID of the category
				int catId = category.getID().intValue();

//				// Updating the category with features
//				IcecatCategory fullCategory = categoriesById.get(catId);
//				fullCategory.setFeatures(category.getFeatures());
//				fullCategory.setCategoryFeatureGroups(category.getCategoryFeatureGroups());
//				
//				// TODO : maintain here the feature group by category
//				if (null != category.getFeatures()) {
//					for (IcecatFeature feature : category.getFeatures()) {
//						int categoryFeatureId = feature.getCategoryFeature_ID();
//						int categoryFeatureGroupId = feature.getCategoryFeatureGroup_ID();
//
//						FeatureGroup fg = new FeatureGroup();
//						fg.setCategoryFeatureId(categoryFeatureId);
//						fg.setCategoryFeatureGroupId(categoryFeatureGroupId);
//
//						featureGroupsByCategoryId.computeIfAbsent(catId, k -> new ArrayList<>()).add(fg);
//					}
//				}
				
				

				// Retriving the defined vertical if any
				VerticalConfig vertical = verticalsConfigService.getByIcecatCategoryId(catId);
				if (null != vertical) {
					// Update the nudger vertical
					updateVertical(category, vertical);
				}
				
				
			}
		} catch (Exception e) {
			LOGGER.error("Error while loading category features list", e);
		}
		LOGGER.info("End loading of features from {}", iceCatConfig.getFeaturesListFileUri());
	}
	

	/**
	 * Update the vertical with the category features from icecat
	 * 
	 * @param category
	 * @param vertical
	 */
	private void updateVertical(IcecatCategory category, VerticalConfig vertical) {

		Map<Integer,FeatureGroup> featureGroupById = new HashMap<>();
//		Map<Integer,FeatureGroup> featureGroupByInternalId = new HashMap<>();
		
		
		
		// TODO : We don't use the category feature group from icecat, see if relevant...
		if (null != category.getCategoryFeatureGroups()) {
			
			
			
			for (IcecatCategoryFeatureGroup cfg : category.getCategoryFeatureGroups()) {
				int cfgId = cfg.getID();
				
				
				for (IcecatFeatureGroup ifg : cfg.getFeatureGroups()) {
					// Setting up names
					FeatureGroup fg = vertical.getOrCreateByIceCatCategoryFeatureGroup(ifg.getID());
					
					// TODO : perf
					// TODO : real I18n
					List<IcecatName> names = featureGroupsById.get(ifg.getID()).getNames();
					
					IcecatName defName = names.stream()
							.filter(e -> e.getLangId() == 1).findFirst().orElse(null);
					
					if (null != defName) {
						fg.getName().put("default", defName.getValue());
					} else {
						LOGGER.warn("No default name found for feature group {}", fg);
					}
					
					// TODO : perf
					// TODO : real I18n
					IcecatName frName = names.stream()
							.filter(e -> e.getLangId() == 3).findFirst().orElse(null);
					
					if (null != defName) {
						fg.getName().put("fr", frName.getValue());
					} else {
						LOGGER.warn("No default name found for feature group {}", fg);
					}
					
					FeatureGroup tmpid = featureGroupById.get(cfgId);
					if (null != tmpid && tmpid.getIcecatCategoryFeatureGroupId() != fg.getIcecatCategoryFeatureGroupId()) {
						LOGGER.warn("Feature group {} already present in category feature group {}", ifg.getID(),
								cfgId);
					} else {
						featureGroupById.put (cfgId, fg);
					}
				}
				

				
				
				List<IcecatFeatureGroup> cfgGroupId = cfg.getFeatureGroups();
				for (IcecatFeatureGroup fg : cfgGroupId) {
					int fgId = fg.getID();
//					featureGroupById.put(cfgId, new FeatureGroup());
				}
				
				
			}
		}

		if (null != category.getFeatures()) {
			for (IcecatFeature feature : category.getFeatures()) {
				
//				IcecatFeature fullFeature = featuresById.get(Long.valueOf(feature.getID()));
				
				int categoryFeatureGroupId = feature.getCategoryFeatureGroup_ID();
//				int categoryFeatureId = feature.getCategoryFeature_ID();

				FeatureGroup fg = featureGroupById.get(categoryFeatureGroupId);
				
				

				
				
				if (null != fg) {
					// fg.setCategoryFeatureId(categoryFeatureId);
					//fg.setCategoryFeatureGroupId(categoryFeatureGroupId);
					
					
					Integer fId = Integer.valueOf(feature.getID());
					if (fg.getFeaturesId().contains(fId)) {
						LOGGER.warn("Feature {} already present in feature group {}", fId, fg);
					} else {
						fg.getFeaturesId().add(fId);
					}
					
				} else {
					LOGGER.warn("No feature group found for feature {}", feature);
				}


			}
		}
	}


	/**
	 * Resolve a feature name to one or more feature ID.
	 * @param featureName
	 * @return
	 */
	public Set<Integer> resolveFeatureName (String featureName) {		
		String f = normalize(featureName);
		return featuresByNames.get(f);		
	}



	private String normalize(String featureName) {
		if (StringUtils.isEmpty(featureName)) {
			return featureName;
		}
		return StringUtils.normalizeSpace(StringUtils.stripAccents(featureName)).toLowerCase();
	}
	
	
	

	/**
	 * Donload feature file, unzip it and maintain the cached version
	 * TODO : Should defer the "cahce" url <> file in the remotefilecachingservice
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
			// TODO : Have a refresh policy
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
	public String getFeatureName(Long featureID, String language) {
		
		IcecatFeature feature = featuresById.get(featureID);
		Integer icecatLanguage = getIceCatLangId(language);
		if (null != feature) {
			
			List<IcecatName> names = feature.getNames().getNames();
			for (IcecatName name : names) {
				if (name.getLangId() == icecatLanguage.intValue()) {
					return name.getValue();
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
					AggregatedAttribute a = product.getAttributes().attributeByFeatureId(fId);
					if (null != a) {
						ufg.getAttributes().add(a);
						// Updating attribute name
						IcecatFeature f = featuresById.get(fId);
						// TODO : Perf
						IcecatName i18nName = f.getNames().getNames().stream().filter(e->e.getLangId() == icecatLanguage).findFirst().orElse(null);
						if (null != i18nName) {
							a.setName(i18nName.getTextValue());
						}
						
						
						// Splitting on "," for multi values
						if (a.getValue().contains(",")) {
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
	
	
	
	/**
	 * Loads the list of features, aggegated by UiFeatureGroup
	 * @param vertical
	 * @param language
	 * @param product
	 * @return
	 */
	// TODO : Caching
	// of icecat stuff to open4goods attribute model
	public Map<String, String> types(VerticalConfig vertical) {
		Map<String,String> ret = new HashMap<>();
		
		// Initial building
		if (null != vertical) {
			for (FeatureGroup fg : vertical.getFeatureGroups()) {
				FeatureGroup ufg = new FeatureGroup();

				for (Integer fId : fg.getFeaturesId()) {
						// Updating attribute name
						IcecatFeature f = featuresById.get(fId);
						
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
	
	
	
}
