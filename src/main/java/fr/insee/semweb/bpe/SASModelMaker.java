package fr.insee.semweb.bpe;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
 * Creates the BPE Jena model from the SAS file and saves the model as Turtle.
 * 
 * @author Franck
 */
public class SASModelMaker {

	public static Logger logger = LogManager.getLogger(SASModelMaker.class);

	static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	final static Long LINES_TO_READ = 0L; // Zero means read all lines
	final static int LOGGING_STEP = 10000;
	final static String filter = "E102"; // Will only produce equipment whose type starts with the filter (set to empty for all equipments)

	public static Model makeBPEModel() throws IOException {

		SasFileReader sasFileReader = new SasFileReaderImpl(new FileInputStream(Configuration.getSASDataFilePath().toString()));
		// Build the map of column indexes
		Map<String, Integer> colIndexes = new HashMap<String, Integer>();
		int index = 0;
		for (Column column : sasFileReader.getColumns()) colIndexes.put(column.getName().toLowerCase(), index++);

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
			// Create equipment resource with relevant types
			String equipmentType = values[colIndexes.get("typequ")].toString().trim();
			// Test conformance to filter
			if (!equipmentType.startsWith(filter)) continue;

			Resource equipmentResource = bpeModel.createResource(Configuration.inseeEquipmentURI(String.valueOf(equipmentId)), BPEOnto.Equipement);
			equipmentResource.addProperty(DCTerms.type, bpeModel.createResource(Configuration.inseeEquipmentTypeURI(equipmentType)));
			Domain equipmentDomain = Configuration.getDomain(equipmentType);
			if (equipmentDomain == Domain.ENSEIGNEMENT) {
				equipmentResource.addProperty(RDF.type, BPEOnto.EquipementEnseignement);
			} else if (equipmentDomain == Domain.SPORT_LOISIR) {
				equipmentResource.addProperty(RDF.type, BPEOnto.EquipementSportLoisir);
			}
			// Add features for equipments of specific domains
			for (String feature : Configuration.sasFeatures.get(equipmentDomain)) {
				Object featureValue = values[colIndexes.get(feature)];
				if (featureValue == null) {
					logger.warn("Null value for feature " + feature + " for equipment " + equipmentId + " of type " + equipmentType);
					continue;
				}
				String featureMarker = featureValue.toString(); // For features, value object type is actually String
				if (Configuration.featurePresence.containsKey(featureMarker)) {
					equipmentResource.addProperty(Configuration.featurePresence.get(featureMarker), ResourceFactory.createResource(Configuration.inseeFeatureURI(feature)));
				}
			}
			// Add sector and municipality (type java.util.String)
			Object sectorValue = values[colIndexes.get("sect")];
			if ((sectorValue != null) && !sectorValue.toString().equals("X")) { // Null does note actually happen in 2018 file
				equipmentResource.addProperty(BPEOnto.secteurEquipement, ResourceFactory.createResource(Configuration.inseeSectorURI(sectorValue.toString())));
			}
			String municipalityCode = values[colIndexes.get("depcom")].toString();
			equipmentResource.addProperty(BPEOnto.communeEquipement, ResourceFactory.createResource(Configuration.inseeMunicipalityURI(municipalityCode)));
			// Add creation date (type java.util.Date)
			Date dateValue = (Date) values[colIndexes.get("date_creation")];
			if (dateValue != null) {
				equipmentResource.addProperty(DCTerms.created, bpeModel.createTypedLiteral(dateFormat.format(dateValue), XSDDatatype.XSDdate));
			}
			// Add capacity (type java.lang.Long)
			Long capacityValue = (Long) values[colIndexes.get("capacite")];
			if ((capacityValue != null) && (capacityValue > 0)) {
				equipmentResource.addProperty(BPEOnto.capacite, bpeModel.createTypedLiteral(capacityValue.intValue(), XSDDatatype.XSDint));
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
					String wktLiteral = Configuration.getPointWKTLiteral(xLambertDouble, yLambertDouble, Configuration.LAMBERT_93_URI);
					geometryResource.addProperty(GeoSPARQL.asWKT, bpeModel.createTypedLiteral(wktLiteral, GeoSPARQL.wktLiteral));
					equipmentResource.addProperty(GeoSPARQL.hasGeometry, geometryResource);
				}
				else {} // What do we do in that case?
			}
			;
			if (++equipmentCreated % LOGGING_STEP == 1) logger.debug("Just created equipment number " + equipmentCreated);
		}
		logger.info(equipmentCreated + " created");
		return bpeModel;
	}

	// See https://www.w3.org/TR/vocab-dqv/#expressQualityClassification
	public static Model makeQualityModel() throws IOException {

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
			Object[] values = sasFileReader.readNext();
			// Get the value of the quality level
			QualityLevel qualityLevelValue = null;
			try {
				qualityLevelValue = QualityLevel.valueOf(values[colIndexes.get("qualite_xy")].toString().trim().toUpperCase());
			} catch (IllegalArgumentException e) {
				// Equipment is not geolocalized or quality value is invalid
				continue;
			}
			System.out.println(qualityLevelValue);
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
			qualityAnnotationResource.addProperty(Annotations.hasBody, qualityModel.createResource(qualityLevelValue.toURI()));
			qualityAnnotationResource.addProperty(Annotations.motivatedBy, DQV.qualityAssessment);
		}

		return qualityModel;
	}
}
