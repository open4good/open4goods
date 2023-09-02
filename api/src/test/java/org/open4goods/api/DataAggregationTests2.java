package org.open4goods.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.cxf.jaxrs.model.UserResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.open4goods.api.Api;
import org.open4goods.api.config.ApiConfig;
import org.open4goods.dao.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@SpringBootTest(
  classes = ProductRepository.class)
//@AutoConfigureMockMvc
//@TestPropertySource(
//  locations = "classpath:application-devsec.yml")

@ActiveProfiles(value = "devsec")
public class DataAggregationTests2 {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ProductRepository produ;

  @Test
  public void registrationWorksThroughAllLayers() throws Exception {
    
	  produ.exportAll();
	  UserResource user = new UserResource("Zaphod", "zaphod@galaxy.net");

    mockMvc.perform(post("/forums/{forumId}/register", 42L)
            .contentType("application/json")
            .param("sendWelcomeMail", "true")
            .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isOk());

  }

}