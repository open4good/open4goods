package org.open4goods.config.yml.test;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.constants.ProductCondition;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.RatingType;

public class TestUrl {

	protected String url;

	protected String affiliatedUrl;

	private String category ;
	private AttributesExpectedResult attributes;


	private PriceExpectedResult price;
	private NamesExpectedResult names;
	private NumericExpectedResult alias;




	private ResourcesExpectedResult resources;
	private DescriptionsExpectedResult descriptions;
	private Map<RatingType, RatingsExpectedResult> ratings;

	private CommentsExpectedResult comments;
	private ProsAndConsExpectedResult pros;
	private ProsAndConsExpectedResult cons;
	private QuestionsExpectedResult questions;
	private SellerExpectedResult seller;


	private ProductCondition productState;
	private NumericExpectedResult shippingCost;
	private NumericExpectedResult shippingTime;
	private NumericExpectedResult warranty;






	public TestResultReport test(final DataFragment data, final String datasourceConfName) {
		final TestResultReport  ret = new TestResultReport (data, datasourceConfName);
		ret.setUrl(data.getUrl());



		// Shipping cost
		if (null != shippingCost) {
			shippingCost.testDouble(data.getShippingCost(), "shipping_cost", ret);
		}

		// Shipping cost
		if (null != shippingTime) {
			shippingTime.testDouble(  data.getShippingTime() == null ? null : data.getShippingTime().doubleValue(), "shipping_time", ret);
		}

		// Shipping cost
		if (null != warranty) {
			warranty.testDouble(   data.getWarranty() == null ? null : data.getWarranty().doubleValue(), "warranty", ret);
		}



		// product state
		if (null != productState) {
			if (null == data.getProductState()) {
				ret.addMessage("Was expecting a productState");
			} else
				if (productState != data.getProductState()) {
					ret.addMessage("Was expecting productState " + productState + ", we have : " + data.getProductState());
				}
		}




		// Affiliated url

		if (!StringUtils.isEmpty(affiliatedUrl)) {
			if (!affiliatedUrl.equals(data.getAffiliatedUrl())) {
				ret.addMessage("Was expecting affiliatedUrl " + affiliatedUrl+ ", we have : " + data.getAffiliatedUrl());
			}
		}

		if (null != category) {
			if (org.apache.commons.lang3.StringUtils.isEmpty(category)) {
				ret.addMessage("Was expecting a productTag");
			} else
				if (!category.equals(data.getCategory())) {
					ret.addMessage("Was expecting productTag " + category + ", we have : " + data.getCategory());
				}
		}

		if (null != attributes) {
			if (null != attributes.getReferentiel()) {
				attributes.test(data.getReferentielAttributes(), data,ret);
			}


		}


		if (null != names) {
			names.test(data.getNames(),ret);
		}

		if (null != alias) {
			alias.testCollection(data.getAlternateIds(),"aliases", ret);
		}

		if (null != price) {
			price.test(data.getPrice(),ret, data.getInStock());
		}

		if (null != descriptions) {
			descriptions.test(data.getDescriptions(),ret);
		}


		if (null != resources) {
			resources.test(data.getResources(),ret);
		}

		if (null != pros) {
			pros.test("pros",data.getPros(),ret);
		}

		if (null != cons) {
			cons.test("cons",data.getCons(),ret);
		}

		if (null != comments)
		{
			comments.test(data.getComments(),ret);
			//
		}

		//		if (null != ratings) {
		//			for (final Entry<RatingType, RatingsExpectedResult> ratingsEs : ratings.entrySet()) {
		//				if (null == data.ratings(ratingsEs.getKey().toString())) {
		//					ret.addMessage("Was expecting "+ratingsEs.getKey()+"ratings");
		//				} else {
		//					if (null == ratingsEs.getValue()) {
		//						final Rating tmp = data.rating(ratingsEs.getKey());
		//						if (null == tmp ) {
		//							ret.addMessage("No "+ratingsEs.getKey()+" ratings ");
		//						}
		//					} else {
		//						ratingsEs.getValue().test(data.rating(ratingsEs.getKey()),ratingsEs.getKey(),ret);
		//					}
		//				}
		//			}
		//		}


		if (null != questions) {
			questions.test(data.getQuestions(),ret);
		}


		//
		//		if (null != comments)
		//		comments.test(data.getComments());
		//
		//
		//		if (null != pros)
		//		pros.test(data.getPros());
		//
		//		if (null != cons)
		//			cons.test(data.getCons());
		//
		//
		//		if (null != questions)
		//		questions.test(data.getQuestions());
		//
		//
		//		if (null != seller)
		//		seller.test(data.getSeller());



		return ret;




	}


