package com.researchspace.dataverse.rspaceadapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;

import com.researchspace.dataverse.api.v1.DataverseAPI;
import com.researchspace.dataverse.http.DataverseAPIImpl;
import com.researchspace.repository.spi.RepositoryConfigurer;

/**
 * Wires up classes and produces Beans for this component.
 */
@Configuration
public class DataverseSpringConfig {
	
	@Bean
	@Scope(value="prototype")
	public DataverseAPI dataverseAPI(){
		return new DataverseAPIImpl();
	}
	
	@Bean(name="configurerDataverse")
	public RepositoryConfigurer configurer() {
		DataverseRepoConfigurer rc =  new DataverseRepoConfigurer();
		ClassPathResource subjects = new ClassPathResource("subjects.txt");
		rc.setResource(subjects);
		return rc;
	}
	
}
