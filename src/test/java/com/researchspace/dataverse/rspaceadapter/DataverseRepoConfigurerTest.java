package com.researchspace.dataverse.rspaceadapter;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(configurer.getSubjects()).size().isEqualTo(14);
	}
	
	@Test
	public void testGetLicenses(){
		assertThat(configurer.getLicenseConfigInfo().getLicenses()).size().isEqualTo(1);
	}

}
