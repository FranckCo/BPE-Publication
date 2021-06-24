package fr.insee.semweb.bpe;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Configuration {

	public static final Charset STRING_CHARSET = Charset.forName("Cp1252"); // For dBase files
	public static final int SAMPLING_RATE = 10000; // Idem
	public static final boolean CREATE_GEOMETRY = true; // Create GeoSPARQL Geometry resources
	public static final boolean QUALITY_ANNOTATIONS = true; // Create DQV quality annotations
	public static final boolean BETA_NAMING = true; // Creates URI in a 'beta' namespace

	/** Input data will be read in this folder, and all files created will be put here */
	public static final Path DATA_RESOURCE_PATH = Paths.get("src/main/resources/data");
	/** Text configuration files (lists of features, equipment lists...) should be in this folder */
	public static final Path CONF_RESOURCE_PATH = Paths.get("src/main/resources/conf");

	
	/** Mappings between feature markers and OWL properties */
	public static Map<String, OntProperty> featurePresence = new HashMap<>();
	static {
		Configuration.featurePresence.put("0", BPEOnto.caractereAbsent);
		Configuration.featurePresence.put("1", BPEOnto.caracterePresent);
	}

	// Configuration files

	/** Path of the TSV file containing the equipment types for a given domain */
	public static Path getTypesCodelistTSVFilePath() {
		// Basically copied with edits from https://www.insee.fr/fr/statistiques/fichier/3568629/Contenu_bpe18_ensemble.pdf
		return CONF_RESOURCE_PATH.resolve("bpe2018-types.tsv");
	}

	/** Path of the TSV file containing the equipment features (for all domains) */
	public static Path getFeaturesCodelistFilePath() {
		return CONF_RESOURCE_PATH.resolve("bpe2018-features.tsv");
	}

	/** Path of the TSV file containing the links between equipment types and features or properties */
	public static Path getFeaturesByTypesFilePath() {
		return CONF_RESOURCE_PATH.resolve("bpe2018-types-features.tsv");
	}

	// dBase files

	/** Returns the paths of the dBase files (data and configuration) for a given domain */
	public static Map<Path, Boolean> getDBFFilePaths(Domain domain) {

		Map<Path, Boolean> paths = new HashMap<>();
		paths.put(DATA_RESOURCE_PATH.resolve("bpe_" + domain + "_xy.dbf"), true); // sampled
		paths.put(DATA_RESOURCE_PATH.resolve("varlist_" + domain + "_xy.dbf"), false);
		paths.put(DATA_RESOURCE_PATH.resolve("varmod_" + domain + "_xy.dbf"), false);

		return paths;
	}

	/** Path of the dBase file containing the equipment types for a given domain */
	public static Path getBDFTypesCodelistFilePath(Domain domain) {
		return DATA_RESOURCE_PATH.resolve("varmod_" + domain.toString() + "_xy.dbf");
	}

	/** Path of the dBase file containing the equipment list for a given domain */
	public static Path getDBFDataFilePath(Domain domain) {
		return DATA_RESOURCE_PATH.resolve("bpe_" + domain.toString() + "_xy.dbf");
	}

	// SAS files

	/** Path of the SAS file containing the data */
	public static Path getSASDataFilePath() {
		return DATA_RESOURCE_PATH.resolve("detail_diffxy_internet.sas7bdat");
	}

	/** Names of the SAS variables corresponding to the main features in the different domains */
	static Map<Domain, List<String>> sasFeatures = new HashMap<>();
	static {
		sasFeatures.put(Domain.ENSEIGNEMENT, Arrays.asList("cantine", "internat", "rpic", "cl_pelem", "cl_pge", "ep"));
		sasFeatures.put(Domain.SPORT_LOISIR, Arrays.asList("couvert", "eclaire"));
		sasFeatures.put(Domain.ENSEMBLE, new ArrayList<>());
	}

	// Naming

	/** (Dereferenceable) URIs for coordinate systems (see http://www.epsg-registry.org/) */
	public static String LAMBERT_93_URI = "http://www.opengis.net/def/crs/EPSG/0/2154"; // See also https://epsg.io/2154
	public static String UTM40S_URI = "http://www.opengis.net/def/crs/EPSG/0/32740"; // UTM40S (https://epsg.io/32740) for the Réunion island
	public static String UTM20N_URI = "http://www.opengis.net/def/crs/EPSG/0/32620"; // UTM20N (https://epsg.io/32620) for the Martinique and Guadeloupe islands
	public static String UTM22N_URI = "http://www.opengis.net/def/crs/EPSG/0/2972"; // UTM22N (https://epsg.io/2972) for the Guyane

	// Constants for naming
	/** Base URI for equipments */
	public static String INSEE_EQUIPMENT_BASE_URI = "http://" + (BETA_NAMING ? "beta." : "") + "id.insee.fr/territoire/equipement/";
	/** Base URI for Insee codes */
	public static String INSEE_CODES_BASE_URI = "http://" + (BETA_NAMING ? "beta." : "") + "id.insee.fr/codes/";
	/** Base URI for Insee quality codes */
	public static String INSEE_QUALITY_CODES_BASE_URI = "http://" + (BETA_NAMING ? "beta." : "") + "id.insee.fr/codes/qualite/";
	/** URI for the concept scheme of equipment types */
	public static String INSEE_EQUIPMENT_TYPES_CODELIST_URI = INSEE_CODES_BASE_URI + "territoire/typesEquipements";
	/** URI for the concept scheme of equipment features */
	public static String INSEE_FEATURES_CODELIST_URI = INSEE_CODES_BASE_URI + "territoire/caracteres";
	/** URI for the concept scheme of education sectors */
	public static String INSEE_SECTORS_CODELIST_URI = INSEE_CODES_BASE_URI + "territoire/secteurs";

	/** URI for a collection of specific equipment types */
	public static String inseeEquipmentTypesCollectionURI(Domain domain) {
		return INSEE_EQUIPMENT_TYPES_CODELIST_URI + domain.toCamelCase();
	}
	/** URI for a collection of specific equipment features */
	public static String inseeFeaturesCollectionURI(Domain domain) {
		return INSEE_FEATURES_CODELIST_URI + domain.toCamelCase();
	}
	/** URI for an equipment */
	public static String inseeEquipmentURI(String equipmentCode) { // TODO Introduce domain
		return INSEE_EQUIPMENT_BASE_URI + equipmentCode;
	}
	/** URI for an equipment type */
	public static String inseeEquipmentTypeURI(String typeCode) {
		return INSEE_CODES_BASE_URI + "territoire/typeEquipement/" + typeCode;
	}
	/** URI for an equipment feature */
	public static String inseeFeatureURI(String featureCode) {
		return INSEE_CODES_BASE_URI + "territoire/caractere/" + featureCode;
	}
	/** URI for an education sector */
	public static String inseeSectorURI(String sectorCode) {
		if (!(("PU".equals(sectorCode)) || ("PR".equals(sectorCode)))) return null;
		return INSEE_CODES_BASE_URI + "territoire/secteur/" + sectorCode;
	}
	/** URI for a municipality ('commune') */
	public static String inseeMunicipalityURI(String municipalityCode) {
		return "http://id.insee.fr/geo/commune/" + municipalityCode;
	}
	/** URI for a quality level */
	public static String inseeQualityLevelURI(String levelCode) {
		return INSEE_QUALITY_CODES_BASE_URI + levelCode;
	}
	/** URI for an equipment geometry */
	public static String inseeEquipmentGeometryURI(String equipmentCode) {
		return inseeEquipmentURI(equipmentCode) + "/geometrie";
	}
	/** URI for a quality annotation on geometry */
	public static String inseeGeometryQualityAnnotationURI(String equipmentCode) {
		if (CREATE_GEOMETRY) return inseeEquipmentGeometryURI(equipmentCode) + "/qualite";
		else return inseeEquipmentURI(equipmentCode) + "/qualiteGeometrie";
	}

	/** HACK There is a case of EM character in the main dBase code list (should be a quote), and trailing spaces */
	public static String normalizeString(String input) {

		if (input == null) return null;
		final char ascii0x19 = (byte) 0x41; // End of Medium (EM)
		final char ascii0x27 = (byte) 0x27; // Single quote
		return input.replace(ascii0x19, ascii0x27).trim();
	}

	/** Checks if an equipment is of type 'enseignement' */
	public static boolean isEducation(String equipmentType) {
		return ((equipmentType != null) && (equipmentType.startsWith("C")));
	}

	/** Checks if an equipment is of type 'sport-loisir' */
	public static boolean isSportLeisure(String equipmentType) {
		return ((equipmentType != null) && (equipmentType.startsWith("F")));
	}

	/** Checks if an equipment is of specific type ('enseignement' or 'sport-loisir') */
	public static boolean isSpecific(String equipmentType) {
		return ((equipmentType != null) && (equipmentType.startsWith("C") || equipmentType.startsWith("F")));
	}

	/**
	 * Return the value of the WKT literal representing a point in a given municipality.
	 * 
	 * See example at https://www.w3.org/2015/spatial/wiki/Coordinate_Reference_Systems.
	 */
	public static String getPointWKTLiteral(double x, double y, String municipalityCode) {
		String crs = LAMBERT_93_URI; // Majority of cases
		if (municipalityCode.startsWith("97")) { // There are some equipments in Mayotte, but they are not geolocalized
			if (municipalityCode.startsWith("971")) crs = UTM20N_URI;
			else if (municipalityCode.startsWith("972")) crs = UTM20N_URI;
			else if (municipalityCode.startsWith("973")) crs = UTM22N_URI;
			else if (municipalityCode.startsWith("974")) crs = UTM40S_URI;
		}
		return "<" + crs + "> Point(" + x + " " + y + ")";
	}

	/** Returns the domain of an equipment type */
	public static Domain getDomain(String equipmentType) {
		if (equipmentType == null) return null;
		if (equipmentType.startsWith("C")) return Domain.ENSEIGNEMENT;
		if (equipmentType.startsWith("F")) return Domain.SPORT_LOISIR;
		return Domain.ENSEMBLE;
	}

	/** Enumeration of high-level domains */
	public enum Domain {

	    ENSEMBLE("ensemble"),
	    ENSEIGNEMENT("enseignement"),
	    SPORT_LOISIR("sport_loisir");
	
	    private final String domain;
	
	    Domain(String domain) {
	        this.domain = domain;
	    }

	    public String toCamelCase() {

	    	StringBuilder result = new StringBuilder();
	    	String[] tokens = this.domain.split("_");
	    	for (String token : tokens) result.append(StringUtils.capitalize(token));
	    	return result.toString();
	    }
	}

	/** Enumeration of quality levels */
	public enum QualityLevel {
		
	    BONNE("Bonne", "BON"),
	    ACCEPTABLE("Acceptable", "ACCEPTABLE"),
	    MAUVAISE("Mauvaise", "MAUVAIS");

	    private final String label;
	    private final String code;
	
	    QualityLevel(String level, String code) {
	        this.label = level;
	        this.code = code;
	    }

	    public String toURI() {

	    	return inseeQualityLevelURI(this.code);
	    }

	    public static Map<QualityLevel, Resource> RESOURCE_MAP;
	    static {
	    	RESOURCE_MAP = new HashMap<>();
	    	RESOURCE_MAP.put(MAUVAISE, ResourceFactory.createResource(inseeQualityLevelURI("MAUVAIS")));
	    	RESOURCE_MAP.put(ACCEPTABLE, ResourceFactory.createResource(inseeQualityLevelURI("ACCEPTABLE")));
	    	RESOURCE_MAP.put(BONNE, ResourceFactory.createResource(inseeQualityLevelURI("BON")));
	    }

	    @Override
	    public String toString() {
	        return this.label;
	    }
	}
}
