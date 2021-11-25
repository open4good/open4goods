package org.open4goods.config.yml.ui;

public class BarcodeAggregationProperties {

	private Integer qrCodeSize = 300;
	private Integer gtinSize = 300;
	private Integer datamatrixSize = 600;


	public Integer getQrCodeSize() {
		return qrCodeSize;
	}
	public void setQrCodeSize(final Integer qrCodeSize) {
		this.qrCodeSize = qrCodeSize;
	}
	public Integer getGtinSize() {
		return gtinSize;
	}
	public void setGtinSize(final Integer gtinSize) {
		this.gtinSize = gtinSize;
	}
	public Integer getDatamatrixSize() {
		return datamatrixSize;
	}
	public void setDatamatrixSize(final Integer datamatrixSize) {
		this.datamatrixSize = datamatrixSize;
	}







}
