package org.open4goods.services;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.open4goods.config.yml.TagCloudConfig;
import org.open4goods.model.data.Resource;
import org.open4goods.model.product.AggregatedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.CircleBackground;
import com.kennycason.kumo.font.scale.SqrtFontScalar;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import com.kennycason.kumo.palette.ColorPalette;

/**
 * This service is in charge of generating tagclouds. Through "grouped" mail
 * sending Through specific logging
 * 
 * @author Goulven.Furet
 *
 */
public class TagCloudService {

	private ResourceService resourceService;
	private TagCloudConfig config;

	public TagCloudService(ResourceService resourceService, TagCloudConfig config) {
		super();
		this.resourceService = resourceService;
		this.config = config;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(TagCloudService.class);

	public InputStream getImageStream(AggregatedData data) throws FileNotFoundException, IOException {

		File f = tagcloudImage(data, config, data.gtin());

		return IOUtils.toBufferedInputStream(new FileInputStream(f));
	}

	public File tagcloudImage(AggregatedData data, TagCloudConfig config, String gtin) {

		Resource r = new Resource();
		r.setCacheKey("tagcloud-" + gtin + ".png");
		File f = resourceService.getCacheFile(r);

		if (f.exists()) {
			return f;
		}

		List<String> tokens = data.tagCloudTokens();

		final List<WordFrequency> wordFrequencies = analysefrequency(tokens, config);

		final Dimension dimension = new Dimension(config.getWidth(), config.getHeight());
		final WordCloud wordCloud = new WordCloud(dimension, config.getCollisionMode());
		wordCloud.setPadding(config.getPadding());

//		wordCloud.setBackground(new PixelBoundryBackground("backgrounds/whale_small.png"));
		// TODO(gof) : from conf
		wordCloud.setBackground(new CircleBackground(250));
		wordCloud.setBackgroundColor(Color.WHITE);
//		wordCloud.setColorPalette(new ColorPalette(new Color(0x4055F1), new Color(0x408DF1), new Color(0x40AAF1), new Color(0x40C5F1), new Color(0x40D3F1), new Color(0xFFFFFF)));
		wordCloud.setColorPalette(new ColorPalette(new Color(0x4055F1), new Color(0x408DF1), new Color(0x40AAF1), new Color(0x40C5F1), new Color(0x40D3F1), new Color(0xFFFFFF)));

		wordCloud.setFontScalar(new SqrtFontScalar(10, 40));
		
		wordCloud.build(wordFrequencies);

		wordCloud.writeToFile(f.getAbsolutePath());
		return new File(f.getAbsolutePath());

	}

	private List<WordFrequency> analysefrequency(List<String> tokens, TagCloudConfig config) {
		// Configuring frequency analyser
		final FrequencyAnalyzer frequencyAnalyzer = new FrequencyAnalyzer();
		frequencyAnalyzer.setWordFrequenciesToReturn(config.getWordFrequenciesToReturn());
		frequencyAnalyzer.setMinWordLength(config.getMinWordLength());
		frequencyAnalyzer.setMaxWordLength(config.getMaxWordLength());

		final List<WordFrequency> wordFrequencies = frequencyAnalyzer.load(tokens);
		return wordFrequencies;
	}

}
