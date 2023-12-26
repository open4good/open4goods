package org.open4goods.services.ai;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiService {

	private Logger logger = LoggerFactory.getLogger(AiService.class);
	private NudgerAgent nudgerAgent;

	public AiService(NudgerAgent customerSupportAgent) {

		this.nudgerAgent = customerSupportAgent;
	
	}

	public String prompt(String value) {
		
		// TODO : log
		String ret = nudgerAgent.chat(value);
		logger.info("Response for {} is {}", value, ret);
		return ret;
	}

}
