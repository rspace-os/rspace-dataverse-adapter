package com.researchspace.dataverse.rspaceadapter;

import static com.researchspace.core.util.ZipUtils.createZip;
import static java.io.File.createTempFile;
import static java.util.Arrays.asList;
import java.util.ArrayList;
import java.util.Optional;

import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;

import java.io.File;
import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientException;

import com.researchspace.dataverse.api.v1.DataverseAPI;
import com.researchspace.dataverse.api.v1.DataverseConfig;
import com.researchspace.dataverse.entities.Dataset;
import com.researchspace.dataverse.entities.DataverseGet;
import com.researchspace.dataverse.entities.Identifier;
import com.researchspace.dataverse.entities.facade.DatasetAuthor;
import com.researchspace.dataverse.entities.facade.DatasetContact;
import com.researchspace.dataverse.entities.facade.DatasetDescription;
import com.researchspace.dataverse.entities.facade.DatasetKeyword;
import com.researchspace.dataverse.entities.facade.License;
import com.researchspace.dataverse.entities.facade.DatasetFacade;
import com.researchspace.dataverse.entities.facade.DatasetFacade.DatasetFacadeBuilder;
import com.researchspace.repository.spi.ExternalId;
import com.researchspace.repository.spi.IDepositor;
import com.researchspace.repository.spi.IRepository;
import com.researchspace.repository.spi.RepositoryConfig;
import com.researchspace.repository.spi.RepositoryConfigurer;
import com.researchspace.repository.spi.RepositoryOperationResult;
import com.researchspace.repository.spi.SubmissionMetadata;
import com.researchspace.repository.spi.ControlledVocabularyTerm;
import com.researchspace.zipprocessing.ArchiveIterator;
import com.researchspace.zipprocessing.ArchiveIteratorImpl;

import lombok.extern.slf4j.Slf4j;


/**
 * Connector to Dataverse API and implementing the IRepository interface for hooking into the archiving workflow.
 */
@Slf4j
public class DataverseRSpaceRepository implements IRepository {

    static final String ARCHIVE_RESOURCE_FOLDER = "/resources/";

	public DataverseRSpaceRepository() {
	}
	
	@Autowired
	private DataverseAPI dvAPI;
	
	private RepositoryConfigurer configurer;
	public void setConfigurer(RepositoryConfigurer configurer) {
		this.configurer = configurer;
	}
	@Override
	public RepositoryConfigurer getConfigurer() {
		return configurer;
	}
	
	
	public void setDvAPI(DataverseAPI dvAPI) {
		this.dvAPI = dvAPI;
	}

	private DataverseConfig cfg;

	@Override
	public RepositoryOperationResult submitDeposit(IDepositor depositor, File toDeposit,
			SubmissionMetadata metadata, RepositoryConfig repoCfg) {
		if(metadata.isPublish()) {
			throw new UnsupportedOperationException("Publishing deposit is not supported.");
		}
		DataverseConfig cfg = new DataverseConfig(repoCfg.getServerURL(), repoCfg.getIdentifier(),
				repoCfg.getRepositoryName());
		log.info("Uploading file {}, size {}", toDeposit.getAbsolutePath(), toDeposit.length());
		dvAPI.configure(cfg);
		DatasetFacade facade = buildDatasetToSubmit(metadata);
		facade.setDepositor(depositor.getUniqueName());
       
		try {
			Identifier createdDs = dvAPI.getDataverseOperations().createDataset(facade, cfg.getRepositoryName());
			Dataset ds = dvAPI.getDatasetOperations().getDataset(createdDs);
			doUpload(toDeposit, ds);
			return new RepositoryOperationResult(true, "Deposit succeeded.", createWebUrl(ds.getPersistentUrl(), repoCfg));
		} catch (RestClientException e) {
			log.error("Couldn't perform action {}", e.getMessage());
			return new RepositoryOperationResult(false, "Deposit failed: " + e.getMessage(), null);
		}  catch (IOException e) {
			log.error("Couldn't perform file upload {}", e.getMessage());
			return new RepositoryOperationResult(false, "Deposit failed due to IO error" + e.getMessage(), null);
		} catch (URISyntaxException e) {
			log.warn("Couldn't generate URI for deposit file upload {}", e.getMessage());
			return new RepositoryOperationResult(true, "Deposit failed due to IO error" + e.getMessage(), null);
		} 
	}
	
	private URL createWebUrl(URL persistentUrl, RepositoryConfig config) throws MalformedURLException, URISyntaxException {
		return URIUtils.persistentDoiToWebUrl(config.getServerURL(), persistentUrl);
	}
	
