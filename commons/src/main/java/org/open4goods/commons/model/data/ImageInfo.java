package org.open4goods.commons.model.data;

public class ImageInfo {

	private Integer height;
	private Integer width;


	/**
	 * Hash value representation
	 * 
	 * Hashes are constructed by left shifting BigIntegers with either Zero or One
	 * depending on the condition found in the image. Preceding 0's will be
	 * truncated therefore it is the algorithms responsibility to add a 1 padding
	 * bit at the beginning new BigInteger("011011) new BigInteger("000101) 1xxxxx
	 * 
	 */
	private Long pHashValue;

	/**
	 * How many bits does this hash represent. Necessary due to suffix 0 bits
	 * beginning dropped.
	 */
	private int pHashLength;
	
	
	
	public ImageInfo() {
		super();
	}
	
	@Override
	public String toString() {
		return height+"*"+width ;
	}

	/**
	 * 
	 * @return the number of pixels this image contains
	 */
	public Integer pixels() {
		return height*width;
	}

	
	
	
	
	
	
	
	
	
	
	
	public Integer getHeight() {
		return height;
	}
	public void setHeight(final Integer height) {
		this.height = height;
	}
	public Integer getWidth() {
		return width;
	}
	public void setWidth(final Integer width) {
		this.width = width;
	}




	public int getpHashLength() {
		return pHashLength;
	}

	public void setpHashLength(int pHashLength) {
		this.pHashLength = pHashLength;
	}

	public Long getpHashValue() {
		return pHashValue;
	}

	public void setpHashValue(Long pHashValue) {
		this.pHashValue = pHashValue;
	}




}
