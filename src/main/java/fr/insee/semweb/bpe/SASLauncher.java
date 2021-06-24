package fr.insee.semweb.bpe;

import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * <code>SASLauncher</code> pilots the creation of the BPE models from data contained in a SAS database.
 *
 * The actual creation of the models is done in {@link SASModelMaker}.
 */
public class SASLauncher {

	public static Logger logger = LogManager.getLogger(SASLauncher.class);

	/**
	 * Main method: sets the model chunks, then creates the corresponding data and quality models.
	 *
	 * @param args Not used.
	 * @throws Exception In case of problem.
	 */
	public static void main(String... args) throws Exception {

		SASModelMaker sasModelMaker = new SASModelMaker();

		// The following parameters should be set before launching the process
		final int SLEEP_DURATION = 20 * 1000; // Length of pause before starting the next chunk
		boolean CREATE_MAIN_MODEL = true; // Create the main model if true
		boolean CREATE_QUALITY_MODEL = true; // Create the quality model if true

		// Specifies the partial models in terms of filters on the type of equipment
		List<String> chunks = Arrays.asList("A1+A2", "A3", "A401+A402", "A403+A404", "A405+A406", "A501+A502+A503", "A504", "A505+A506+A507", "B1+B2", "B3", "C", "D-D2", "D2", "E", "F", "G");

		// Calculate the predicate associated to each chunk expression
		Map<String, Predicate<String>> predicates = new HashMap<>();
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
				for (final String filter : componentsPlus) {
					predicate = predicate.or(type -> type.startsWith(filter));
				}
			}
			predicates.put(chunk, predicate);
		}

		// Now launch Jena models creation chunk by chunk
		int tripleCount = 0;
		if (CREATE_MAIN_MODEL) {
			for (String chunk : chunks) {
				logger.info("Launching main model creation for filter " + chunk);
				Model equipments = sasModelMaker.makeBPEModel(predicates.get(chunk));
				equipments.write(new FileWriter("src/main/resources/data/facilities-" + chunk.toLowerCase() + ".ttl"), "TTL");
				logger.info("Model created for filter " + chunk + " with " + equipments.size() + " triples");
				tripleCount += equipments.size();
				equipments.close();
				Thread.sleep(SLEEP_DURATION); // Let the garbage collection proceed
			}
			logger.info(chunks.size() + " models created with a total of " + tripleCount + " triples");
		}
		if (CREATE_QUALITY_MODEL) {
			tripleCount = 0;
			int modelCount = chunks.size();
			for (String chunk : chunks) {
				logger.info("Launching quality model creation for filter " + chunk);
				Model quality = sasModelMaker.makeQualityModel(predicates.get(chunk));
				if (quality.size() > 0) {
					quality.write(new FileWriter("src/main/resources/data/geo-quality-" + chunk.toLowerCase() + ".ttl"), "TTL");
					logger.info("Quality model created for filter " + chunk + " with " + quality.size() + " triples");
					tripleCount += quality.size();
				} else { // Some types of equipments are not geocoded
					logger.info("No quality metadata for filter " + chunk + ", no model created");
					modelCount--;
				}
				quality.close();
				Thread.sleep(SLEEP_DURATION); // Let the garbage collection proceed
			}
			logger.info(modelCount + " models created with a total of " + tripleCount + " triples");
		}
	}
}
