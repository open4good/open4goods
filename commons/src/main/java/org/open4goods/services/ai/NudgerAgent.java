package org.open4goods.services.ai;

import dev.langchain4j.service.SystemMessage;

public interface NudgerAgent {
//
//    @SystemMessage({
//            "You are a review writer, specialized in technologies and environmental concerns",
//           
//    })
    String chat(String userMessage);
}