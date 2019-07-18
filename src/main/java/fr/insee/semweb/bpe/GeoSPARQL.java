package fr.insee.semweb.bpe;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definition for the GeoSPARQL vocabulary.
 */
public class GeoSPARQL {
	/**
	 * The RDF model that holds the GeoSPARQL ontology entities
	 */
	public static OntModel model = ModelFactory.createOntologyModel();
	/**
	 * The namespace of the GeoSPARQL vocabulary as a string
	 */
	public static final String uri = "http://www.opengis.net/ont/geosparql#";
	/**
	 * Returns the namespace of the GeoSPARQL vocabulary as a string.
	 * @return the namespace of the GeoSPARQL vocabulary.
	 */
	public static String getURI() {
		return uri;
	}
	/**
	 * The namespace of the GeoSPARQL vocabulary
	 */
	public static final Resource NAMESPACE = model.createResource(uri);
	/* ##########################################################
	 * Defines GeoSPARQL Classes
	   ########################################################## */
	public static final OntClass SpatialObject = model.createClass(uri + "SpatialObject");
	public static final OntClass Feature = model.createClass(uri + "Feature");
	public static final OntClass Geometry = model.createClass(uri + "Geometry");

	/* ##########################################################
	 * Defines GeoSPARQL Properties
	   ########################################################## */
	// BPE data properties
	public static final DatatypeProperty dimension = model.createDatatypeProperty(uri + "dimension");
	public static final DatatypeProperty coordinateDimension = model.createDatatypeProperty(uri + "coordinateDimension");
	public static final DatatypeProperty spatialDimension = model.createDatatypeProperty(uri + "spatialDimension");
	public static final DatatypeProperty isEmpty = model.createDatatypeProperty(uri + "isEmpty");
	public static final DatatypeProperty isSimple = model.createDatatypeProperty(uri + "isSimple");
	public static final DatatypeProperty hasSerialization = model.createDatatypeProperty(uri + "hasSerialization");
	public static final DatatypeProperty asWKT = model.createDatatypeProperty(uri + "asWKT");
	public static final DatatypeProperty asGML = model.createDatatypeProperty(uri + "asGML");
	// BPE object properties
	public static final ObjectProperty hasGeometry = model.createObjectProperty(uri + "hasGeometry");
	public static final ObjectProperty hasDefaultGeometry = model.createObjectProperty(uri + "hasDefaultGeometry");

	/* ##########################################################
	 * Defines GeoSPARQL Datatypes
	   ########################################################## */
	public static final BaseDatatype wktLiteral = new BaseDatatype(uri + "wktLiteral");
	public static final BaseDatatype gmlLiteral = new BaseDatatype(uri + "gmlLiteral");
}