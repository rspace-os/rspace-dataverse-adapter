package com.researchspace.dataverse.rspaceadapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
