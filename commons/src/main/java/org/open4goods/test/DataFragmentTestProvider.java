package org.open4goods.test;

import java.util.UUID;

import org.open4goods.model.data.DataFragment;

public class DataFragmentTestProvider {




	public static  DataFragmentTestBuilder empty() {
		final DataFragmentTestBuilder p = new DataFragmentTestBuilder(new DataFragment());
		return p;
	}

	public static  DataFragmentTestBuilder defaulted() {
		final DataFragment pd = new DataFragment();
		pd.setLastIndexationDate(System.currentTimeMillis());
		pd.setUrl("http://perdu.com/"+UUID.randomUUID().toString());
		pd.setDatasourceName("defaultStore");
		pd.addProductTag("tag1");
		//		pd.setProviderType(ProviderType.AFFILIATED);
		final DataFragmentTestBuilder p = new DataFragmentTestBuilder(pd);



		return p;
	}




}
