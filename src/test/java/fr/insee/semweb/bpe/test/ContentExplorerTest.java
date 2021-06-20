package fr.insee.semweb.bpe.test;

import org.junit.jupiter.api.Test;

import fr.insee.semweb.bpe.Configuration.Domain;
import fr.insee.semweb.bpe.ContentExplorer;

public class ContentExplorerTest {

	@Test
	public void testListCharacteristics() {

		ContentExplorer explorer= new ContentExplorer();
		System.out.println("Ensemble :" + explorer.listCharacteristics(Domain.ENSEMBLE));
		System.out.println("Enseignement :" + explorer.listCharacteristics(Domain.ENSEIGNEMENT));
		System.out.println("Sport et loisirs :" + explorer.listCharacteristics(Domain.SPORT_LOISIR));
	}

	@Test
	public void testListQualityRatings() {

		ContentExplorer explorer= new ContentExplorer();
		System.out.println("Ensemble :" + explorer.listQualityRatings(Domain.ENSEMBLE));
		System.out.println("Enseignement :" + explorer.listQualityRatings(Domain.ENSEIGNEMENT));
		System.out.println("Sport et loisirs :" + explorer.listQualityRatings(Domain.SPORT_LOISIR));
	}
}
