package com.researchspace.dataverse.rspaceadapter;

import static com.researchspace.core.util.TransformerUtils.toList;
import static com.researchspace.dataverse.rspaceadapter.DataverseRSpaceRepository.IGSN_INVENTORY_LINKED_ITEMS;
import static com.researchspace.dataverse.rspaceadapter.DataverseRSpaceRepository.RAID_METADATA_PROPERTY;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.directoryFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.fileFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.researchspace.core.util.ZipUtils;
import com.researchspace.dataverse.api.v1.DatasetOperations;
import com.researchspace.dataverse.api.v1.DataverseAPI;
import com.researchspace.dataverse.api.v1.DataverseOperations;
import com.researchspace.dataverse.entities.Dataset;
import com.researchspace.dataverse.entities.Identifier;
import com.researchspace.dataverse.entities.facade.DatasetAuthor;
import com.researchspace.dataverse.entities.facade.DatasetFacade;
import com.researchspace.repository.spi.ExternalId;
import com.researchspace.repository.spi.IDepositor;
import com.researchspace.repository.spi.IdentifierScheme;
import com.researchspace.repository.spi.RepositoryConfig;
import com.researchspace.repository.spi.RepositoryOperationResult;
import com.researchspace.repository.spi.SubmissionMetadata;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

// needed to support temp folder
@EnableRuleMigrationSupport
public class DataverseRSpaceRepositoryTest {

  private static final String ORCIDID = "orcidid1";
  protected static final String RAID_REFERENCE = "https://raid.org/10.12345/NICO26";
  protected static final String IGSN_ITEM_1 = "https://doi.org/10.82316/kfwc-xd82";
  protected static final String IGSN_ITEM_2 = "https://doi.org/10.82316/3fz7-mr43";
  DataverseRSpaceRepository adaptor;
  @Mock
  DataverseAPI api;
  @Mock
  DataverseOperations dvOps;
  @Mock
  DatasetOperations dsOps;
  @Mock
  IDepositor depositor;
  @Mock
  IDepositor author;
  public @Rule TemporaryFolder tempFolder = new TemporaryFolder();
  File file = null;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    adaptor = new DataverseRSpaceRepository();
    adaptor.setDvAPI(api);
    file = new File("src/test/resources/anyfile.doc");
  }

  @AfterEach
  public void tearDown() {
  }

  @Test
  public void testSubmitDeposit() throws MalformedURLException {
    SubmissionMetadata metaData = createAMetaDataWithOrcidId();
    RepositoryConfig repoCfg = createARepConfig();
    when(api.getDataverseOperations()).thenReturn(dvOps);
    when(dvOps.createDataset(Mockito.any(DatasetFacade.class), Mockito.anyString())).thenReturn(
        new Identifier());
    when(api.getDatasetOperations()).thenReturn(dsOps);
    Dataset ds = new Dataset();
    ds.setPersistentUrl(new URL("https://hdl.handle.net/21.T15999/DSDDEV/7ZYZT6"));

    when(dsOps.getDataset(Mockito.any(Identifier.class))).thenReturn(ds);
    RepositoryOperationResult result = adaptor.submitDeposit(depositor, file, metaData, repoCfg);
    assertThat(result.isSucceeded()).isTrue();
  }

  @Test
  public void buildDataset() {
    //check ORcid ID is set properly
    SubmissionMetadata metadata = createAMetaDataWithOrcidId();
    DatasetFacade facade = adaptor.buildDatasetToSubmit(metadata, "depositor");
    assertThat(facade.getAuthors())
        .extracting(DatasetAuthor::getAuthorIdentifier, DatasetAuthor::getAuthorIdentifierScheme)
        .containsExactly(tuple(ORCIDID, IdentifierScheme.ORCID.name()));
    assertEquals(RAID_REFERENCE, facade.getOtherReferences().get(0));
    assertEquals(IGSN_ITEM_1, facade.getRelatedMaterial().get(0));
    assertEquals(IGSN_ITEM_2, facade.getRelatedMaterial().get(1));
    assertEquals("depositor", facade.getDepositor());
  }

  private RepositoryConfig createARepConfig() throws MalformedURLException {
    return new RepositoryConfig(new URL("https://any.com"), "id", null, "repoName");
  }

  private SubmissionMetadata createAMetaDataWithOrcidId() {
    List<ExternalId> ids = new ArrayList<>();
    ids.add(new ExternalId(IdentifierScheme.ORCID, ORCIDID));
    SubmissionMetadata md = new SubmissionMetadata();
    when(author.getEmail()).thenReturn("email@somewhere.com");
    when(author.getUniqueName()).thenReturn("anyone");
    when(author.getExternalIds()).thenReturn(ids);

    md.setAuthors(List.of(author));
    md.setContacts(List.of(author));
    md.setDescription("desc");
    md.setPublish(false);
    md.setSubjects(toList("subject"));
    md.setTitle("title");

    Map<String, String> otherPropertiesMap = new HashMap<>();
    otherPropertiesMap.put(RAID_METADATA_PROPERTY, RAID_REFERENCE);
    otherPropertiesMap.put(IGSN_INVENTORY_LINKED_ITEMS, IGSN_ITEM_1 + "," + IGSN_ITEM_2);
    md.setOtherProperties(otherPropertiesMap);
    return md;
  }

  @Test
  public void testDoubleZip() throws Exception {
    File originalZip = new File("src/test/resources/HTMLExportWithAttachments.zip");
    File dblZip = adaptor.generateDoubleZip(originalZip);
    ZipUtils.extractZip(dblZip, tempFolder.getRoot());
    assertZipIsFlatListWithNoFolders();
    assertDoubleZipContainsInternalOriginalZip();
    final int expectedFileCountInOriginalExport = 10;
    assertEquals(expectedFileCountInOriginalExport,
        listFiles(tempFolder.getRoot(), fileFileFilter(), null).size());
  }

  private void assertDoubleZipContainsInternalOriginalZip() {
    assertEquals(1, listFiles(tempFolder.getRoot(), suffixFileFilter("zip"), null).size());
  }

  private void assertZipIsFlatListWithNoFolders() {
    assertEquals(0, listFiles(tempFolder.getRoot(), directoryFileFilter(), null).size());
  }

}
