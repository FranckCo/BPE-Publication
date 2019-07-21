package fr.insee.semweb.bpe;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.iryndin.jdbf.core.DbfRecord;
import net.iryndin.jdbf.reader.DbfReader;

/**
 * BPE files can be downloaded from https://www.insee.fr/fr/statistiques/3568638
 * https://www.insee.fr/fr/statistiques/fichier/3568638/bpe17_ensemble_xy_dbase.zip
 * https://www.insee.fr/fr/statistiques/fichier/3568638/bpe17_enseignement_xy_dbase.zip
 * https://www.insee.fr/fr/statistiques/fichier/3568638/bpe17_sport_Loisir_xy_dbase.zip
 * 
 * @author Franck
 *
 */
public class ContentExplorer {

	private static final Logger logger = LogManager.getLogger();

	public static void main(String[] args) throws Exception {

		ContentExplorer explorer= new ContentExplorer();
		PrintStream report = new PrintStream(Configuration.DATA_RESOURCE_PATH.resolve("report.txt").toString());
		for (Configuration.Domain domain : Configuration.Domain.values()) {
			explorer.exploreArchive(domain, report);
		}
		report.close();
	}

	public void exploreArchive(Configuration.Domain domain, PrintStream report) {

		Path archivePath = Configuration.DATA_RESOURCE_PATH.resolve("bpe17_" + domain + "_xy_dbase.zip");
		logger.info("Exploring archive " + archivePath);

		// Unzip the archive in the 'resources' directory
		try (ArchiveInputStream archiveStream = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, new BufferedInputStream(new FileInputStream(archivePath.toFile())))) {
			ArchiveEntry entry = null;
			while ((entry = archiveStream.getNextEntry()) != null) {
				Path outputPath = Configuration.DATA_RESOURCE_PATH.resolve(entry.getName()); // We know there are no directories in the BPE archives
				try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
					IOUtils.copy(archiveStream, outputStream);
				}
			}
		} catch (Exception e) {
			logger.error("Error unzipping " + archivePath + " - " + e.getMessage());
			return;
		}
		Map<Path, Boolean> paths = Configuration.getDBFFilePaths(domain);
		for (Path path : paths.keySet()) {
			try {
				InputStream dbf = new FileInputStream(path.toString());
				DbfRecord record;
				try (DbfReader reader = new DbfReader(dbf)) {
					report.println("Metadata for file " + path.toString() + ":\n" + reader.getMetadata());
					while ((record = reader.read()) != null) {
						if (!paths.get(path) || (record.getRecordNumber() % Configuration.SAMPLING_RATE == 0)) {
							record.setStringCharset(Configuration.STRING_CHARSET);
							report.println("Record #" + record.getRecordNumber() + ": " + record.toMap());
						}
					}
					report.println();
					dbf.close();
				}
			} catch (Exception e) {
				logger.error("Error processing " + path + " - " + e.getMessage());
			}
		}
	}

	public Map<String, String> listCharacteristics(Configuration.Domain domain) {

		logger.info("Listing characteristics for domain " + domain.toString());
		Map<String, String> characteristics = new HashMap<String, String>();

		try {
			logger.info("Opening " + Configuration.getBDFTypesCodelistFilePath(domain).toString());
			InputStream dbf = new FileInputStream(Configuration.getBDFTypesCodelistFilePath(domain).toString());
			DbfRecord record;
			try (DbfReader reader = new DbfReader(dbf)) {
				while ((record = reader.read()) != null) {
					if (record.getString("MODALITE") == null) continue;
					if (record.getString("MODALITE").equals("1")) {
						characteristics.put(record.getString("VARIABLE"), record.getString("MODLIBELLE"));
					}
				}
				dbf.close();
			}
		} catch (Exception e) {
			logger.error("Error processing file - " + e.getMessage());
		}

		return characteristics;
	}

	public Set<String> listQualityRatings(Configuration.Domain domain) {

		logger.info("Listing characteristics for domain " + domain.toString());
		Set<String> ratings = new HashSet<String>();

		try {
			logger.info("Opening " + Configuration.getDBFDataFilePath(domain).toString());
			InputStream dbf = new FileInputStream(Configuration.getDBFDataFilePath(domain).toString());
			DbfRecord record;
			try (DbfReader reader = new DbfReader(dbf)) {
				while ((record = reader.read()) != null) ratings.add(record.getString("qualite_xy"));
				dbf.close();
			}
		} catch (Exception e) {
			logger.error("Error processing file - " + e.getMessage());
		}

		return ratings;	
	}
}
