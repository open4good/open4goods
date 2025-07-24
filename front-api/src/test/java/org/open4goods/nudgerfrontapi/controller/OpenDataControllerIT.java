package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.controller.api.OpenDataController;
import org.open4goods.nudgerfrontapi.dto.opendata.OpenDataMetaDto;
import org.open4goods.nudgerfrontapi.service.OpenDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "front.cache.path=${java.io.tmpdir}")
@AutoConfigureMockMvc
class OpenDataControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpenDataController controller;

    @MockBean
    private OpenDataService service;

    @Test
    void metaEndpointReturnsData() throws Exception {
        OpenDataMetaDto dto = new OpenDataMetaDto(1L, 2L, "1KB", "2KB", new Date(1L), new Date(2L));
        given(service.countGtin()).willReturn(dto.countGtin());
        given(service.countIsbn()).willReturn(dto.countIsbn());
        given(service.gtinFileSize()).willReturn(dto.gtinFileSize());
        given(service.isbnFileSize()).willReturn(dto.isbnFileSize());
        given(service.gtinLastUpdated()).willReturn(dto.gtinLastUpdated());
        given(service.isbnLastUpdated()).willReturn(dto.isbnLastUpdated());

        mockMvc.perform(get("/opendata/meta").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countGtin").value(1));
    }

    @Test
    void downloadEndpointReturnsFile() throws Exception {
        Resource res = new ByteArrayResource(new byte[] {1,2,3});
        given(service.gtinResource()).willReturn(res);

        mockMvc.perform(get("/opendata/gtin-open-data.zip").with(jwt()))
                .andExpect(status().isOk());
    }
}
