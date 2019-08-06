package fr.insee.semweb.bpe.test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import fr.insee.semweb.bpe.CodelistModelMaker;
import fr.insee.semweb.bpe.Configuration;

public class CodelistModelMakerTest {

	@Test
	public void testEquipmentTypesCodelist() throws Exception {

		Model codeList = CodelistModelMaker.makeEquipmentTypesCodelistModel();
		codeList.write(new FileWriter("src/main/resources/data/cl-typequ.ttl"), "TTL");
	}

	@Test
	public void testFeaturesCodelist() throws Exception {

		Model codeList = CodelistModelMaker.makeFeaturesCodelistModel();
		codeList.write(new FileWriter("src/main/resources/data/cl-caract.ttl"), "TTL");
	}

	@Test
	public void testSectorsCodelist() throws Exception {

		Model codeList = CodelistModelMaker.makeSectorsCodelistModel();
		codeList.write(new FileWriter("src/main/resources/data/cl-sect.ttl"), "TTL");
	}

	@Test
	public void testQualityLevelsCodelist() throws Exception {

		Model codeList = CodelistModelMaker.makeQualityLevelsCodelistModel();
		codeList.write(new FileWriter("src/main/resources/data/cl-qual.ttl"), "TTL");
	}

	@Test
	public void testGetFeatureList() throws Exception {

		System.out.println(CodelistModelMaker.getFeaturesList(Configuration.Domain.ENSEMBLE));
		System.out.println(CodelistModelMaker.getFeaturesList(Configuration.Domain.ENSEIGNEMENT));
		System.out.println(CodelistModelMaker.getFeaturesList(Configuration.Domain.SPORT_LOISIR));
	}

	@Test
	public void testOrderCodeList() throws IOException {

		Path codeListPath = Configuration.DATA_RESOURCE_PATH.resolve("cl-sect.ttl");
		CodelistModelMaker.orderCodeList(codeListPath, null);
	}
}
