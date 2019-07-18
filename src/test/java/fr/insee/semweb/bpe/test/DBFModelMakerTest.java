package fr.insee.semweb.bpe.test;

import java.io.FileWriter;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import fr.insee.semweb.bpe.DBFModelMaker;
import fr.insee.semweb.bpe.Configuration.Domain;

public class DBFModelMakerTest {

	@Test
	public void testMakeOtherEquipmentsModel() throws Exception {

		Model equipments = DBFModelMaker.makeOtherEquipmentsModel(false);
		equipments.write(new FileWriter("src/main/resources/data/other-equipments.ttl"), "TTL");
	}

	@Test
	public void testMakeEductionEquipmentsModel() throws Exception {

		Model equipments = DBFModelMaker.makeEductionEquipmentsModel(true);
		equipments.write(new FileWriter("src/main/resources/data/education-equipments.ttl"), "TTL");
	}

	@Test
	public void testMakeSportsLeisureEquipmentsModel() throws Exception {

		Model equipments = DBFModelMaker.makeSportsLeisureEquipmentsModel(true);
		equipments.write(new FileWriter("src/main/resources/data/sport-loisir-equipments.ttl"), "TTL");
	}

	@Test
	public void testMakeQualityModel() throws Exception {

		Model qualityInfo = DBFModelMaker.makeQualityModel(Domain.ENSEIGNEMENT, true);
		qualityInfo.write(new FileWriter("src/main/resources/data/qualite-enseignement.ttl"), "TTL");
	}
}
