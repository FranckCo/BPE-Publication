package fr.insee.semweb.bpe.test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
	public void testAllCodeListsOrdered() throws Exception {

		Path tempFilePath = Configuration.DATA_RESOURCE_PATH.resolve("cl-temp.ttl");

		Model codeList = CodelistModelMaker.makeEquipmentTypesCodelistModel();
		Path orderedCodeListPath = Configuration.DATA_RESOURCE_PATH.resolve("cl-typequ-ord.ttl");
		codeList.write(new FileWriter(tempFilePath.toString()), "TTL");
		CodelistModelMaker.orderCodeList(tempFilePath, orderedCodeListPath);

		codeList = CodelistModelMaker.makeFeaturesCodelistModel();
		orderedCodeListPath = Configuration.DATA_RESOURCE_PATH.resolve("cl-caract-ord.ttl");
		codeList.write(new FileWriter(tempFilePath.toString()), "TTL");
		CodelistModelMaker.orderCodeList(tempFilePath, orderedCodeListPath);

	}

	@Test
	public void testGetFeatureList() throws Exception {

		System.out.println(CodelistModelMaker.getFeaturesList(Configuration.Domain.ENSEMBLE));
		System.out.println(CodelistModelMaker.getFeaturesList(Configuration.Domain.ENSEIGNEMENT));
		System.out.println(CodelistModelMaker.getFeaturesList(Configuration.Domain.SPORT_LOISIR));
	}

	@Test
	public void testOrderCodeList() throws IOException {

		String codeListName = "cl-typequ";
		Path codeListPath = Configuration.DATA_RESOURCE_PATH.resolve(codeListName + ".ttl");
		Path orderedCodeListPath = Configuration.DATA_RESOURCE_PATH.resolve(codeListName + "-ord.ttl");
		CodelistModelMaker.orderCodeList(codeListPath, orderedCodeListPath);
	}
}
