package fr.insee.semweb.bpe.test;

import java.io.FileWriter;

import fr.insee.semweb.bpe.Configuration;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Test;

import fr.insee.semweb.bpe.DBFModelMaker;
import fr.insee.semweb.bpe.Configuration.Domain;

public class DBFModelMakerTest {

	@Test
	public void testMakeOtherEquipmentsModel() throws Exception {

		Model equipments = DBFModelMaker.makeOtherEquipmentsModel(false);
		equipments.write(new FileWriter(Configuration.DATA_RESOURCE_PATH_OUT.resolve("other-equipments.ttl").toString()), "TTL");
	}

	@Test
	public void testMakeEductionEquipmentsModel() throws Exception {

		Model equipments = DBFModelMaker.makeEductionEquipmentsModel(true);
		equipments.write(new FileWriter(Configuration.DATA_RESOURCE_PATH_OUT.resolve("education-equipments.ttl").toString()), "TTL");
	}

	@Test
	public void testMakeSportsLeisureEquipmentsModel() throws Exception {

		Model equipments = DBFModelMaker.makeSportsLeisureEquipmentsModel(true);
		equipments.write(new FileWriter(Configuration.DATA_RESOURCE_PATH_OUT.resolve("sport-loisir-equipments.ttl").toString()), "TTL");
	}

	@Test
	public void testMakeQualityModel() throws Exception {

		Model qualityInfo = DBFModelMaker.makeQualityModel(Domain.ENSEIGNEMENT, true);
		qualityInfo.write(new FileWriter(Configuration.DATA_RESOURCE_PATH_OUT.resolve("qualite-enseignement.ttl").toString()), "TTL");
	}
}
