package fr.insee.semweb.bpe.test;

import java.io.FileWriter;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import fr.insee.semweb.bpe.SASModelMaker;

public class SASModelMakerTest {

	@Test
	public void testMakeBPEModel() throws Exception {

		Model equipments = SASModelMaker.makeBPEModel();
		equipments.write(new FileWriter("src/main/resources/data/equipments.ttl"), "TTL");
	}

	@Test
	public void testMakeBPEModelFilter() throws Exception {

		String filter = "D2";
		Model equipments = SASModelMaker.makeBPEModel(type -> type.startsWith(filter));
		equipments.write(new FileWriter("src/main/resources/data/equipments-" + filter.toLowerCase() + ".ttl"), "TTL");
	}

	@Test
	public void testMakeQualityModel() throws Exception {

		Model quality = SASModelMaker.makeQualityModel();
		quality.write(new FileWriter("src/main/resources/data/quality.ttl"), "TTL");
	}
}
