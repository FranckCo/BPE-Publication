package fr.insee.semweb.bpe;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.insee.semweb.bpe.Configuration.Domain;
import fr.insee.semweb.bpe.Configuration.QualityLevel;
import net.iryndin.jdbf.core.DbfRecord;
import net.iryndin.jdbf.reader.DbfReader;

public class DBFModelMaker {

	public static Logger logger = LogManager.getLogger(DBFModelMaker.class);

	/**
	 * Reads the list of equipments (other than specific ones) from the DBF file into a Jena model.
	 * 
	 * @return A Jena <code>Model</code> containing the list of equipments.
	 */
	public static Model makeOtherEquipmentsModel(boolean forceAll) {

		Model equipmentsModel = ModelFactory.createDefaultModel();
		equipmentsModel.setNsPrefix("dcterms", DCTerms.getURI());
		equipmentsModel.setNsPrefix("rdfs", RDFS.getURI());
		equipmentsModel.setNsPrefix("ibpe", BPEOnto.getURI());
		equipmentsModel.setNsPrefix("ibpe-eq", "http://id.insee.fr/territoire/equipement/");
		equipmentsModel.setNsPrefix("icod-teq", "http://id.insee.fr/codes/territoire/typeEquipement/");

		// Read the DBF file to get the list of equipments 
		Path allEquipmentsFilePath = Configuration.getDBFDataFilePath(Domain.ENSEMBLE);
		DbfRecord record = null;
		try {
			InputStream dbf = new FileInputStream(allEquipmentsFilePath.toString());
			try (DbfReader reader = new DbfReader(dbf)) {
				while ((record = reader.read()) != null) {
					if (forceAll || (record.getRecordNumber() % Configuration.SAMPLING_RATE == 0)) {
						// Get the equipment type code and check that it is not in a specific domain
						String typeCode = record.getString("typequ");
						if (Configuration.getDomain(typeCode) != Domain.ENSEMBLE) continue;
						Resource equipmentResource = equipmentsModel.createResource(Configuration.inseeEquipmentURI(String.valueOf(record.getRecordNumber())), BPEOnto.Equipement);
						equipmentResource.addProperty(DCTerms.type, equipmentsModel.createResource(Configuration.inseeEquipmentTypeURI(typeCode)));
					}
				}
			}
			dbf.close();
		} catch (Exception e) {
			logger.error("Error processing record " + record.getRecordNumber() + " - " + e.getMessage());
		}

		return equipmentsModel;
	}

	/**
	 * Reads the list of education equipments from the DBF file into a Jena model.
	 * 
	 * @return A Jena <code>Model</code> containing the list of equipments.
	 * @throws IOException In case of problem reading the data.
	 */
	public static Model makeEductionEquipmentsModel(boolean forceAll) throws IOException {

		// Get the list of features for the education domain (features are in lower case in data files)
		List<String> featureCodes = CodelistModelMaker.getFeaturesList(Domain.ENSEIGNEMENT).stream().map(String::toLowerCase).collect(Collectors.toList());

		Model equipmentsModel = ModelFactory.createDefaultModel();
		equipmentsModel.setNsPrefix("dcterms", DCTerms.getURI());
		equipmentsModel.setNsPrefix("rdfs", RDFS.getURI());
		equipmentsModel.setNsPrefix("ibpe", BPEOnto.getURI());
		equipmentsModel.setNsPrefix("ibpe-eq", "http://id.insee.fr/territoire/equipement/");
		equipmentsModel.setNsPrefix("icod-teq", "http://id.insee.fr/codes/territoire/typeEquipement/");
		equipmentsModel.setNsPrefix("icod-car", "http://id.insee.fr/codes/territoire/caractere/");
		equipmentsModel.setNsPrefix("icod-sec", "http://id.insee.fr/codes/territoire/secteur/");
		equipmentsModel.setNsPrefix("igeo-com", "http://id.insee.fr/geo/commune/");

		// Read the DBF file to get the list of education equipments 
		Path educationEquipmentsFilePath = Configuration.getDBFDataFilePath(Domain.ENSEIGNEMENT);
		DbfRecord record = null;
		try {
			InputStream dbf = new FileInputStream(educationEquipmentsFilePath.toString());
			try (DbfReader reader = new DbfReader(dbf)) {
				while ((record = reader.read()) != null) {
					if (forceAll || (record.getRecordNumber() % Configuration.SAMPLING_RATE == 0)) {
						// Get the equipment type code and check that it is in the education domain
						String typeCode = record.getString("typequ");
						if (Configuration.getDomain(typeCode) != Domain.ENSEIGNEMENT) continue;
						Resource equipmentResource = equipmentsModel.createResource(Configuration.inseeEquipmentURI(String.valueOf(record.getRecordNumber())), BPEOnto.Equipement);
						equipmentResource.addProperty(RDF.type, BPEOnto.EquipementEnseignement);
						equipmentResource.addProperty(DCTerms.type, equipmentsModel.createResource(Configuration.inseeEquipmentTypeURI(typeCode)));
						for (String featureCode : featureCodes) {
							String featureMarker = record.getString(featureCode);
							if (Configuration.featurePresence.containsKey(featureMarker)) {
								equipmentResource.addProperty(Configuration.featurePresence.get(featureMarker), ResourceFactory.createResource(Configuration.inseeFeatureURI(featureCode)));
							}
						}
						// Remaining variables are AN, DCIRIS, DEP, DEPCOM, LAMBERT_X, LAMBERT_Y, QUALITE_XY, REG and SECT
						// Add sector and municipality
						String sectorCode = record.getString("sect");
						if ("PR".equals(sectorCode) || "PU".equals(sectorCode))
							equipmentResource.addProperty(BPEOnto.secteurEquipement, ResourceFactory.createResource(Configuration.inseeSectorURI(sectorCode)));
						String municipalityCode = record.getString("depcom");
						equipmentResource.addProperty(BPEOnto.communeEquipement, ResourceFactory.createResource(Configuration.inseeMunicipalityURI(municipalityCode)));
					}
				}
			}
			dbf.close();
		} catch (Exception e) {
			logger.error("Error processing record " + record.getRecordNumber() + " - " + e.getMessage());
		}

		return equipmentsModel;
	}

