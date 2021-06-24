package fr.insee.semweb.bpe.test;

import java.io.FileWriter;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Test;

import fr.insee.semweb.bpe.SASModelMaker;

public class SASModelMakerTest {

	SASModelMaker sasModelMaker = new SASModelMaker();

	@Test
	public void testMakeBPEModel() throws Exception {

		Model equipments = sasModelMaker.makeBPEModel();
		equipments.write(new FileWriter("src/main/resources/data/equipments.ttl"), "TTL");
	}

	@Test
	public void testMakeBPEModelFilter() throws Exception {

		String filter = "D2";
		Model equipments = sasModelMaker.makeBPEModel(type -> type.startsWith(filter));
		equipments.write(new FileWriter("src/main/resources/data/equipments-" + filter.toLowerCase() + ".ttl"), "TTL");
	}

	@Test
	public void testMakeQualityModel() throws Exception {

		Model quality = sasModelMaker.makeQualityModel();
		quality.write(new FileWriter("src/main/resources/data/quality.ttl"), "TTL");
	}
}