	//demo.dataverse.org/dataset.xhtml?persistentId=doi:10.5072/FK2/6RSCWM
	 void doUpload(File toDeposit, Dataset ds) throws IOException {
		if ("zip".equals(getExtension(toDeposit.getName()))) {
			File tempDoubleZip = null;
			log.info("Uploading main zip as single zip archive...");
			try {
			 tempDoubleZip = generateDoubleZip(toDeposit);
			} catch (Throwable t) {
				System.err.println(t.getMessage());
				t.printStackTrace();
			}
			dvAPI.getDatasetOperations().uploadFile(ds.getDoiId().get(), tempDoubleZip, ds.getProtocol());
		}
		else {
		    dvAPI.getDatasetOperations().uploadFile(ds.getDoiId().get(), toDeposit, ds.getProtocol());
		}
		
	
	}
	File generateDoubleZip(File toDeposit) throws IOException {
		ArchiveIterator it = new ArchiveIteratorImpl();
		File tempFolder = new File(getTempDirectory(), randomAlphabetic(10));
		tempFolder.mkdir();
		it.processZip(toDeposit, 
				file -> {
					try {
						copyFileToDirectory(file, tempFolder);
					} catch(IOException e) {
						e.printStackTrace();
						throw new IllegalStateException("IO exception while pre-processing export for upload to Dataverse.");
					}
				},
				entry -> !entry.getName().contains(ARCHIVE_RESOURCE_FOLDER));
		copyFileToDirectory(toDeposit, tempFolder);
		File tempDoubleZip = createTempFile(getBaseName(toDeposit.getName()), ".zip");
		createZip(tempDoubleZip, tempFolder.listFiles());
		return tempDoubleZip;
	}

	 DatasetFacade buildDatasetToSubmit(SubmissionMetadata metadata) {
		DatasetFacadeBuilder builder = DatasetFacade.builder();
		for (IDepositor author: metadata.getAuthors()) {
			builder.author(buildAuthor(author));
		}
		for (IDepositor contact: metadata.getContacts()) {
			builder.contact(buildContact(contact));
		}

		var keywords = new ArrayList<DatasetKeyword>();
		for(ControlledVocabularyTerm term : metadata.getTerms()) {
			keywords.add(DatasetKeyword
				.builder()
				.value(term.getValue())
				.vocabulary(term.getVocabulary())
				.vocabularyURI(term.getUri())
				.build());
		}

		// since there seems no way to set a license, this is ignored here.
		DatasetFacade facade = builder
		.title(metadata.getTitle())
		.subject(metadata.getSubjects().isEmpty()?"":metadata.getSubjects().get(0))
		.description(DatasetDescription.builder().description(metadata.getDescription()).build())
		.languages(asList(new String []{"English"}))
		.keywords(keywords)
		.build();
		Optional<URL> license = metadata.getLicense();
		Optional<String> licenseName = metadata.getLicenseName();
		if (license != null && licenseName != null && license.isPresent() && licenseName.isPresent()) {
			try {
				facade.setLicense(new License(licenseName.get(), license.get().toURI()));
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return facade;
	}

	private DatasetAuthor buildAuthor(IDepositor author) {
		ExternalId extId = getExtId(author);
		return DatasetAuthor.builder()
				.authorName(author.getUniqueName())
				.authorIdentifier((extId != null) ? extId.getIdentifier() : null)
				.authorIdentifierScheme((extId != null) ? extId.getScheme().name() : null)
				.build();
	}

	private ExternalId getExtId(IDepositor author) {
		if(!author.getExternalIds().isEmpty()) {
			return author.getExternalIds().get(0);
		}
		return null;
	}

	private DatasetContact buildContact(IDepositor contact) {		
		return DatasetContact.builder()
				 .datasetContactEmail(contact.getEmail())
				 .datasetContactName(contact.getUniqueName()).build();
	}

	@Override
	public void configure(RepositoryConfig repoCfg) {
		this.cfg = new DataverseConfig(repoCfg.getServerURL(), repoCfg.getIdentifier(),
				repoCfg.getRepositoryName());
	}

	@Override
	public RepositoryOperationResult testConnection() {
		try {
			dvAPI.configure(cfg);
			DataverseGet dv = dvAPI.getDataverseOperations().getDataverseById(cfg.getRepositoryName());
			if(dv != null ) {
				return new RepositoryOperationResult (true, "Test connection OK!", null);
			} else {
				return new RepositoryOperationResult (false, "Test connection failed - please check settings.", null);
			}
			
		} catch (RestClientException e) {
			log.error("Couldn't perform test action {}" + e.getMessage());
			return new RepositoryOperationResult(false, "Test connection failed - " + e.getMessage(), null);
		}
	}



}
