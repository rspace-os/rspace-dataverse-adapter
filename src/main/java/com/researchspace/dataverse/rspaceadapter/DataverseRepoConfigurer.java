package com.researchspace.dataverse.rspaceadapter;

import static com.researchspace.repository.spi.LicenseDefs.NO_LICENSE_URL;

import com.researchspace.repository.spi.LicenseDefs;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import com.researchspace.core.util.TransformerUtils;
import com.researchspace.repository.spi.License;
import com.researchspace.repository.spi.LicenseConfigInfo;
import com.researchspace.repository.spi.LicenseDef;
import com.researchspace.repository.spi.RepositoryConfigurer;
import com.researchspace.repository.spi.Subject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataverseRepoConfigurer implements RepositoryConfigurer {

	private List<Subject> subjects = new ArrayList<>();
	
	private Resource resource;
	
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	@PostConstruct
	void init() {
		try {
			var lines = IOUtils.readLines(resource.getInputStream(), Charset.defaultCharset());
			lines.forEach(line->{
				subjects.add(new Subject(line));
			});
		} catch (IOException e) {
			log.warn("Couldn't load subjects, returning empty list");
		}
	}
	@Override
	public List<Subject> getSubjects() {
		return Collections.unmodifiableList(subjects);		
	}
	
	@Override
	public LicenseConfigInfo getLicenseConfigInfo() {
		License customLicense = new License(new LicenseDef(createUrl(NO_LICENSE_URL), "Custom Dataset Terms"), true);
		// 'CC0 1.0' license is not necessarily allowed for given dataset, that's why default is no license
		License cc0License = new License(LicenseDefs.CC_0, false);
		return new LicenseConfigInfo(false, false, List.of(customLicense, cc0License));
	}

	private URL createUrl(String urlString) {
		try {
			return new URL(urlString);
		} catch (MalformedURLException e) {
			log.warn("MalformedURLException for license url: " + urlString);
			return null;
		}
	}

}