	/**
	 * Reads the list of sports and leisure equipments from the DBF file into a Jena model.
	 * 
	 * @return A Jena <code>Model</code> containing the list of equipments.
	 * @throws IOException In case of problem reading the data.
	 */
	public static Model makeSportsLeisureEquipmentsModel(boolean forceAll) throws IOException {

		// Get the list of features for the sports and leisure domain (features are in lower case in data files)
		List<String> featureCodes = CodelistModelMaker.getFeaturesList(Domain.SPORT_LOISIR).stream().map(String::toLowerCase).collect(Collectors.toList());

		Model equipmentsModel = ModelFactory.createDefaultModel();
		equipmentsModel.setNsPrefix("dcterms", DCTerms.getURI());
		equipmentsModel.setNsPrefix("rdfs", RDFS.getURI());
		equipmentsModel.setNsPrefix("ibpe", BPEOnto.getURI());
		equipmentsModel.setNsPrefix("ibpe-eq", "http://id.insee.fr/territoire/equipement/");
		equipmentsModel.setNsPrefix("icod-teq", "http://id.insee.fr/codes/territoire/typeEquipement/");
		equipmentsModel.setNsPrefix("icod-car", "http://id.insee.fr/codes/territoire/caractere/");
		equipmentsModel.setNsPrefix("igeo-com", "http://id.insee.fr/geo/commune/");

		// Read the DBF file to get the list of education equipments 
		Path sportLeisureEquipmentsFilePath = Configuration.getDBFDataFilePath(Domain.SPORT_LOISIR);
		DbfRecord record = null;
		try {
			InputStream dbf = new FileInputStream(sportLeisureEquipmentsFilePath.toString());
			try (DbfReader reader = new DbfReader(dbf)) {
				while ((record = reader.read()) != null) {
					if (forceAll || (record.getRecordNumber() % Configuration.SAMPLING_RATE == 0)) {
						// Get the equipment type code and check that it is in the education domain
						String typeCode = record.getString("typequ");
						if (Configuration.getDomain(typeCode) != Domain.SPORT_LOISIR) continue;
						Resource equipmentResource = equipmentsModel.createResource(Configuration.inseeEquipmentURI(String.valueOf(record.getRecordNumber())), BPEOnto.Equipement);
						equipmentResource.addProperty(RDF.type, BPEOnto.EquipementSportLoisir);
						equipmentResource.addProperty(DCTerms.type, equipmentsModel.createResource(Configuration.inseeEquipmentTypeURI(typeCode)));
						for (String featureCode : featureCodes) {
							String featureMarker = record.getString(featureCode);
							if (Configuration.featurePresence.containsKey(featureMarker)) {
								equipmentResource.addProperty(Configuration.featurePresence.get(featureMarker), ResourceFactory.createResource(Configuration.inseeFeatureURI(featureCode)));
							}
						}
						// TODO Add nb_aire_je and nb_salle (check that not null)
						// Remaining variables are AN, DCIRIS, DEP, DEPCOM, LAMBERT_X, LAMBERT_Y, QUALITE_XY, REG and SECT
						// Add municipality
						String municipalityCode = record.getString("depcom");
						equipmentResource.addProperty(BPEOnto.communeEquipement, ResourceFactory.createResource(Configuration.inseeMunicipalityURI(municipalityCode)));
					}
				}
			}
			dbf.close();
		} catch (Exception e) {
			logger.error("Error processing record " + record.getRecordNumber() + " - " + e.getMessage());
		}

		return equipmentsModel;
	}

