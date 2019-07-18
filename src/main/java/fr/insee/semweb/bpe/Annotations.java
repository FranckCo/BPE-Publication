package fr.insee.semweb.bpe;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definition for the <a href="https://www.w3.org/TR/annotation-vocab/">Web Annotation Vocabulary</a>.
 * 
 */
public class Annotations {
	/**
	 * The RDF model that holds the Annotations entities
	 */
	private static Model model = ModelFactory.createDefaultModel();
	/**
	 * The namespace of the Annotations vocabulary as a string
	 */
	public static final String uri = "http://www.w3.org/ns/oa#";
	/**
	 * Returns the namespace of the Annotations vocabulary as a string
	 * @return the namespace of the Annotations vocabulary
	 */
	public static String getURI() {
		return uri;
	}
	/**
	 * The namespace of the Annotations vocabulary
	 */
	public static final Resource NAMESPACE = model.createResource(uri);
	/* ##########################################################
	 * Defines Annotations Classes
	   ########################################################## */

	/* ##########################################################
	 * Defines Annotations Properties
	   ########################################################## */

	public static final Property hasBody = model.createProperty(uri + "hasBody");
	public static final Property hasTarget = model.createProperty(uri + "hasTarget");
	public static final Property motivatedBy = model.createProperty(uri + "motivatedBy");
}