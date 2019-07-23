package fr.insee.semweb.bpe;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Predicate;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.epam.parso.Column;
import com.epam.parso.SasFileReader;
import com.epam.parso.impl.SasFileReaderImpl;

import fr.insee.semweb.bpe.Configuration.Domain;
import fr.insee.semweb.bpe.Configuration.QualityLevel;

/**
 * Creates the BPE data and quality Jena models from the SAS database.
 * 
 * @author Franck
 */
public class SASModelMaker {

	public static Logger logger = LogManager.getLogger(SASModelMaker.class);

	static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	final static Long LINES_TO_READ = 0L; // Zero means read all lines
	final static int LOGGING_STEP = 10000; // Should be strictly positive
	final static String DEFAULT_FILTER = "E102"; // Will only produce equipment whose type starts with the filter (set to empty for all equipments)

	/**
	 * Creates the BPE model with a custom filter on the type of equipments.
	 * 
	 * @param typeFilter The filter as a predicate on the equipment type code.
	 * @return The BPE extract as a Jena model.
	 * @throws IOException In case of problem reading the database.
	 */
	public static Model makeBPEModel(Predicate<String> typeFilter) throws IOException {

		SasFileReader sasFileReader = new SasFileReaderImpl(new FileInputStream(Configuration.getSASDataFilePath().toString()));
		// Build the map of column indexes
		Map<String, Integer> colIndexes = new HashMap<String, Integer>();
		int index = 0;
		for (Column column : sasFileReader.getColumns()) colIndexes.put(column.getName().toLowerCase(), index++);

		// Read the list of columns to process for each type of equipment
		SortedMap<String, SortedSet<String>> featuresAndPropertiesByType = SASUtils.listFeaturesAndPropertiesByType();

		Model bpeModel = ModelFactory.createDefaultModel();
		bpeModel.setNsPrefix("dcterms", DCTerms.getURI());
		bpeModel.setNsPrefix("rdfs", RDFS.getURI());
		bpeModel.setNsPrefix("ibpe", BPEOnto.getURI());
		bpeModel.setNsPrefix("xsd", XSD.getURI());
		bpeModel.setNsPrefix("ibpe-eq", "http://id.insee.fr/territoire/equipement/");
		bpeModel.setNsPrefix("icod-teq", "http://id.insee.fr/codes/territoire/typeEquipement/");
		bpeModel.setNsPrefix("icod-car", "http://id.insee.fr/codes/territoire/caractere/");
		bpeModel.setNsPrefix("icod-sec", "http://id.insee.fr/codes/territoire/secteur/");
		bpeModel.setNsPrefix("igeo-com", "http://id.insee.fr/geo/commune/");
		if (Configuration.CREATE_GEOMETRY) bpeModel.setNsPrefix("geo", GeoSPARQL.getURI());

		long linesToRead = sasFileReader.getSasFileProperties().getRowCount();
		if ((LINES_TO_READ > 0) && (LINES_TO_READ < linesToRead)) linesToRead = LINES_TO_READ;
		long equipmentCreated = 0L;
		logger.debug("Reading " + linesToRead + " lines from " + Configuration.getSASDataFilePath().toString() + " to create BPE model");
		for (long line = 0; line < linesToRead; line++) {
			if (line % LOGGING_STEP == 1) logger.debug("About to process line number " + line);
			Object[] values = sasFileReader.readNext();
			// Equipment identifier is first column + second column
			String equipmentId = values[colIndexes.get("idetab")].toString().trim() + values[colIndexes.get("idservice")].toString().trim();
			// Read equipment type and test conformance to filter predicate
			String equipmentType = values[colIndexes.get("typequ")].toString().trim();
			if (!typeFilter.test(equipmentType)) continue;
 
			// Create equipment resource with relevant types
			Resource equipmentResource = bpeModel.createResource(Configuration.inseeEquipmentURI(String.valueOf(equipmentId)), BPEOnto.Equipement);
			equipmentResource.addProperty(DCTerms.type, bpeModel.createResource(Configuration.inseeEquipmentTypeURI(equipmentType)));
			Domain equipmentDomain = Configuration.getDomain(equipmentType);
			if (equipmentDomain == Domain.ENSEIGNEMENT) {
				equipmentResource.addProperty(RDF.type, BPEOnto.EquipementEnseignement);
			} else if (equipmentDomain == Domain.SPORT_LOISIR) {
				equipmentResource.addProperty(RDF.type, BPEOnto.EquipementSportLoisir);
			}
			// Add general properties (municipality code, creation date)
			String municipalityCode = values[colIndexes.get("depcom")].toString();
			equipmentResource.addProperty(BPEOnto.communeEquipement, ResourceFactory.createResource(Configuration.inseeMunicipalityURI(municipalityCode)));
			// Add creation date (type java.util.Date)
			Date dateValue = (Date) values[colIndexes.get("date_creation")];
			if (dateValue != null) {
				equipmentResource.addProperty(DCTerms.created, bpeModel.createTypedLiteral(dateFormat.format(dateValue), XSDDatatype.XSDdate));
			}
			// Add specialized properties and features for equipments of specific domains
			SortedSet<String> featuresAndProperties = featuresAndPropertiesByType.get(equipmentType);
			if (featuresAndProperties != null) { // Would be null if no specialized features or properties for this type
				for (String column : featuresAndProperties) {
					if (column == null) continue; // No specialize
					Object columnValue = values[colIndexes.get(column)];
					if (columnValue == null) {
						// Data checks indicate that this does not happen for 2018
						logger.warn("Null value of column " + column + " for equipment " + equipmentId + " of type " + equipmentType);
						continue;
					}
					if (Configuration.sasFeatures.get(equipmentDomain).contains(column)) {
						// Case of a feature: value is 0 or 1
						String featureMarker = columnValue.toString(); // For features, value object type is actually String
						if (Configuration.featurePresence.containsKey(featureMarker)) {
							equipmentResource.addProperty(Configuration.featurePresence.get(featureMarker), ResourceFactory.createResource(Configuration.inseeFeatureURI(column)));
						}
					} else { // Case of a specialized property (capacity, sector, number of screens...)
						if ("sect".equals(column)) {
							// Add sector
							String sectorURI = Configuration.inseeSectorURI(columnValue.toString());
							if (sectorURI == null) { // Sector should be "PU" or "PR"
								logger.warn("Invalid sector value " + columnValue.toString() + " for equipment " + equipmentId + " of type " + equipmentType);
							} else {
								equipmentResource.addProperty(BPEOnto.secteurEquipement, ResourceFactory.createResource(sectorURI));
							}
						}
						if ("capacite".equals(column)) {
							// Add capacity (type java.lang.Long)
							Long capacity = (Long) columnValue;
							if ((capacity != null) && (capacity > 0)) {
								equipmentResource.addProperty(BPEOnto.capacite, bpeModel.createTypedLiteral(capacity.intValue(), XSDDatatype.XSDint));
							}
						}
						if ("nbsalles".equals(column)) {
							// Add screen/stage number (type java.lang.Long)
							Long rooms = (Long) columnValue;
							if ((rooms != null) && (rooms > 0)) {
								equipmentResource.addProperty(BPEOnto.nombreSalles, bpeModel.createTypedLiteral(rooms.intValue(), XSDDatatype.XSDint));
							}
						}
					}
				}
			}

			// Add geometry (lambert_x and lambert_y are of type java.lang.Double, or java.lang.Long if no decimal)
			Object xLambert = values[colIndexes.get("lambert_x")];
			Object yLambert = values[colIndexes.get("lambert_y")];
			if ((xLambert != null) && (yLambert != null)) {
				double xLambertDouble = (xLambert instanceof Double) ? (Double) xLambert : (Long) xLambert; // Harmonize to double
				double yLambertDouble = (yLambert instanceof Double) ? (Double) yLambert : (Long) yLambert;
				// If creation of GeoSPARQL geometries is required, create the corresponding resource
				if (Configuration.CREATE_GEOMETRY) {
					equipmentResource.addProperty(RDF.type, GeoSPARQL.SpatialObject);
					Resource geometryResource = bpeModel.createResource(Configuration.inseeEquipmentGeometryURI(equipmentId), GeoSPARQL.Geometry);
					String wktLiteral = Configuration.getPointWKTLiteral(xLambertDouble, yLambertDouble, municipalityCode);
					geometryResource.addProperty(GeoSPARQL.asWKT, bpeModel.createTypedLiteral(wktLiteral, GeoSPARQL.wktLiteral));
					equipmentResource.addProperty(GeoSPARQL.hasGeometry, geometryResource);
				}
				else {} // For now, we don't do anything in that case
			}
			if (++equipmentCreated % LOGGING_STEP == 1) logger.debug("Just created equipment number " + equipmentCreated);
		}
		logger.info(equipmentCreated + " equipments created, the model contains " + bpeModel.size() + " triples");
		return bpeModel;
	}

