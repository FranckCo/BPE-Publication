package fr.insee.semweb.bpe.test;

import java.io.FileWriter;

import fr.insee.semweb.bpe.Configuration;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Test;

import fr.insee.semweb.bpe.SASModelMaker;

public class SASModelMakerTest {

	SASModelMaker sasModelMaker = new SASModelMaker();

	@Test
	public void testMakeBPEModel() throws Exception {

		Model equipments = sasModelMaker.makeBPEModel();
		equipments.write(new FileWriter(Configuration.DATA_RESOURCE_PATH_OUT.resolve("equipments.ttl").toString()), "TTL");
	}

	@Test
	public void testMakeBPEModelFilter() throws Exception {

		String filter = "D2";
		String fileName = Configuration.DATA_RESOURCE_PATH_OUT.toString() + "/equipments-" + filter.toLowerCase() + ".ttl";
		Model equipments = sasModelMaker.makeBPEModel(type -> type.startsWith(filter));
		equipments.write(new FileWriter(fileName), "TTL");
	}

	@Test
	public void testMakeQualityModel() throws Exception {

		Model quality = sasModelMaker.makeQualityModel();
		quality.write(new FileWriter(Configuration.DATA_RESOURCE_PATH_OUT.resolve("quality.ttl").toString()), "TTL");
	}
}
