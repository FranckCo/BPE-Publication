package fr.insee.semweb.bpe;

import com.epam.parso.CSVDataWriter;
import com.epam.parso.Column;
import com.epam.parso.SasFileProperties;
import com.epam.parso.SasFileReader;
import com.epam.parso.impl.CSVDataWriterImpl;
import com.epam.parso.impl.SasFileReaderImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Performs counts and checks on the BPE SAS database.
 * 
 * @author Franck
 */
public class SASUtils {

	public static Logger logger = LogManager.getLogger(SASUtils.class);

	/**
	 * Checks that the values of columns for expected features and properties are not null.
	 * 
	 * @throws IOException In case of problem reading the database.
	 */
	public static void checkFeaturesAndProperties() throws IOException {

		SortedMap<String, SortedSet<String>> featuresAndPropertiesByType = Configuration.listFeaturesAndPropertiesByType();

		SasFileReader sasFileReader = new SasFileReaderImpl(new FileInputStream(Configuration.getSASDataFilePath().toString()));
		// Build the map of column indexes
		Map<String, Integer> colIndexes = new HashMap<>();
		int index = 0;
		for (Column column : sasFileReader.getColumns()) colIndexes.put(column.getName().toLowerCase(), index++);
		long linesToRead = sasFileReader.getSasFileProperties().getRowCount();
		for (long line = 0; line < linesToRead; line++) {
			Object[] values = sasFileReader.readNext();
			// Equipment identifier is first column + second column
			String equipmentId = values[colIndexes.get("idetab")].toString().trim() + values[colIndexes.get("idservice")].toString().trim();
			// Read equipment type
			String equipmentType = values[colIndexes.get("typequ")].toString().trim();
			// Expected features and properties for this type of equipment
			if (featuresAndPropertiesByType.containsKey(equipmentType)) {
				for (String column : featuresAndPropertiesByType.get(equipmentType)) {
					Object columnValue = values[colIndexes.get(column)];
					if (columnValue == null) {
						logger.warn("Null value for column " + column + " for equipment " + equipmentId + " of type " + equipmentType);
					}
				}
			}
		}
	}

	/**
	 * Returns the list of columns in the SAS database.
	 * 
	 * @return A map indexed by column numbers, each value being a <code>Column</code> object.
	 * @throws IOException In case of problem reading the database.
	 */
	public static Map<Integer, Column> listColumns() throws IOException {

		SasFileReader sasFileReader = new SasFileReaderImpl(new FileInputStream(Configuration.getSASDataFilePath().toString()));
		// Build the map of columns by indexes
		Map<Integer, Column> columns = new HashMap<>();
		int index = 0;
		for (Column column : sasFileReader.getColumns()) columns.put(index++, column);

		return columns;
	}

	/**
	 * Checks the values of the municipality codes (especially overseas).
	 * 
	 * @throws IOException In case of problem reading the database.
	 */
	public static void checkMunicipalityCodes() throws IOException {

		SasFileReader sasFileReader = new SasFileReaderImpl(new FileInputStream(Configuration.getSASDataFilePath().toString()));
		// Build the map of column indexes
		Map<String, Integer> colIndexes = new HashMap<>();
		int index = 0;
		for (Column column : sasFileReader.getColumns()) colIndexes.put(column.getName().toLowerCase(), index++);
		long linesToRead = sasFileReader.getSasFileProperties().getRowCount();
		for (long line = 0; line < linesToRead; line++) {
			Object[] values = sasFileReader.readNext();
			// Equipment identifier is first column + second column
			String equipmentId = values[colIndexes.get("idetab")].toString().trim() + values[colIndexes.get("idservice")].toString().trim();
			// Read equipment type
			String equipmentType = values[colIndexes.get("typequ")].toString().trim();
			// Municipality code
			String municipalityCode = values[colIndexes.get("depcom")].toString().trim();
			if ((municipalityCode == null) || (municipalityCode.length() != 5)) System.out.println("Invalid municipality code " + municipalityCode + " for equipment " + equipmentId + " of type " + equipmentType);
			else {
				if (municipalityCode.startsWith("97")) {
					if ("1234".indexOf(municipalityCode.charAt(2)) < 0) {
						// There are actually some equipments from Mayotte, let us check they are not geolocalized
						if (municipalityCode.startsWith("976")) {
							Object xLambert = values[colIndexes.get("lambert_x")];
							Object yLambert = values[colIndexes.get("lambert_y")];
							if ((xLambert != null) && (yLambert != null)) System.out.println("Equipment " + equipmentId + " of type " + equipmentType + " in Mayotte has coordinates");
						} else System.out.println("Invalid municipality code " + municipalityCode + " for equipment " + equipmentId + " of type " + equipmentType);
					}
				}
			}
		}
	}

