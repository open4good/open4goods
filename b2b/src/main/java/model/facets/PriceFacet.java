package model.facets;

public class PriceFacet extends AbstractFacet{

	private Float price;
	private String currency;


	public float getPrice() {
		return price;
	}
	public void setPrice(float price) {
		this.price = price;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}



}
