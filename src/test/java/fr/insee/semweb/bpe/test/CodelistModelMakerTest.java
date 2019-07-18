package fr.insee.semweb.bpe.test;

import java.io.FileWriter;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import fr.insee.semweb.bpe.CodelistModelMaker;
import fr.insee.semweb.bpe.Configuration;

public class CodelistModelMakerTest {

	@Test
	public void testEquipmentTypesCodelist() throws Exception {

		Model codeList = CodelistModelMaker.makeEquipmentTypesCodelistModel();
		codeList.write(new FileWriter("src/main/resources/data/cl-typequ-tsv.ttl"), "TTL");
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
}
