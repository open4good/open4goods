package org.open4goods.model.vertical;

public record BarcodeAggregationProperties(Integer qrCodeSize, Integer gtinSize, Integer datamatrixSize) {

        public BarcodeAggregationProperties() {
                this(300, 300, 600);
        }


        public Integer getQrCodeSize() {
                return qrCodeSize;
        }
        public Integer getGtinSize() {
                return gtinSize;
        }
        public Integer getDatamatrixSize() {
                return datamatrixSize;
        }







}
