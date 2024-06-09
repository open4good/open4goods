package org.open4goods.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
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
import org.open4goods.config.yml.IcecatConfiguration;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.data.Brand;
import org.open4goods.model.data.FeatureGroup;
import org.open4goods.model.icecat.IcecatCategory;
import org.open4goods.model.icecat.IcecatCategoryFeatureGroup;
import org.open4goods.model.icecat.IcecatFeature;
import org.open4goods.model.icecat.IcecatFeatureGroup;
import org.open4goods.model.icecat.IcecatLanguageHandler;
import org.open4goods.model.icecat.IcecatModel;
import org.open4goods.model.icecat.IcecatParentCategory;
import org.open4goods.model.icecat.IcecatSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import jakarta.annotation.PostConstruct;


/**
 * This service preloads some open4goods with (great) icecat data. 
 * Brands are injected in BrandService
 * FeatureGroups are injected in verticals
 * 
 *   It also provides endpoints to access icecat features, languages, and so on....
 * 
 */
public class IcecatService {

		private  Logger LOGGER = LoggerFactory.getLogger(IcecatService.class);
		
		
		private BrandService brandService;
		
		private XmlMapper xmlMapper;
		private IcecatConfiguration iceCatConfig;


		// For features
		private Map<Long, IcecatFeature> featuresById = new HashMap<>();
		private Map<String, Set<Long>> featuresByNames = new HashMap<>();
		
		
		// For language
		private Map<String, String> codeByLanguage;
		private Map<String, String> languageByCode;
		
		
		 // Créez un XMLReader
		private Map<Integer,List<FeatureGroup>> featureGroupsByCategoryId = new HashMap<>();
		private Map<Integer, IcecatCategory> categoriesById = new HashMap<>();
		
		
		private RemoteFileCachingService fileCachingService;
		private String remoteCachingFolder;
	
	public IcecatService(XmlMapper xmlMapper, IcecatConfiguration iceCatConfig, RemoteFileCachingService fileCachingService, String remoteCacheFolder, BrandService brandService) throws SAXException {
		super();
		this.xmlMapper = xmlMapper;
		this.iceCatConfig = iceCatConfig;
		this.fileCachingService = fileCachingService;
		this.remoteCachingFolder = remoteCacheFolder;
		this.brandService = brandService;

	}


	@PostConstruct
	public void icecatInit () throws TechnicalException {
		loadCategories();
		loadCategoryFeatureList();

		
		//		loadFeatures();
//		loadBrands();
//		loadLanguages();
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
				
				Long id = Long.valueOf(feature.getID());
				// Loading the by id map
				featuresById.put(id, feature);
				
				// Loading the by name map
				feature.getNames().getNames().forEach(name -> {
                  
					String val = normalize(name.getValue());
					Set<Long> fIds = featuresByNames.get(val);
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
			List<IcecatCategory> categories = xmlMapper.readValue(icecatFile, IcecatModel.class).getResponse()
					.getCategoryList().getCategories();

			categories.forEach(category -> {
				List<IcecatFeature> features = category.getFeatures();
				category.getCategoryFeatureGroups();
				category.getDescriptions();
				category.getID();
				category.getLowPic();
				categoriesById.put(category.getID(), category);

			});

		} catch (Exception e) {
			LOGGER.error("Error while loading categories", e);
		}
		LOGGER.info("End loading of categories from {}", iceCatConfig.getCategoriesListFileUri());
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
				
			 for (IcecatCategory category : features) 
			 {

				// The ID of the category
				 
				 Integer catId = category.getID();
				 IcecatCategory fullCategory = categoriesById.get(catId);
				 fullCategory.setFeatures(category.getFeatures());
				 fullCategory.setCategoryFeatureGroups(category.getCategoryFeatureGroups());
				
				for (IcecatFeature feature : category.getFeatures()) {
					IcecatFeature fullFeature = featuresById.get(Long.valueOf(feature.getID()));
					int categoryFeatureId =    feature.getCategoryFeature_ID();
					int categoryFeatureGroupId = feature.getCategoryFeatureGroup_ID();
					
					// Update the feature
					fullFeature.setCategoryFeature_ID(categoryFeatureId);
			
					FeatureGroup fg = new FeatureGroup();
					fg.setCategoryFeatureId(categoryFeatureId);
					fg.setCategoryFeatureGroupId(categoryFeatureGroupId);
					
					featureGroupsByCategoryId.computeIfAbsent(catId, k -> new ArrayList<>()).add(fg);
				 }
				 
//				 for (IcecatCategoryFeatureGroup catFeatGroup :  category.getCategoryFeatureGroups()) {
//						String fGroup = catFeatGroup.getFeatureGroup();
//						Integer fGroupId = catFeatGroup.getID();
////						catFeatGroup.feat
//						for (IcecatFeatureGroup featureGroup :  catFeatGroup.getFeatureGroups()) {
//							featureGroup.getID();
////							featureGroup.
////							for (IcecatFeature feature : featureGroup. )
//							featureGroup.getNames();
//						}
//				 }
			}
					
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
	public Set<Long> resolveFeatureName (String featureName) {		
		String f = normalize(featureName);
		return featuresByNames.get(f);		
	}



	private String normalize(String featureName) {
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
	
}
