package fr.insee.semweb.bpe;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definition for the BPE vocabulary.
 */
public class BPEOnto {
	/**
	 * The RDF model that holds the BPE ontology entities
	 */
	public static OntModel model = ModelFactory.createOntologyModel();
	/**
	 * The namespace of the BPE vocabulary as a string
	 */
	public static final String uri = "http://rdf.insee.fr/def/bpe#";
	/**
	 * Returns the namespace of the BPE vocabulary as a string.
	 * @return the namespace of the BPE vocabulary
	 */
	public static String getURI() {
		return uri;
	}
	/**
	 * The namespace of the BPE vocabulary
	 */
	public static final Resource NAMESPACE = model.createResource(uri);
	/* ##########################################################
	 * Defines BPE Classes
	   ########################################################## */
	public static final OntClass Equipement = model.createClass(uri + "Equipement");
	public static final OntClass EquipementEnseignement = model.createClass(uri + "EquipementEnseignement");
	public static final OntClass EquipementSportLoisir = model.createClass(uri + "EquipementSportLoisir");
	public static final OntClass TypeEquipement = model.createClass(uri + "TypeEquipement");
	public static final OntClass TypeEquipementEnseignement = model.createClass(uri + "TypeEquipementEnseignement");
	public static final OntClass TypeEquipementSportLoisir = model.createClass(uri + "TypeEquipementSportLoisir");
	public static final OntClass Caractere = model.createClass(uri + "Caractere");
	public static final OntClass CaractereEnseignement = model.createClass(uri + "CaractereEnseignement");
	public static final OntClass CaractereSportLoisir = model.createClass(uri + "CaractereSportLoisir");
	public static final OntClass Secteur = model.createClass(uri + "Secteur");
	/* ##########################################################
	 * Defines BPE Properties
	   ########################################################## */
	// BPE data properties
	public static final DatatypeProperty anneeDescription = model.createDatatypeProperty(uri + "anneeDescription");
	public static final DatatypeProperty capacite = model.createDatatypeProperty(uri + "capacite");
	public static final DatatypeProperty nombreSalles = model.createDatatypeProperty(uri + "nombreSalles");
	// BPE object properties
	public static final ObjectProperty caractereAbsent = model.createObjectProperty(uri + "caractereAbsent");
	public static final ObjectProperty caractereApplicable = model.createObjectProperty(uri + "caractereApplicable");
	public static final ObjectProperty caracterePresent = model.createObjectProperty(uri + "caracterePresent");
	public static final ObjectProperty communeEquipement = model.createObjectProperty(uri + "communeEquipement");
	public static final ObjectProperty qualiteGeometrie = model.createObjectProperty(uri + "qualiteGeometrie");
	public static final ObjectProperty secteurEquipement = model.createObjectProperty(uri + "secteurEquipement");
}