	@Override
	public int hashCode() {
		return url.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof TestUrl)) {
			return false;
		}

		return url.equals(((TestUrl)obj).getUrl());


	}

	public AttributesExpectedResult getAttributes() {
		return attributes;
	}




	public void setAttributes(final AttributesExpectedResult attributes) {
		this.attributes = attributes;
	}




	public PriceExpectedResult getPrice() {
		return price;
	}




	public void setPrice(final PriceExpectedResult price) {
		this.price = price;
	}




	public DescriptionsExpectedResult getDescriptions() {
		return descriptions;
	}




	public void setDescriptions(final DescriptionsExpectedResult descriptions) {
		this.descriptions = descriptions;
	}










	public String getCategory() {
		return category;
	}




	public void setCategory(final String tags) {
		category = tags;
	}




	public NamesExpectedResult getNames() {
		return names;
	}




	public void setNames(final NamesExpectedResult names) {
		this.names = names;
	}




	public ResourcesExpectedResult getResources() {
		return resources;
	}




	public void setResources(final ResourcesExpectedResult resources) {
		this.resources = resources;
	}

	//
	//
	//
	//	public Map<RatingType, RatingsExpectedResult> getRatings() {
	//		return ratings;
	//	}
	//
	//
	//
	//
	//	public void setRatings(final Map<RatingType, RatingsExpectedResult> ratings) {
	//		this.ratings = ratings;
	//	}




	public ProsAndConsExpectedResult getPros() {
		return pros;
	}




	public void setPros(final ProsAndConsExpectedResult pros) {
		this.pros = pros;
	}




	public ProsAndConsExpectedResult getCons() {
		return cons;
	}




	public void setCons(final ProsAndConsExpectedResult cons) {
		this.cons = cons;
	}




	public QuestionsExpectedResult getQuestions() {
		return questions;
	}




	public void setQuestions(final QuestionsExpectedResult questions) {
		this.questions = questions;
	}




	public SellerExpectedResult getSeller() {
		return seller;
	}




	public void setSeller(final SellerExpectedResult seller) {
		this.seller = seller;
	}




	public CommentsExpectedResult getComments() {
		return comments;
	}




	public void setComments(final CommentsExpectedResult comments) {
		this.comments = comments;
	}




	public String getUrl() {
		return url;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	public String getAffiliatedUrl() {
		return affiliatedUrl;
	}

	public void setAffiliatedUrl(final String affiliatedUrl) {
		this.affiliatedUrl = affiliatedUrl;
	}

	public NumericExpectedResult getShippingCost() {
		return shippingCost;
	}

	public void setShippingCost(final NumericExpectedResult shippingCost) {
		this.shippingCost = shippingCost;
	}

	public NumericExpectedResult getShippingTime() {
		return shippingTime;
	}

	public void setShippingTime(final NumericExpectedResult shippingTime) {
		this.shippingTime = shippingTime;
	}

	public NumericExpectedResult getWarranty() {
		return warranty;
	}

	public void setWarranty(final NumericExpectedResult warranty) {
		this.warranty = warranty;
	}

	public ProductCondition getProductState() {
		return productState;
	}

	public void setProductState(final ProductCondition productState) {
		this.productState = productState;
	}

	public void setAlias(final NumericExpectedResult alias) {
		this.alias = alias;
	}







}
