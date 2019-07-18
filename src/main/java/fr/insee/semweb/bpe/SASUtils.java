package fr.insee.semweb.bpe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.epam.parso.CSVDataWriter;
import com.epam.parso.Column;
import com.epam.parso.SasFileProperties;
import com.epam.parso.SasFileReader;
import com.epam.parso.impl.CSVDataWriterImpl;
import com.epam.parso.impl.SasFileReaderImpl;

/**
 * Performs countings and checks on the BPE SAS database.
 * 
 * @author Franck
 */
public class SASUtils {

	public static Logger logger = LogManager.getLogger(SASUtils.class);

	static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	public static void countEquipmentsByType() throws IOException {

		SasFileReader sasFileReader = new SasFileReaderImpl(new FileInputStream(Configuration.getSASDataFilePath().toString()));
		// Build the map of column indexes
		Map<String, Integer> colIndexes = new HashMap<String, Integer>();
		int index = 0;
		for (Column column : sasFileReader.getColumns()) colIndexes.put(column.getName().toLowerCase(), index++);

		SortedMap<String, Integer> countings = new TreeMap<String, Integer>();

		long linesToRead = sasFileReader.getSasFileProperties().getRowCount();
		logger.debug("There are " + linesToRead + " lines in " + Configuration.getSASDataFilePath().toString());
		for (long line = 0; line < linesToRead; line++) {

			Object[] values = sasFileReader.readNext();
			// Equipment identifier is first column + second column
			String equipmentType = values[colIndexes.get("typequ")].toString().trim();
			countings.putIfAbsent(equipmentType, 0);
			countings.put(equipmentType, countings.get(equipmentType) + 1);
		}
		for (String equipmentType : countings.keySet()) {
			System.out.println(equipmentType + "\t" + countings.get(equipmentType));
		}
		countings = aggregateCountings(countings, 2);
		for (String equipmentType : countings.keySet()) {
			System.out.println(equipmentType + "\t" + countings.get(equipmentType));
		}
	}

	private static SortedMap<String, Integer> aggregateCountings(Map<String, Integer> countings, int keyLength) {

		SortedMap<String, Integer> aggregates = new TreeMap<String, Integer>();

		for (String key : countings.keySet()) {
			String aggegateKey = (keyLength == 0) ? "Total" : key.substring(0, keyLength);
			aggregates.putIfAbsent(aggegateKey, 0);
			aggregates.put(aggegateKey, aggregates.get(aggegateKey) + countings.get(key));
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
	
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(CSV_FILE_NAME)), "UTF-8"));
		CSVDataWriter csvDataWriter = new CSVDataWriterImpl(writer);
		csvDataWriter.writeColumnNames(sasFileReader.getColumns());
		int linesToRead = (int) Math.min(rowCount, SASModelMaker.LINES_TO_READ);
		for (int i = 0; i < linesToRead; i++) {
			csvDataWriter.writeRow(sasFileReader.getColumns(), sasFileReader.readNext());			
		}
		writer.close();
	}
}