	/**
	 * Reads the quality information in a given domain from the DBF file and creates quality annotations into a Jena model.
	 * Depending on the configuration, quality information can be simple links to quality level codes, or full-fledged DQV annotations.
	 * Also depending on the configuration, quality information is attached to the equipment or to the geometry.
	 * 
	 * @return A Jena <code>Model</code> containing the quality information.
	 */
	public static Model makeQualityModel(Domain domain, boolean forceAll) {

		Model qualityModel = ModelFactory.createDefaultModel();
		if (!Configuration.CREATE_GEOMETRY) qualityModel.setNsPrefix("ibpe-eq", "http://id.insee.fr/territoire/equipement/");
		qualityModel.setNsPrefix("icod-qlt", "http://id.insee.fr/codes/qualite/");
		if (Configuration.QUALITY_ANNOTATIONS) {
			qualityModel.setNsPrefix("oa", Annotations.getURI());
			qualityModel.setNsPrefix("dqv", DQV.getURI());
		} else qualityModel.setNsPrefix("ibpe", BPEOnto.getURI());

		// Read the DBF file to get the list of equipments 
		Path equipmentsFilePath = Configuration.getDBFDataFilePath(domain);
		DbfRecord record = null;
		try {
			InputStream dbf = new FileInputStream(equipmentsFilePath.toString());
			try (DbfReader reader = new DbfReader(dbf)) {
				while ((record = reader.read()) != null) {
					if (forceAll || (record.getRecordNumber() % Configuration.SAMPLING_RATE == 0)) {
						// Get the value of the quality level
						QualityLevel qualityLevelValue = QualityLevel.valueOf(record.getString("qualite_xy").toUpperCase());
						if (qualityLevelValue == null) continue; // No quality information for this record
						// Get the equipment type code and check that it is not in a specific domain
						String typeCode = record.getString("typequ");
						if ((domain == Domain.ENSEMBLE) && (Configuration.isSpecific(typeCode))) continue; // Specific equipments will be treated separately
						// Create the resource to which quality information will be attached
						Resource targetResource = null;
						String equipmentCode = String.valueOf(record.getRecordNumber());
						if (Configuration.CREATE_GEOMETRY) targetResource = qualityModel.createResource(Configuration.inseeEquipmentGeometryURI(equipmentCode));
						else targetResource = qualityModel.createResource(Configuration.inseeEquipmentURI(equipmentCode));
						Resource qualityLevelResource = qualityModel.createResource(qualityLevelValue.getURI());
						if (Configuration.QUALITY_ANNOTATIONS) {
							// Create annotation instance
							Resource qualityAnnotationResource = qualityModel.createResource(Configuration.inseeGeometryQualityAnnotationURI(equipmentCode), DQV.QualityAnnotation);
							targetResource.addProperty(DQV.hasQualityAnnotation, qualityAnnotationResource);
							qualityAnnotationResource.addProperty(Annotations.hasTarget, targetResource);
							qualityAnnotationResource.addProperty(Annotations.hasBody, qualityLevelResource);
							qualityAnnotationResource.addProperty(Annotations.motivatedBy, DQV.qualityAssessment);
						} else { // Simpler model: direct predicate between target and quality level
							targetResource.addProperty(BPEOnto.qualiteGeometrie, qualityLevelResource);
						}
					}
				}
			}
			dbf.close();
		} catch (Exception e) {
			logger.error("Error processing record " + record.getRecordNumber() + " - " + e.getMessage());
		}

		return qualityModel;
	}

}