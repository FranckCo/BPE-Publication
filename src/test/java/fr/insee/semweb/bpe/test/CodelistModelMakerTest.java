package fr.insee.semweb.bpe.test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Test;

import fr.insee.semweb.bpe.CodelistModelMaker;
import fr.insee.semweb.bpe.Configuration;

public class CodelistModelMakerTest {

	@Test
	public void testEquipmentTypesCodelist() throws Exception {

		Model codeList = CodelistModelMaker.makeEquipmentTypesCodelistModel(false);
		codeList.write(new FileWriter(Configuration.DATA_RESOURCE_PATH_OUT.resolve("cl-typequ.ttl").toString()), "TTL");
	}

	@Test
	public void testFeaturesCodelist() throws Exception {

		Model codeList = CodelistModelMaker.makeFeaturesCodelistModel();
		codeList.write(new FileWriter(Configuration.DATA_RESOURCE_PATH_OUT.resolve("cl-caract.ttl").toString()), "TTL");
	}

	@Test
	public void testSectorsCodelist() throws Exception {

		Model codeList = CodelistModelMaker.makeSectorsCodelistModel();
		codeList.write(new FileWriter(Configuration.DATA_RESOURCE_PATH_OUT.resolve("cl-sect.ttl").toString()), "TTL");
	}

	@Test
	public void testQualityLevelsCodelist() throws Exception {

		Model codeList = CodelistModelMaker.makeQualityLevelsCodelistModel();
		codeList.write(new FileWriter(Configuration.DATA_RESOURCE_PATH_OUT.resolve("cl-qual.ttl").toString()), "TTL");
	}

	@Test
	public void testAllCodeListsOrdered() throws Exception {

		Path tempFilePath = Configuration.DATA_RESOURCE_PATH_OUT.resolve("cl-temp.ttl");

		Model codeList = CodelistModelMaker.makeEquipmentTypesCodelistModel(false);
		Path orderedCodeListPath = Configuration.DATA_RESOURCE_PATH_OUT.resolve("cl-typequ-ord.ttl");
		codeList.write(new FileWriter(tempFilePath.toString()), "TTL");
		CodelistModelMaker.orderCodeList(tempFilePath, orderedCodeListPath);

		codeList = CodelistModelMaker.makeFeaturesCodelistModel();
		orderedCodeListPath = Configuration.DATA_RESOURCE_PATH_OUT.resolve("cl-caract-ord.ttl");
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
		Path codeListPath = Configuration.DATA_RESOURCE_PATH_OUT.resolve(codeListName + ".ttl");
		Path orderedCodeListPath = Configuration.DATA_RESOURCE_PATH_OUT.resolve(codeListName + "-ord.ttl");
		CodelistModelMaker.orderCodeList(codeListPath, orderedCodeListPath);
	}

	@Test
	public void testQE() {
		System.out.println(Configuration.QualityLevel.RESOURCE_MAP);
	}
}
