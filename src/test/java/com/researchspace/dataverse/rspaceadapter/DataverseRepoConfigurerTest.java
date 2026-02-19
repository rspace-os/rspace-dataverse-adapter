package com.researchspace.dataverse.rspaceadapter;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.researchspace.dataverse.entities.Dataset;
import java.io.IOException;
import java.net.URL;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.researchspace.repository.spi.RepositoryConfigurer;

@DataverseSpringTest
public class DataverseRepoConfigurerTest extends AbstractJUnit4SpringContextTests {
	
	@Autowired
	private RepositoryConfigurer configurer;

	@Test
	public void testGetSubjects() {
		assertEquals(14, configurer.getSubjects().size());
	}
	
	@Test
	public void testGetLicenses(){
		assertEquals(2, configurer.getLicenseConfigInfo().getLicenses().size());
	}

	@Test
	public void testJacksonBehaviour() throws IOException {
		Dataset dataverseDataset = new Dataset();
		dataverseDataset.setId(1L);
		    dataverseDataset.setPersistentUrl(new URL("http://localhost:8080/api/datasets/1/versions/1.1"));
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new Jdk8Module());
		String json = "";
		try {
			json = objectMapper.writeValueAsString(dataverseDataset);
			System.out.println(json);
		} catch (JsonProcessingException e) {
			fail(e.getMessage());
		}
	}

}
