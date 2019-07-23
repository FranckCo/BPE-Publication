package fr.insee.semweb.bpe;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SASLauncher {

	public static Logger logger = LogManager.getLogger(SASLauncher.class);

	public static void main(String... args) throws InterruptedException, IOException {

		final int SLEEP_DURATION = 20 * 1000;
		boolean CREATE_MAIN_MODEL = false;
		boolean CREATE_QUALITY_MODEL = true;

		Map<String, Predicate<String>> predicates = new HashMap<String, Predicate<String>>();
		List<String> chunks = Arrays.asList("A1+A2", "A3", "A401+A402", "A403+A404", "A405+A406", "A501+A502+A503", "A504", "A505+A506+A507", "B1+B2", "B3", "C", "D-D2", "D2", "E", "F", "G");

		// Calculate the predicate associated to each chunk expression
		for (String chunk : chunks) {
			Predicate<String> predicate;
			// First process the 'minus' patterns
			final String[] componentsMinus = chunk.split("-"); // Must have final variables in the predicate expressions
			if (componentsMinus.length == 2) { // Minus expressions should have exactly two components
				predicate = type -> type.startsWith(componentsMinus[0]);
				predicate = predicate.and(type -> !type.startsWith(componentsMinus[1]));
			} else {
				final String[] componentsPlus = chunk.split("\\+");
				predicate  = type -> false;
				for (int index = 0; index < componentsPlus.length; index++) {
					final String filter = componentsPlus[index];
					predicate = predicate.or(type -> type.startsWith(filter));
				}
			}
			predicates.put(chunk, predicate);
		}

		// Now launch Jena models creation chunk by chunk
		int tripleCount = 0;
		if (CREATE_MAIN_MODEL) {
			for (String chunk : chunks) {
				logger.info("Lanching main model creation for filter " + chunk);
				Model equipments = SASModelMaker.makeBPEModel(predicates.get(chunk));
				equipments.write(new FileWriter("src/main/resources/data/equipments-" + chunk.toLowerCase() + ".ttl"), "TTL");
				System.out.println("Model created for filter " + chunk + " with " + equipments.size() + " triples");
				tripleCount += equipments.size();
				equipments.close();
				Thread.sleep(SLEEP_DURATION); // Let the garbage collection proceed
			}
			System.out.println(chunks.size() + " models created with a total of " + tripleCount + " triples");
		}
		if (CREATE_QUALITY_MODEL) {
			tripleCount = 0;
			for (String chunk : chunks) {
				logger.info("Lanching quality model creation for filter " + chunk);
				Model quality = SASModelMaker.makeQualityModel(predicates.get(chunk));
				if (quality.size() > 0) {
					quality.write(new FileWriter("src/main/resources/data/geo-quality-" + chunk.toLowerCase() + ".ttl"), "TTL");
					System.out.println("Quality model created for filter " + chunk + " with " + quality.size() + " triples");
					tripleCount += quality.size();
				} else { // Some types of equipments are not geocoded
					chunks.remove(chunk);
					System.out.println("No quality metadata for filter " + chunk + ", no model created");
				}
				quality.close();
				Thread.sleep(SLEEP_DURATION); // Let the garbage collection proceed
			}
			System.out.println(chunks.size() + " models created with a total of " + tripleCount + " triples");
		}
	}
}
