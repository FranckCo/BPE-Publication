package fr.insee.semweb.bpe;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.insee.semweb.bpe.Configuration.Domain;
import net.iryndin.jdbf.core.DbfRecord;
import net.iryndin.jdbf.reader.DbfReader;

public class CodelistModelMaker {

	public static Logger logger = LogManager.getLogger(CodelistModelMaker.class);

	/**
	 * Reads the code list of equipment types in the DBF file into a Jena model.
	 * 
	 * @return A Jena <code>Model</code> containing the code list as a SKOS concept scheme.
	 * @throws IOException In case of problem reading the source file.
	 */
	public static Model makeEquipmentTypesCodelistModel() throws IOException {

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
		// Create also the class representing the code values (see Data Cube §8.1)
		schemeResource.addProperty(RDFS.seeAlso, BPEOnto.TypeEquipement);  // Add a reference from the scheme to the TypeEquipement class

		// Create the collections for 'enseignement' and 'sport-loisir' equipment types
		Resource educationCollectionResource = codeListModel.createResource(Configuration.inseeEquipmentTypesCollectionURI(Domain.ENSEIGNEMENT), SKOS.Collection);
		educationCollectionResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Types d'équipements d'enseignement", "fr"));
		educationCollectionResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Education equipment types", "en"));
		educationCollectionResource.addProperty(RDFS.seeAlso, BPEOnto.TypeEquipementEnseignement);
		Resource sportLeisureCollectionResource = codeListModel.createResource(Configuration.inseeEquipmentTypesCollectionURI(Domain.SPORT_LOISIR), SKOS.Collection);
		sportLeisureCollectionResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Types d'équipements de sport et loisirs", "fr"));
		sportLeisureCollectionResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral("Sports and leisure equipment types", "en"));
		sportLeisureCollectionResource.addProperty(RDFS.seeAlso, BPEOnto.TypeEquipementSportLoisir);

		boolean useTSV = true; // Set to false to use dBase source file
		Map<String, String> equipmentTypes = useTSV ? readEquipmentTypesTSV() : readEquipmentTypesDBF();
		for (String equipementTypeCode : equipmentTypes.keySet()) {
			Resource codeResource = codeListModel.createResource(Configuration.inseeEquipmentTypeURI(equipementTypeCode), SKOS.Concept);
			codeResource.addProperty(RDF.type, BPEOnto.TypeEquipement); // The codes are instances of the code concept class
			codeResource.addProperty(SKOS.notation, equipementTypeCode);
			codeResource.addProperty(SKOS.prefLabel, codeListModel.createLiteral(equipmentTypes.get(equipementTypeCode), "fr"));
			codeResource.addProperty(SKOS.inScheme, schemeResource);
			// If in specific domain, specify narrower type and add to collection
			if (Configuration.isEducation(equipementTypeCode)) {
				codeResource.addProperty(RDF.type, BPEOnto.TypeEquipementEnseignement);
				educationCollectionResource.addProperty(SKOS.member, codeResource);
			}
			if (Configuration.isSportLeisure(equipementTypeCode)) {
				codeResource.addProperty(RDF.type, BPEOnto.TypeEquipementSportLoisir);
				sportLeisureCollectionResource.addProperty(SKOS.member, codeResource);
			}
			if (useTSV) {
				// Codes of higher levels are only in the TSV file
				if (equipementTypeCode.length() == 1) {
					schemeResource.addProperty(SKOS.hasTopConcept, codeResource);
					continue;
				}
				int parentCodeLength = (equipementTypeCode.length() == 2) ? 1 : 2;
				Resource parentResource = codeListModel.createResource(Configuration.inseeEquipmentTypeURI(equipementTypeCode.substring(0, parentCodeLength)));
				codeResource.addProperty(SKOS.broader, parentResource);
				parentResource.addProperty(SKOS.narrower, codeResource);
			} else schemeResource.addProperty(SKOS.hasTopConcept, codeResource);
		}

		return codeListModel;
	}

	/**
	 * Reads the code list of equipment features in the text file into a Jena model.
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
		// Create also the class representing the code values (see Data Cube §8.1)
		schemeResource.addProperty(RDFS.seeAlso, BPEOnto.Caractere);  // Add a reference from the scheme to the Caractere class

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
        try (Stream<String> lines = Files.lines(codelistFilePath)) {
        	lines.forEach(new Consumer<String>() {
                public void accept(String line) {
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
		// Create also the class representing the code values (see Data Cube §8.1)
		schemeResource.addProperty(RDFS.seeAlso, BPEOnto.Secteur);  // Add a reference from the scheme to the Secteur class

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

		Map<String, String> equipmentTypes = new HashMap<String, String>();

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
}