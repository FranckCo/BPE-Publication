package fr.insee.semweb.bpe;

import com.epam.parso.Column;
import com.epam.parso.SasFileReader;
import com.epam.parso.impl.SasFileReaderImpl;
import fr.insee.semweb.bpe.Configuration.Domain;
import fr.insee.semweb.bpe.Configuration.QualityLevel;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;

/**
 * <code>BPEModelMaker</code> is the base class for model makers specialized by format.
 * 
 * @author Franck
 */
public abstract class BPEModelMaker {

	final static String DEFAULT_FILTER = "E102"; // Will only produce equipment whose type starts with the filter (set to empty for all equipments)

	/**
	 * Creates the BPE model with a custom filter on the type of equipments.
	 * 
	 * @param typeFilter The filter as a predicate on the equipment type code.
	 * @return The BPE extract as a Jena model.
	 * @throws IOException In case of problem reading the database.
	 */
	public Model makeBPEModel(Predicate<String> typeFilter) throws IOException {

		return null;
	}

	/**
	 * Creates the BPE main model with the default filter on the type of equipments.
	 * The default filter is specified by the DEFAULT_FILTER constant.
	 * 
	 * @return The BPE extract as a Jena model.
	 * @throws IOException In case of problem reading the database.
	 */
	public Model makeBPEModel() throws IOException {

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
	public Model makeQualityModel(Predicate<String> typeFilter) throws IOException {

		return null;
	}

	/**
	 * Creates the BPE quality model with the default filter on the type of equipments.
	 * The default filter is specified by the DEFAULT_FILTER constant.
	 * 
	 * @return The BPE quality extract as a Jena model.
	 * @throws IOException In case of problem reading the database.
	 */
	public Model makeQualityModel() throws IOException {

		return makeQualityModel(type -> type.startsWith(DEFAULT_FILTER));
	}
}
