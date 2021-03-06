package fr.insee.semweb.bpe.test;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import fr.insee.semweb.bpe.Configuration;
import org.junit.jupiter.api.Test;

import com.epam.parso.Column;

import fr.insee.semweb.bpe.SASUtils;

public class SASUtilsTest {

	@Test
	public void testCountEquipmentsByType() throws Exception {

		SortedMap<String, Integer> countings = SASUtils.countEquipmentsByType();
		for (String equipmentType : countings.keySet()) {
			System.out.println(equipmentType + "\t" + countings.get(equipmentType));
		}
		countings = SASUtils.aggregateCounts(countings, 2);
		int total = 0;
		for (String equipmentType : countings.keySet()) {
			total += countings.get(equipmentType);
			System.out.println(equipmentType + "\t" + countings.get(equipmentType));
		}
		System.out.println("Total number of equipments: " + total);
	}

	@Test
	public void testListFeaturesAndPropertiesByType() {
		SortedMap<String, SortedSet<String>> featuresAndProperties = Configuration.listFeaturesAndPropertiesByType();
		for (String type : featuresAndProperties.keySet()) {
			System.out.println(type + "\t" + featuresAndProperties.get(type));
		}
	}

	@Test
	public void testCheckFeaturesAndPropertiesByType() throws IOException {
		SASUtils.checkFeaturesAndProperties();
	}

	@Test
	public void testCheckMunicipalityCodes() throws IOException {
		SASUtils.checkMunicipalityCodes();
	}

	@Test
	public void testListColumns() throws IOException {

		Map<Integer, Column> columns = SASUtils.listColumns();
		System.out.println("Index\t\tLabel\t\tName\t\tType\t\tFormat");
		for (Integer index : columns.keySet()) {
			Column column = columns.get(index);
			System.out.println(index + "\t\t" + column.getLabel() + "\t\t" + column.getName() + "\t\t" + column.getType() + "\t\t" + column.getFormat());
		}
	}
}