	/**
	 * Creates the BPE main model with the default filter on the type of equipments.
	 * The default filter is specified by the DEFAULT_FILTER constant.
	 * 
	 * @return The BPE extract as a Jena model.
	 * @throws IOException In case of problem reading the database.
	 */
	public static Model makeBPEModel() throws IOException {

		return makeBPEModel(type -> type.startsWith(DEFAULT_FILTER));
	}


	/**
	 * Creates the BPE quality model with a custom filter on the type of equipments.
	 * See https://www.w3.org/TR/vocab-dqv/#expressQualityClassification.
	 * 
	 * @param typeFilter The filter as a predicate on the equipment type code.
	 * @return The BPE quality extract as a Jena model.
	 * @throws IOException In case of problem reading the database.
	 */
	public static Model makeQualityModel(Predicate<String> typeFilter) throws IOException {

		SasFileReader sasFileReader = new SasFileReaderImpl(new FileInputStream(Configuration.getSASDataFilePath().toString()));
		// Build the map of column indexes
		Map<String, Integer> colIndexes = new HashMap<String, Integer>();
		int index = 0;
		for (Column column : sasFileReader.getColumns()) colIndexes.put(column.getName().toLowerCase(), index++);

		Model qualityModel = ModelFactory.createDefaultModel();
		if (!Configuration.CREATE_GEOMETRY) qualityModel.setNsPrefix("ibpe-eq", "http://id.insee.fr/territoire/equipement/");
		qualityModel.setNsPrefix("icod-qlt", "http://id.insee.fr/codes/qualite/");
		qualityModel.setNsPrefix("oa", Annotations.getURI());
		qualityModel.setNsPrefix("dqv", DQV.getURI());

		long linesToRead = sasFileReader.getSasFileProperties().getRowCount();
		if ((LINES_TO_READ > 0) && (LINES_TO_READ < linesToRead)) linesToRead = LINES_TO_READ;
		logger.debug("Reading " + linesToRead + " lines from " + Configuration.getSASDataFilePath().toString() + " to create quality model");
		for (int line = 0; line < linesToRead; line++) {
			if (line % LOGGING_STEP == 1) logger.debug("About to process line number " + line);
			Object[] values = sasFileReader.readNext();
			// First apply the filter on equipment type
			String equipmentType = values[colIndexes.get("typequ")].toString().trim();
			if (!typeFilter.test(equipmentType)) continue;
			// Get the value of the quality level
			QualityLevel qualityLevelValue = null;
			try {
				qualityLevelValue = QualityLevel.valueOf(values[colIndexes.get("qualite_xy")].toString().trim().toUpperCase());
			} catch (IllegalArgumentException e) {
				// Equipment is not geolocalized or quality value is invalid
				continue;
			}
			if (qualityLevelValue == null) continue; // No quality information for this record
			// Equipment identifier is first column + second column
			String equipmentId = values[colIndexes.get("idetab")].toString().trim() + values[colIndexes.get("idservice")].toString().trim();
			// The quality annotation target is the equipment or the geometry itself
			Resource targetResource = null;
			if (Configuration.CREATE_GEOMETRY) targetResource = qualityModel.createResource(Configuration.inseeEquipmentGeometryURI(equipmentId));
			else targetResource = qualityModel.createResource(Configuration.inseeEquipmentURI(equipmentId));
			// Create annotation instance
			Resource qualityAnnotationResource = qualityModel.createResource(Configuration.inseeGeometryQualityAnnotationURI(equipmentId), DQV.QualityAnnotation);
			targetResource.addProperty(DQV.hasQualityAnnotation, qualityAnnotationResource);
			qualityAnnotationResource.addProperty(Annotations.hasTarget, targetResource);
			qualityAnnotationResource.addProperty(Annotations.hasBody, QualityLevel.RESOURCE_MAP.get(qualityLevelValue));
			qualityAnnotationResource.addProperty(Annotations.motivatedBy, DQV.qualityAssessment);
		}

		return qualityModel;
	}

	/**
	 * Creates the BPE quality model with the default filter on the type of equipments.
	 * The default filter is specified by the DEFAULT_FILTER constant.
	 * 
	 * @return The BPE quality extract as a Jena model.
	 * @throws IOException In case of problem reading the database.
	 */
	public static Model makeQualityModel() throws IOException {

		return makeQualityModel(type -> type.startsWith(DEFAULT_FILTER));
	}
}
