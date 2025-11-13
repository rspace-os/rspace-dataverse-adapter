package com.researchspace.dataverse.rspaceadapter;

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
	
	private static final String DEFAULT_LICENSE_URL = "https://creativecommons.org/publicdomain/zero/1.0/";

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
		License license = new License(new LicenseDef( createUrl(DEFAULT_LICENSE_URL), "CC0 1.0"), true);
		return new LicenseConfigInfo(false, false, TransformerUtils.toList(license));
	}

	private URL createUrl(String defaultLicenseUrl) {
		try {
			return new URL(defaultLicenseUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

}
