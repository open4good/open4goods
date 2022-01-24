package org.open4goods.ui.controllers.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.helper.IdHelper;
import org.open4goods.services.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.talanlabs.avatargenerator.Avatar;
import com.talanlabs.avatargenerator.IdenticonAvatar;
import com.talanlabs.avatargenerator.cat.CatAvatar;
import com.talanlabs.avatargenerator.eightbit.EightBitAvatar;
import com.talanlabs.avatargenerator.layers.backgrounds.RandomColorPaintBackgroundLayer;
import com.talanlabs.avatargenerator.layers.masks.RoundRectMaskLayer;
import com.talanlabs.avatargenerator.layers.others.ShadowLayer;
import com.talanlabs.avatargenerator.smiley.SmileyAvatar;

@RestController
/**
 * Generate avatar images
 * @author goulven
 * TODO(design,P2,0.25) : should make an AvatarService
 */
public class AvatarController {

	private static final Logger logger = LoggerFactory.getLogger(AvatarController.class);

	RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

	@Autowired
	private ResourceService resourceService;
	
	private final List<Avatar> avatarBuilders = new ArrayList<>();


	public AvatarController () {


//		avatarBuilders.add(GitHubAvatar.newAvatarBuilder().layers(new ColorPaintBackgroundLayer(java.awt.Color.WHITE)).build());
//		avatarBuilders.add(TriangleAvatar.newAvatarBuilder().build());
//		avatarBuilders.add(SquareAvatar.newAvatarBuilder().build());
		avatarBuilders.add(IdenticonAvatar.newAvatarBuilder().build());
//		avatarBuilders.add(GitHubAvatar.newAvatarBuilder().build());
		avatarBuilders.add(CatAvatar.newAvatarBuilder()
			    .layers(new ShadowLayer(), new RandomColorPaintBackgroundLayer(), new RoundRectMaskLayer())
			    .padding(8).margin(8).build());
		avatarBuilders.add(SmileyAvatar.newAccessoriesAvatarBuilder().build());
		avatarBuilders.add(SmileyAvatar.newEyeMouthAvatarBuilder().build());
		avatarBuilders.add(SmileyAvatar.newGhostAvatarBuilder().build());
		avatarBuilders.add(SmileyAvatar.newDefaultAvatarBuilder().build());
		avatarBuilders.add(EightBitAvatar.newMaleAvatarBuilder().build());
		avatarBuilders.add(EightBitAvatar.newFemaleAvatarBuilder().build());
	}


	@GetMapping("/avatar/{key}.png")
	public void avatar(@PathVariable  String key, final HttpServletResponse response)
			throws ResourceNotFoundException, IOException {

		key = IdHelper.azCharAndDigits(key.replace("é", "e"));

		response.setContentType("image/png");

		File file = null;

		file = createPngFile(key);


		{
			try (InputStream str = org.apache.commons.io.FileUtils.openInputStream(file)){				
				IOUtils.copy(str, response.getOutputStream());
			} catch (final IOException e) {
				logger.error("Error rendering avatar file  {} : {}", file.getAbsolutePath(), e.getMessage());
				throw new ResourceNotFoundException(e.getMessage());
			}

			// Adding cache control headers
			response.setHeader("Cache-Control", "public, max-age=31536000");
			response.addDateHeader("Last-Modified", runtimeBean.getStartTime());

		}
	}

	public File createPngFile(final String key) throws ResourceNotFoundException {
		File file = null;
		try {
			file = resourceService.getCacheFile(key);
		} catch (final ValidationException e1) {
			throw new ResourceNotFoundException(key + " is not found");
		}

		// If file exists, serving it
		if (!file.exists()) {
			createAsPngToFile(key.hashCode(), file);
		}
		return file;
	}

	private void createAsPngToFile(final int key, final File file) {
	    final Random rand = new Random();
	    final Avatar avatar = avatarBuilders.get(rand.nextInt(avatarBuilders.size()));
	    try {
			avatar.createAsPngToFile(key, file);
		} catch (final Exception e) {
			logger.error("Cannot create PNG Avatar for user {} : {}",key,e.getMessage());
		}
	}
}
