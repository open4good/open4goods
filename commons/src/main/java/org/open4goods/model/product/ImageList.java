package org.open4goods.model.product;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.model.constants.ResourceType;
import org.open4goods.model.data.IndexedResource;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gof
 *
 */
public class ImageList {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ImageList.class.getName());

	public enum SizeFilterType {
		HEIGHT__EQUALS, HEIGHT_BIGGER, HEIGHT_SMALLER, WIDTH__EQUALS, WIDTH_BIGGER, WIDTH_SMALLER

	}

	private Set<IndexedResource> medias = new HashSet<>();

	public ImageList(final Set<IndexedResource> medias) {
		if (null == medias) {
			this.medias=new HashSet<>();
		} else {
			this.medias = medias.stream().filter(e -> e.getResourceType() == ResourceType.IMAGE).collect(Collectors.toSet());
		}
	}


	public boolean empty() {
		return 0 ==medias.size();
	}
	//TODO(perf) : add uidMap
	public IndexedResource smallest() {
		return medias == null ? null : medias.stream().sorted((o1, o2) -> o1.getImageInfo().getHeight() * o1.getImageInfo().getWidth() - o2.getImageInfo().getHeight() * o2.getImageInfo().getWidth() > 0 ? 0 : -1).findFirst().orElse(null);

	}

	//TODO(perf) : add uidMap
	public IndexedResource biggest() {
		return medias == null ? null : medias.stream().sorted((o1, o2) -> o1.getImageInfo().getHeight() * o1.getImageInfo().getWidth() - o2.getImageInfo().getHeight() * o2.getImageInfo().getWidth() > 0 ? -1 : 0).findFirst().orElse(null);

	}

	/**
	 * Filtering by containing tags (case insensitive)
	 * result is empty
	 *
	 *
	 *
	 * @return
	 */
	public ImageList filterByTags(final String tag) {
		return new ImageList(medias.stream().filter(e -> e.getTags() != null && e.getTags().contains(tag)).collect(Collectors.toSet()));
	}

	public ImageList preferByTags(final String tag) {
		final ImageList ret = filterByTags(tag);
		if (ret.empty()) {
			return new ImageList(medias);
		} else {
			return ret;
		}
	}

	/**
	 * Filtering the mediasthat are horizontal, returning all (no filter) if
	 * result is empty
	 *
	 *
	 *
	 * @return
	 */
	public ImageList filterBySizeOrAll(final int size, final SizeFilterType type) {
		return filterBySizeOr(size, type, medias);
	}

	/**
	 * Filtering the medias that are horizontal, returning all medias (no
	 * filter) if result is empty
	 *
	 *
	 *s
	 * @return
	 */

	public ImageList filterBySizeOr(final int size, final SizeFilterType type, final Set<IndexedResource> medias) {

		ImageList ret = null;
		switch (type) {

		case HEIGHT__EQUALS:
			ret = new ImageList(medias.stream().filter(e -> e.getImageInfo().getHeight() == size).collect(Collectors.toSet()));

		case WIDTH__EQUALS:
			ret = new ImageList(medias.stream().filter(e -> e.getImageInfo().getWidth() == size).collect(Collectors.toSet()));
		case WIDTH_BIGGER:
			ret = new ImageList(medias.stream().filter(e -> e.getImageInfo().getWidth() > size).collect(Collectors.toSet()));
		case HEIGHT_BIGGER:
			ret = new ImageList(medias.stream().filter(e -> e.getImageInfo().getHeight() > size).collect(Collectors.toSet()));
		case HEIGHT_SMALLER:
			ret = new ImageList(medias.stream().filter(e -> e.getImageInfo().getHeight() < size).collect(Collectors.toSet()));
		case WIDTH_SMALLER:
			ret = new ImageList(medias.stream().filter(e -> e.getImageInfo().getWidth() < size).collect(Collectors.toSet()));
		default:
			logger.warn("Unexpected SizeFilterType : " + type);

		}

		if (null == ret || null == ret.getMedias() || ret.getMedias().size() == 0) {
			return new ImageList(medias);
		}

		return ret;
	}

	/**
	 * Filtering the medias that are horizontal
	 *
	 *
	 * @return
	 */
	public ImageList filterByHorizontal() {
		return filterByHorizontalOr(medias);
	}

	/**
	 * Filtering the mediasthat are horizontal, returning all (no filter) if
	 * result is empty
	 *
	 *
	 *
	 * @return
	 */
	public ImageList filterByHorizontalOrAll() {
		return filterByHorizontalOr(medias);
	}

	/**
	 * Filtering the medias that are horizontal, returning all medias (no
	 * filter) if result is empty
	 *
	 *
	 *s
	 * @return
	 */

	public ImageList filterByHorizontalOr(final Set<IndexedResource> images) {
		final ImageList ret = new ImageList(
				medias.stream().filter(e -> e.getImageInfo().getWidth() > e.getImageInfo().getHeight()).collect(Collectors.toSet()));
		if (null == ret || null == ret.getMedias() || ret.getMedias().size() == 0) {
			return new ImageList(images);
		}
		return ret;
	}


	/**
	 * Removing a media
	 *
	 *
	 * @return
	 */
	public ImageList remove(final IndexedResource img) {
		medias.remove(img);
		return new ImageList(medias);
	}

	/**
	 * Filtering the medias that are Vertical
	 *
	 *
	 * @return
	 */
	public ImageList filterByVertical() {
		return filterByVerticalOr(new HashSet<>());
	}

	/**
	 * Filtering the medias that are vertical, returning all (no filter) if
	 * result is empty
	 *
	 *
	 *
	 * @return
	 */
	public ImageList filterByVerticalOrAll() {
		return filterByVerticalOr(medias);
	}

	/**
	 * Filtering the medias that are Vertical, returning all medias (no filter)
	 * if result is empty
	 *
	 *
	 *s
	 * @return
	 */

	public ImageList filterByVerticalOr(final Set<IndexedResource> images) {
		if (null == medias) {
			return new ImageList(images);
		}

		final ImageList ret = new ImageList(
				medias.stream().filter(e -> e.getImageInfo().getHeight() > e.getImageInfo().getWidth()).collect(Collectors.toSet()));
		if (null == ret || null == ret.getMedias() || ret.getMedias().size() == 0) {
			return new ImageList(images);
		}

		return ret;
	}

	//	/**
	//	 * Filtering the medias by language
	//	 *
	//	 *
	//	 * @return
	//	 */
	//	public ImageList filterByLang(final String lang) {
	//		return filterByLangOr(lang, null);
	//	}
	//
	//	/**
	//	 * Filtering the medias by language, returning all (no filter) if result is
	//	 * empty
	//	 *
	//	 *
	//	 *
	//	 * @return
	//	 */
	//	public ImageList filterByLangOrAll(final String lang) {
	//		return filterByLangOr(lang, medias);
	//	}

	/**
	 * Filtering the medias by language, returning all medias (no filter) if
	 * result is empty
	 *
	 *
	 *s
	 * @return
	 */
	//
	//	public ImageList filterByLangOr(final String lang, final Set<IndexedResource> images) {
	//
	//		final ImageList ret = new ImageList(
	//				medias == null ? null : medias .stream().filter(e -> lang.equalsIgnoreCase(e.getLanguageCode())).collect(Collectors.toSet()));
	//		if (null == ret || null == ret.getMedias() || ret.getMedias().size() == 0) {
	//			return new ImageList(images);
	//		}
	//
	//		return ret;
	//	}

	/**
	 * Terminal operation to get all Medias
	 *
	 * @return
	 */
	public Set<IndexedResource> all() {
		return medias;
	}

	/**
	 * Terminal operation to get one Medias
	 *
	 * @return
	 */
	public Optional<IndexedResource> one() {
		return medias == null ? Optional.empty() : medias.stream().findFirst();
	}

	public Set<IndexedResource> getMedias() {
		return medias;
	}

}