package com.researchspace.dataverse.rspaceadapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class URIUtilsTest {

	private static final String EXPECTED_DATASET_URL = "https://demo.dataverse.org/dataset.xhtml?persistentId=doi:10.5072/FK2/X2GWUE";
	private static final String PERSISTENT_DOI = "https://dx.doi.org/10.5072/FK2/X2GWUE";

	@Test
	@DisplayName("Converts DOI to a web URL")
	public void persistentDoiToWebUrl() throws MalformedURLException, URISyntaxException {
		URL doi = new URL(PERSISTENT_DOI);
		URL server = new URL("https://demo.dataverse.org");
		URL converted = URIUtils.persistentIDToWebUrl(server, doi, "doi");
		assertEquals(EXPECTED_DATASET_URL, converted.toString());
	}

	@Test
	public void handleToWebUrl() throws MalformedURLException, URISyntaxException {
		URL hdl = new URL("https://hdl.handle.net/21.T15999/DSDDEV/7ZYZT6");
		URL server = new URL("https://demo.dataverse.org");
		URL converted = URIUtils.persistentIDToWebUrl(server, hdl, "hdl");
		assertEquals("https://demo.dataverse.org/dataset.xhtml?persistentId=hdl:21.T15999/DSDDEV/7ZYZT6",
				converted.toString());
	}

}