	/**
	 * Returns the number of equipments of each type.
	 * 
	 * @return A map indexed by equipment types, each value being the number of equipments of this type.
	 * @throws IOException In case of problem reading the database.
	 */
	public static SortedMap<String, Integer> countEquipmentsByType() throws IOException {

		SasFileReader sasFileReader = new SasFileReaderImpl(new FileInputStream(Configuration.getSASDataFilePath().toString()));
		// Build the map of column indexes
		Map<String, Integer> colIndexes = new HashMap<>();
		int index = 0;
		for (Column column : sasFileReader.getColumns()) colIndexes.put(column.getName().toLowerCase(), index++);

		SortedMap<String, Integer> countings = new TreeMap<>();

		long linesToRead = sasFileReader.getSasFileProperties().getRowCount();
		logger.debug("There are " + linesToRead + " lines in " + Configuration.getSASDataFilePath());
		for (long line = 0; line < linesToRead; line++) {
			Object[] values = sasFileReader.readNext();
			// Equipment identifier is first column + second column
			String equipmentType = values[colIndexes.get("typequ")].toString().trim();
			countings.putIfAbsent(equipmentType, 0);
			countings.put(equipmentType, countings.get(equipmentType) + 1);
		}

		return countings;
	}

	/**
	 * Aggregates counts of equipments by type according to the first characters of the type.
	 * 
	 * @param counts The numbers of equipments of each type (see above).
	 * @param keyLength The length of the aggregation keys.
	 * @return A map indexed by aggregation keys, each value being the aggregated number of equipments for this key.
	 */
	public static SortedMap<String, Integer> aggregateCounts(Map<String, Integer> counts, int keyLength) {

		SortedMap<String, Integer> aggregates = new TreeMap<>();

		for (String key : counts.keySet()) {
			String aggregateKey = (keyLength == 0) ? "Total" : key.substring(0, keyLength);
			aggregates.putIfAbsent(aggregateKey, 0);
			aggregates.put(aggregateKey, aggregates.get(aggregateKey) + counts.get(key));
		}

		return aggregates;
	}

	/**
	 * Utility function to export the SAS file as CSV.
	 * 
	 * @throws IOException In case of problem reading SAS file or writing CSV file.
	 */
	public static void convertToCSV() throws IOException {
	
		final String CSV_FILE_NAME = "src/main/resources/data/bpe.csv";
	
		InputStream sasStream = new FileInputStream(Configuration.getSASDataFilePath().toString());
		SasFileReader sasFileReader = new SasFileReaderImpl(sasStream);
	
		SasFileProperties sasFileProperties = sasFileReader.getSasFileProperties();
		long rowCount = sasFileProperties.getRowCount();
		SASModelMaker.logger.info("Number of lines in dataset: " + rowCount);
		SASModelMaker.logger.info("Character set: " + sasFileProperties.getEncoding());
	
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_FILE_NAME), StandardCharsets.UTF_8));
		CSVDataWriter csvDataWriter = new CSVDataWriterImpl(writer);
		csvDataWriter.writeColumnNames(sasFileReader.getColumns());
		int linesToRead = (int) Math.min(rowCount, SASModelMaker.LINES_TO_READ);
		for (int i = 0; i < linesToRead; i++) {
			csvDataWriter.writeRow(sasFileReader.getColumns(), sasFileReader.readNext());			
		}
		writer.close();
	}
}