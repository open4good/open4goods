package org.open4goods.config.yml;

import com.kennycason.kumo.CollisionMode;

public class TagCloudConfig {

	private int wordFrequenciesToReturn = 100;

	private int maxWordLength = 200;

	private int minWordLength = 2;	
		
	private CollisionMode collisionMode = CollisionMode.PIXEL_PERFECT;
	
	private int padding = 5;
	
	
	
    /**
     * The width dimension; negative values can be used.
     *
     * @serial
     * @see #getSize
     * @see #setSize
     * @since 1.0
     */
    public int width = 500;

    /**
     * The height dimension; negative values can be used.
     *
     * @serial
     * @see #getSize
     * @see #setSize
     * @since 1.0
     */
    public int height = 500;
    
    

	public int getWordFrequenciesToReturn() {
		return wordFrequenciesToReturn;
	}

	public void setWordFrequenciesToReturn(int wordFrequenciesToReturn) {
		this.wordFrequenciesToReturn = wordFrequenciesToReturn;
	}

	public int getMaxWordLength() {
		return maxWordLength;
	}

	public void setMaxWordLength(int maxWordLength) {
		this.maxWordLength = maxWordLength;
	}

	public int getMinWordLength() {
		return minWordLength;
	}

	public void setMinWordLength(int minWordLength) {
		this.minWordLength = minWordLength;
	}

	public CollisionMode getCollisionMode() {
		return collisionMode;
	}

	public void setCollisionMode(CollisionMode collisionMode) {
		this.collisionMode = collisionMode;
	}

	public int getPadding() {
		return padding;
	}

	public void setPadding(int padding) {
		this.padding = padding;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	
	
	
	
}
