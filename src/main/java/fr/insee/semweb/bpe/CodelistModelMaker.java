package fr.insee.semweb.bpe;

import fr.insee.semweb.bpe.Configuration.Domain;
import net.iryndin.jdbf.core.DbfRecord;
import net.iryndin.jdbf.reader.DbfReader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CodelistModelMaker {

	public static Logger logger = LogManager.getLogger(CodelistModelMaker.class);

	/**
	 * Reads the code list of equipment types in the DBF file into a Jena model.
	 *
	 * @param useDBF Indicates if code list should be read in the dBase files (otherwise, TSV is used).
	 * @return A Jena <code>Model</code> containing the code list as a SKOS concept scheme.
	 * @throws IOException In case of problem reading the source file.
	 */
	public static Model makeEquipmentTypesCodelistModel(boolean useDBF) throws IOException {

		Model codeListModel = ModelFactory.createDefaultModel();
		codeListModel.setNsPrefix("skos", SKOS.getURI());
		codeListModel.setNsPrefix("rdfs", RDFS.getURI());
		codeListModel.setNsPrefix("ibpe", BPEOnto.getURI());
		codeListModel.setNsPrefix("icod-ter", Configuration.INSEE_CODES_BASE_URI + "territoire/");
		codeListModel.setNsPrefix("icod-teq", Configuration.INSEE_CODES_BASE_URI + "territoire/typeEquipement/");

		// Create the code list resource
		Resource schemeResource = codeListModel.createResource(Configuration.INSEE_EQUIPMENT_TYPES_CODELIST_URI, SKOS.ConceptScheme);
		schemeResource.addProperty(SKOS.notation, "CL_TYPEQU");
		schemeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Equipment types", "en"));
		schemeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Types d'équipements", "fr"));
		// Create also the class representing the code values (see Data Cube §8.1) and add cross-references to and from the scheme
		// For better clarity, we use the TypeEquipement class defined in the ontology
		Resource classResource = codeListModel.createResource(BPEOnto.TypeEquipement.getURI(), OWL.Class);
		classResource.addProperty(RDFS.seeAlso, schemeResource);
		schemeResource.addProperty(RDFS.seeAlso, classResource);

		// Create the collections for 'enseignement' and 'sport-loisir' equipment types
		Resource educationCollectionResource = codeListModel.createResource(Configuration.inseeEquipmentTypesCollectionURI(Domain.ENSEIGNEMENT), SKOS.Collection);
		educationCollectionResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Types d'équipements d'enseignement", "fr"));
		educationCollectionResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Education equipment types", "en"));
		educationCollectionResource.addProperty(RDFS.seeAlso, BPEOnto.TypeEquipementEnseignement);
		Resource sportLeisureCollectionResource = codeListModel.createResource(Configuration.inseeEquipmentTypesCollectionURI(Domain.SPORT_LOISIR), SKOS.Collection);
		sportLeisureCollectionResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Types d'équipements de sport et loisirs", "fr"));
		sportLeisureCollectionResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Sports and leisure equipment types", "en"));
		sportLeisureCollectionResource.addProperty(RDFS.seeAlso, BPEOnto.TypeEquipementSportLoisir);

		// Create the resources corresponding to individual codes
		Map<String, String> equipmentTypes = useDBF ? readEquipmentTypesDBF() : readEquipmentTypesTSV() ;
		for (String equipmentTypeCode : equipmentTypes.keySet()) {
			Resource codeResource = codeListModel.createResource(Configuration.inseeEquipmentTypeURI(equipmentTypeCode), SKOS.Concept);
			codeResource.addProperty(RDF.type, BPEOnto.TypeEquipement); // The codes are instances of the code concept class
			codeResource.addProperty(SKOS.notation, equipmentTypeCode);
			codeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral(equipmentTypes.get(equipmentTypeCode), "fr"));
			codeResource.addProperty(SKOS.inScheme, schemeResource);
			// If in specific domain, specify narrower type and add to collection
			if (Configuration.isEducation(equipmentTypeCode)) {
				codeResource.addProperty(RDF.type, BPEOnto.TypeEquipementEnseignement);
				educationCollectionResource.addProperty(SKOS.member, codeResource);
			}
			if (Configuration.isSportLeisure(equipmentTypeCode)) {
				codeResource.addProperty(RDF.type, BPEOnto.TypeEquipementSportLoisir);
				sportLeisureCollectionResource.addProperty(SKOS.member, codeResource);
			}
			if (!useDBF) {
				// Codes of higher levels are only in the TSV file
				if (equipmentTypeCode.length() == 1) {
					schemeResource.addProperty(SKOS.hasTopConcept, codeResource);
					continue;
				}
				int parentCodeLength = (equipmentTypeCode.length() == 2) ? 1 : 2;
				Resource parentResource = codeListModel.createResource(Configuration.inseeEquipmentTypeURI(equipmentTypeCode.substring(0, parentCodeLength)));
				codeResource.addProperty(SKOS.broader, parentResource);
				parentResource.addProperty(SKOS.narrower, codeResource);
			} else schemeResource.addProperty(SKOS.hasTopConcept, codeResource);
		}

		return codeListModel;
	}

	/**
	 * Reads the code list of equipment features in the text file and converts it into a Jena model.
	 * 
	 * @return A Jena <code>Model</code> containing the code list as a SKOS concept scheme.
	 */
	public static Model makeFeaturesCodelistModel() {

		Model codeListModel = ModelFactory.createDefaultModel();
		codeListModel.setNsPrefix("skos", SKOS.getURI());
		codeListModel.setNsPrefix("rdfs", RDFS.getURI());
		codeListModel.setNsPrefix("ibpe", BPEOnto.getURI());
		codeListModel.setNsPrefix("icod-ter", Configuration.INSEE_CODES_BASE_URI + "territoire/");
		codeListModel.setNsPrefix("icod-car", Configuration.INSEE_CODES_BASE_URI + "territoire/caractere/");

		// Create the code list resource
		Resource schemeResource = codeListModel.createResource(Configuration.INSEE_FEATURES_CODELIST_URI, SKOS.ConceptScheme);
		schemeResource.addProperty(SKOS.notation, "CL_CARACT");
		schemeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Equipment features", "en"));
		schemeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Caractéristiques d'équipements", "fr"));
		// Create also the class representing the code values (see Data Cube §8.1) and add cross-references to and from the scheme
		Resource classResource = codeListModel.createResource(BPEOnto.Caractere.getURI(), OWL.Class);
		classResource.addProperty(RDFS.seeAlso, schemeResource);
		schemeResource.addProperty(RDFS.seeAlso, classResource);

		// Create the collections for 'enseignement' and 'sport-loisir' features
		Resource educationCollectionResource = codeListModel.createResource(Configuration.inseeFeaturesCollectionURI(Domain.ENSEIGNEMENT), SKOS.Collection);
		educationCollectionResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Caractères des équipements d'enseignement", "fr"));
		educationCollectionResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Education equipment features", "en"));
		educationCollectionResource.addProperty(RDFS.seeAlso, BPEOnto.CaractereEnseignement);
		Resource sportLeisureCollectionResource = codeListModel.createResource(Configuration.inseeFeaturesCollectionURI(Domain.SPORT_LOISIR), SKOS.Collection);
		sportLeisureCollectionResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Caractères des équipements de sport et loisirs", "fr"));
		sportLeisureCollectionResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Sports and leisure equipment features", "en"));
		sportLeisureCollectionResource.addProperty(RDFS.seeAlso, BPEOnto.CaractereSportLoisir);

		// Read the TSV file to get the list of features 
		Path codelistFilePath = Configuration.getFeaturesCodelistFilePath();
		logger.info("Building the features code list from file " + codelistFilePath);
        try (Stream<String> lines = Files.lines(codelistFilePath)) {
        	lines.forEach(line -> {
				String[] tokens = line.split("\t");
				if (tokens.length == 3) {
					String featureCode = tokens[0];
					Resource codeResource = codeListModel.createResource(Configuration.inseeFeatureURI(featureCode), SKOS.Concept);
					codeResource.addProperty(RDF.type, BPEOnto.Caractere); // The codes are instances of the code concept class
					codeResource.addProperty(SKOS.notation, featureCode);
					codeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral(tokens[2], "fr"));
					codeResource.addProperty(SKOS.inScheme, schemeResource);
					schemeResource.addProperty(SKOS.hasTopConcept, codeResource);
					try {
						Domain featureDomain = Domain.valueOf(tokens[1]); // Will throw InvalidArgumentExpression if input string does not match
						if (featureDomain == Domain.ENSEIGNEMENT) {
							codeResource.addProperty(RDF.type, BPEOnto.CaractereEnseignement);
							educationCollectionResource.addProperty(SKOS.member, codeResource);
						}
						if (featureDomain == Domain.SPORT_LOISIR) {
							codeResource.addProperty(RDF.type, BPEOnto.CaractereSportLoisir);
							sportLeisureCollectionResource.addProperty(SKOS.member, codeResource);
						}
					} catch (Exception ignored) {}
				}
			});
		} catch (Exception e) {
			logger.error("Error processing file - " + e.getMessage());
		}

		return codeListModel;
	}

	/**
	 * Creates the code list of equipment sectors into a Jena model.
	 * 
	 * @return A Jena <code>Model</code> containing the code list as a SKOS concept scheme.
	 */
	public static Model makeSectorsCodelistModel() {

		Model codeListModel = ModelFactory.createDefaultModel();
		codeListModel.setNsPrefix("skos", SKOS.getURI());
		codeListModel.setNsPrefix("rdfs", RDFS.getURI());
		codeListModel.setNsPrefix("ibpe", BPEOnto.getURI());
		codeListModel.setNsPrefix("icod-ter", Configuration.INSEE_CODES_BASE_URI + "territoire/");
		codeListModel.setNsPrefix("icod-sec", Configuration.INSEE_CODES_BASE_URI + "territoire/secteur/"); // Could be promoted to global-level code list

		// Create the code list resource
		Resource schemeResource = codeListModel.createResource(Configuration.INSEE_SECTORS_CODELIST_URI, SKOS.ConceptScheme);
		schemeResource.addProperty(SKOS.notation, "CL_SECTEURS");
		schemeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Education sectors", "en"));
		schemeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Secteurs d'éducation", "fr"));
		// Create also the class representing the code values (see Data Cube §8.1) and add cross-references to and from the scheme
		Resource classResource = codeListModel.createResource(BPEOnto.Secteur.getURI(), OWL.Class);
		classResource.addProperty(RDFS.seeAlso, schemeResource);
		schemeResource.addProperty(RDFS.seeAlso, classResource);

    	Resource codeResource = codeListModel.createResource(Configuration.inseeSectorURI("PR"), SKOS.Concept);
		codeResource.addProperty(RDF.type, BPEOnto.Secteur); // The codes are instances of the code concept class
		codeResource.addProperty(SKOS.notation, "PR");
		codeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Privé", "fr"));
		codeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Private", "en"));
		codeResource.addProperty(SKOS.inScheme, schemeResource);
		schemeResource.addProperty(SKOS.hasTopConcept, codeResource);
		codeResource = codeListModel.createResource(Configuration.inseeSectorURI("PU"), SKOS.Concept);
		codeResource.addProperty(RDF.type, BPEOnto.Secteur); // The codes are instances of the code concept class
		codeResource.addProperty(SKOS.notation, "PU");
		codeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Public", "fr"));
		codeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Public", "en"));
		codeResource.addProperty(SKOS.inScheme, schemeResource);
		schemeResource.addProperty(SKOS.hasTopConcept, codeResource);

		return codeListModel;
	}

	/**
	 * Creates the code list of quality levels into a Jena model.
	 * 
	 * @return A Jena <code>Model</code> containing the code list as a SKOS concept scheme.
	 */
	public static Model makeQualityLevelsCodelistModel() {

		Model codeListModel = ModelFactory.createDefaultModel();
		codeListModel.setNsPrefix("skos", SKOS.getURI());
		codeListModel.setNsPrefix("rdfs", RDFS.getURI());
		codeListModel.setNsPrefix("owl", OWL.getURI());
		codeListModel.setNsPrefix("icod-qlt", Configuration.INSEE_QUALITY_CODES_BASE_URI);

		// Create the code list resource
		Resource schemeResource = codeListModel.createResource(Configuration.INSEE_QUALITY_CODES_BASE_URI + "niveaux", SKOS.ConceptScheme);
		schemeResource.addProperty(SKOS.notation, "CL_NIVQUAL");
		schemeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Quality levels", "en"));
		schemeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Niveaux de qualité", "fr"));
		// Create also the class representing the code values (see Data Cube §8.1)
		Resource classResource = codeListModel.createResource(Configuration.INSEE_QUALITY_CODES_BASE_URI + "Niveau", RDFS.Class);
		classResource.addProperty(RDF.type, OWL.Class);
		classResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Quality level", "en"));
		classResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Niveau de qualité", "fr"));
		schemeResource.addProperty(RDFS.seeAlso, classResource);  // Add cross-references between the scheme and the class
		classResource.addProperty(RDFS.seeAlso, schemeResource);

		// Add the three modalities used in the BPE datasets
    	Resource codeResource = codeListModel.createResource(Configuration.inseeQualityLevelURI("BON"), SKOS.Concept);
		codeResource.addProperty(RDF.type, classResource); // The codes are instances of the code concept class
		codeResource.addProperty(SKOS.notation, "BON");
		codeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Bon", "fr"));
		codeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Good", "en"));
		codeResource.addProperty(SKOS.inScheme, schemeResource);
		schemeResource.addProperty(SKOS.hasTopConcept, codeResource);
		codeResource = codeListModel.createResource(Configuration.inseeQualityLevelURI("ACCEPTABLE"), SKOS.Concept);
		codeResource.addProperty(RDF.type, classResource);
		codeResource.addProperty(SKOS.notation, "ACCEPTABLE");
		codeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Acceptable", "fr"));
		codeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Acceptable", "en"));
		codeResource.addProperty(SKOS.inScheme, schemeResource);
		schemeResource.addProperty(SKOS.hasTopConcept, codeResource);
		codeResource = codeListModel.createResource(Configuration.inseeQualityLevelURI("MAUVAIS"), SKOS.Concept);
		codeResource.addProperty(RDF.type, classResource);
		codeResource.addProperty(SKOS.notation, "MAUVAIS");
		codeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Mauvais", "fr"));
		codeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Bad", "en"));
		codeResource.addProperty(SKOS.inScheme, schemeResource);
		schemeResource.addProperty(SKOS.hasTopConcept, codeResource);

		return codeListModel;
	}

	/**
	 * Reads the list of equipment types from a TSV file.
	 * 
	 * @return The list of equipmentTypes as a map with codes as keys and labels as values.
	 * @throws IOException In case of problem reading the file.
	 */
	public static Map<String, String> readEquipmentTypesTSV() throws IOException {

		String delimiter = "\t";
		return Files.lines(Configuration.getTypesCodelistTSVFilePath()).collect(Collectors.toMap(line -> line.split(delimiter)[0], line -> line.split(delimiter)[1]));		
	}

	/**
	 * Reads the list of equipment types from a dBase file.
	 * 
	 * @return The list of equipmentTypes as a map with codes as keys and labels as values.
	 */
	public static Map<String, String> readEquipmentTypesDBF() {

		Map<String, String> equipmentTypes = new HashMap<>();

		Path codelistFilePath = Configuration.getBDFTypesCodelistFilePath(Domain.ENSEMBLE);
		DbfRecord record = null;
		try {
			InputStream dbf = new FileInputStream(codelistFilePath.toString());
			try (DbfReader reader = new DbfReader(dbf)) {
				while ((record = reader.read()) != null) {
					record.setStringCharset(Configuration.STRING_CHARSET);
					logger.debug("Processing record " + record.toMap());
					logger.debug("VARIABLE field value: " + record.getString("VARIABLE"));
					if (record.getString("VARIABLE").equals("TYPEQU")) {
						String typeCode = record.getString("MODALITE");
						String typeLabel = Configuration.normalizeString(record.getString("MODLIBELLE"));
						equipmentTypes.put(typeCode, typeLabel);
					}
				}
			}
			dbf.close();
		} catch (Exception e) {
			logger.error("Error processing " + record + " - " + e.getMessage());
		}
		return equipmentTypes;
	}

	/**
	 * Retrieves the list of feature for a given domain.
	 * 
	 * @param domain The domain of the features.
	 * @return The list of feature identifiers.
	 * @throws IOException In case of problem reading the file.
	 */
	public static List<String> getFeaturesList(Domain domain) throws IOException {

		return Files.lines(Configuration.getFeaturesCodelistFilePath())
				.filter(line -> line.trim().length() > 0)
				.filter(line -> Domain.valueOf(line.split("\t")[1]).equals(domain))
				.map(line -> line.split("\t")[0])
				.collect(Collectors.toList());
	}

	/**
	 * Orders a list of codes in a Turtle file.
	 * Reads of Turtle file containing a code lists and writes a Turtle file containing the same list correctly ordered.
	 * The order is: OWL class (if any), SKOS concept scheme, SKOS collections if any, SKOS concepts ordered by URIs.
	 * 
	 * @throws IOException In case of problem reading the input or writing the output.
	 */
	public static void orderCodeList(Path unorderedIn, Path orderedOut) throws IOException {

		logger.info("Reordering of code list in Turtle file " + unorderedIn);

		List<String> chunks = new ArrayList<>();
		Map<CodeListComponent, List<Integer>> indexes = new HashMap<>();
		for (CodeListComponent composant : CodeListComponent.values()) indexes.put(composant, new ArrayList<>());

		try (BufferedReader reader = new BufferedReader(new FileReader(unorderedIn.toFile()))) {
			StringBuilder chunk = new StringBuilder();
			String line;
			do {
				line = reader.readLine();
				if ((line != null) && (line.trim().length() > 0)) chunk.append(line).append(System.lineSeparator());
				else { // End of a chunk
					if (chunk.length() == 0) continue; // Several empty lines, or empty line before end of file
					String chunkString = chunk.toString();
					CodeListComponent componentType = CodeListComponent.guessComponentType(chunkString);
					if (componentType == null) {
						// The first chunk is prefix declarations
						if (chunks.size() > 0) logger.warn("Unrecognized Turtle segment starting with " + chunkString.substring(0, 100));
					}
					else indexes.get(componentType).add(chunks.size());
					chunks.add(chunkString);
					chunk.setLength(0);
				}
			} while (line != null);
		}
		// A bit of reporting, and write ordered output
		logger.info("Code list components: " + indexes);
		if (indexes.get(CodeListComponent.CLASS).size() > 1) logger.warn("Several classes are defined in the code list");
		if (indexes.get(CodeListComponent.SCHEME).size() > 1) logger.warn("Several concept schemes are defined in the code list");

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(orderedOut.toFile()))) {

			// Fist write prefixes, then class and concept scheme (hopefully unique), then collections
			writer.write(chunks.get(0));
			for (int index : indexes.get(CodeListComponent.CLASS)) writer.write(chunks.get(index));
			writer.newLine();
			for (int index : indexes.get(CodeListComponent.SCHEME)) {
				writer.write(chunks.get(index));
				writer.newLine();
			}
			for (int index : indexes.get(CodeListComponent.COLLECTION)) {
				writer.write(chunks.get(index));
				writer.newLine();
			}
			// Sort concepts and write them
			SortedSet<String> orderedConcepts = new TreeSet<>();
			for (int index : indexes.get(CodeListComponent.CONCEPT)) orderedConcepts.add(chunks.get(index));
			for (String concept : orderedConcepts) {
				writer.write(concept);
				writer.newLine();
			}
		}
	}

	/** Enumeration of code list components */
	public enum CodeListComponent {

		CLASS,
		SCHEME,
		COLLECTION,
		CONCEPT;

		/**
		 * Guesses the type of the subject of a Turtle statement.
		 * Very basic implementation. Could do better using https://github.com/antlr/grammars-v4/blob/master/turtle/TURTLE.g4.
		 * 
		 * @param turtleText A Turtle statement as defined in https://www.w3.org/TR/turtle/#sec-grammar-grammar.
		 * @return The value of the enumeration corresponding to the guess made, or <code>null</code> if not recognised.
		 */
		public static CodeListComponent guessComponentType(String   turtleText) {

			if (turtleText == null) return null;
			if (turtleText.contains("owl:Class")) return CLASS;
			if (turtleText.contains("skos:ConceptScheme")) return SCHEME;
			if (turtleText.contains("skos:Collection")) return COLLECTION;
			if (turtleText.contains("skos:Concept")) return CONCEPT;
			
			return null;
		}
	}
}