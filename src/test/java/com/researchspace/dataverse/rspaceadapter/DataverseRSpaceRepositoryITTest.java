package com.researchspace.dataverse.rspaceadapter;

import static com.researchspace.core.util.TransformerUtils.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.researchspace.repository.spi.LicenseDefs;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.researchspace.dataverse.api.v1.DataverseAPI;
import com.researchspace.repository.spi.ExternalId;
import com.researchspace.repository.spi.IDepositor;
import com.researchspace.repository.spi.RepositoryConfig;
import com.researchspace.repository.spi.RepositoryOperationResult;
import com.researchspace.repository.spi.SubmissionMetadata;
import com.researchspace.repository.spi.ControlledVocabularyTerm;

@DataverseSpringTest
public class DataverseRSpaceRepositoryITTest extends AbstractJUnit4SpringContextTests {
	final String validSubject = "Law";
	
	private @Autowired DataverseAPI dvAPI;
	private @Autowired DataverseRepoConfigurer configurer;
	DataverseRSpaceRepository adapter;
	
	// defaults from test.properties
	@Value("${dataverseAlias}")
	protected String dataverseAlias;
	@Value("${dataverseServerURL}")
	protected String serverURL;
	@Value("${dataverseApiKey}")
	protected String apiKey;
	File toDeposit = new File("src/test/resources/anyfile.doc");

	@Before
	public void setUp() {
		adapter = new DataverseRSpaceRepository();
		adapter.setDvAPI(dvAPI);
		adapter.setConfigurer(configurer);
	}

	@After
	public void tearDown() {
	}

	@Ignore("requires passing dataverseApiKey in test.properties")
	@Test
	public void testConnection() throws MalformedURLException {
		RepositoryConfig cfg = createRepoConnectionCfg();
		adapter.configure(cfg);
		assertTrue(adapter.testConnection().isSucceeded());
	}

	private RepositoryConfig createRepoConnectionCfg() throws MalformedURLException {
		return new RepositoryConfig(new URL(serverURL), apiKey, null, dataverseAlias);
	}

	@lombok.Value
	static class Depositor implements IDepositor {
		String email, uniqueName;
		List<ExternalId> externalIds;
	}

	@Ignore("requires passing dataverseApiKey in test.properties")
	@Test
	public void testDeposit() throws MalformedURLException, URISyntaxException {
		RepositoryConfig cfg = createRepoConnectionCfg();

		SubmissionMetadata md = new SubmissionMetadata();
		IDepositor auth = new Depositor("a@b.com", "any", Collections.emptyList());
		md.setAuthors(toList(auth));
		md.setContacts(toList(auth));
		md.setDescription("desc (from DataverseRSpaceRepositoryITTest)");
		md.setPublish(false);
		md.setTitle("Title");

		md.setSubjects(toList(validSubject));

		md.setTerms(toList(ControlledVocabularyTerm.builder().value("foo").vocabulary("bar").uri(new URI("https://www.example.com/foo")).build()));
		//if Hungarian option is supported in dataverse metadata languages, uncomment line below
		//md.addProperty("metadataLanguage", "hu");
		md.setLicense(Optional.of(LicenseDefs.CC_0.getUrl()));
		md.setLicenseName(Optional.of(LicenseDefs.CC_0.getName()));
		RepositoryOperationResult result = adapter.submitDeposit(auth, toDeposit, md, cfg);
		assertTrue(result.isSucceeded());
	}

	@Test
	public void testGetSubjects() {
		assertEquals(14, adapter.getConfigurer().getSubjects().size());
	}

	@Test
	public void testConnectionFailure() throws MalformedURLException {
		RepositoryConfig cfg = new RepositoryConfig(new URL(serverURL), apiKey, null, "unknownRepo");
		adapter.configure(cfg);
		assertFalse(adapter.testConnection().isSucceeded());
	}

}
