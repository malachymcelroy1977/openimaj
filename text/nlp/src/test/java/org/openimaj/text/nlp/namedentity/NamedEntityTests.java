package org.openimaj.text.nlp.namedentity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.index.CorruptIndexException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openimaj.ml.annotation.ScoredAnnotation;
import org.openimaj.text.nlp.TweetTokeniser;
import org.openimaj.text.nlp.TweetTokeniserException;
import org.openimaj.text.nlp.namedentity.YagoEntityCandidateFinderFactory.YagoEntityCandidateFinder;
import org.openimaj.text.nlp.namedentity.YagoEntityContextScorerFactory.YagoEntityContextScorer;

/**
 * Tests for named entity extraction
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * @author Laurence Willmore (lgw1e10@ecs.soton.ac.uk)
 *
 */
public class NamedEntityTests {

	private String[][] UriTweets = new String[][] {
			new String[] {
					"http://yago-knowledge.org/resource/Olam_International",
					"Is Olam that company in singapore that produces rice and cotton?" },
			new String[] {
					"http://yago-knowledge.org/resource/Array_Networks",
					"Proven in over 5000 worldwide customer deployments, Array Networks is a global leader in application, desktop and cloud service delivery."		
			},
			new String[] { "http://yago-knowledge.org/resource/Supermicro",
					"Super Micro are a global leader in high-performance, high-efficiency server technology" },
			new String[] {
					"http://yago-knowledge.org/resource/Scientific_Atlanta",
					"This site allows our customers to download various technical documents from the Transmission Networks Systems Division" },
			new String[] {
					"http://yago-knowledge.org/resource/Government_National_Mortgage_Association",
					"At Ginnie Mae, we help make affordable housing a reality for millions of low- and moderate-income households across America" },
			new String[] {
					"http://yago-knowledge.org/resource/International_Union_for_Conservation_of_Nature",
					"the world’s oldest and largest global environmental organization" },			
			new String[] {
					"http://yago-knowledge.org/resource/Yahoo!",
					"Yahoo was founded in 1994 by Stanford PhD candidates David Filo and Jerry Yang as a way for them to keep track of their personal interests on the Internet" },

			new String[] { "http://yago-knowledge.org/resource/Fujifilm",
					"I have been looking at cannon and fuji for a new camera, but can't decide" },
			new String[] {
					"http://yago-knowledge.org/resource/HSBC",
					"What do people thing of HSBC? How does it compare to other British Banks? In particular for its Commercial Banking Services" } };
	private String[] bogusTweets = new String[]{
		"I love apples",
		"There are no companies mentioned in this little peice of text",	
		"My dog is going to have to go to the kennels while I am on holiday.",	
		"If the weather is really good tomorrow, I am going to go for a ride on my bike",	
		"I like beer"
	};
	ArrayList<String> companyUris = new ArrayList<String>();
	ArrayList<String> tweets = new ArrayList<String>();
	
	/**
	 * The location of the lucene index
	 */
	@Rule
	public TemporaryFolder index = new TemporaryFolder();
	private YagoEntityContextScorer contextScorer;
	private YagoEntityCandidateFinder candidateFinder;
	private boolean verbose = true;

	/**
	 * Tests the functionality of the ContextScorer with a small and easy set of entities.
	 */
	@Test
	public void testYagoContextScorer() {
		contextScorer = getContextScorer();
		for (int i = 0; i < tweets.size(); i++) {
			String tweet = tweets.get(i);
			TweetTokeniser t = null;
			try {
				t = new TweetTokeniser(tweet);
			} catch (UnsupportedEncodingException e) {				
				e.printStackTrace();
			} catch (TweetTokeniserException e) {				
				e.printStackTrace();
			}
			List<String> tokens = t.getStringTokens();
			String company = YagoQueryUtils.yagoResourceToString(companyUris
					.get(i));
			String topresult = null;
			float topscore = 0;
			HashMap<String, Float> res = contextScorer
					.getScoredEntitiesFromContext(tokens);
			for (String com : res.keySet()) {
				if (res.get(com) > topscore) {
					topresult = com;
					topscore = res.get(com);
				}
			}
			System.out.println(topscore);
			assertEquals(company, topresult);
		}

	}
	
	/**
	 * Tests the functionality of the Complete Yago entity extractor with a small and easy set of entities.
	 */
	@Test
	public void testYagoCompleteEntityExtractor(){
		contextScorer = getContextScorer();
		candidateFinder = getCandidateFinder();
		YagoEntityCompleteAnnotator anno = new YagoEntityCompleteAnnotator(contextScorer, candidateFinder);
		buildArrays();
		for (int i = 0; i < tweets.size(); i++) {
			String tweet = tweets.get(i);
			TweetTokeniser t = null;
			try {
				t = new TweetTokeniser(tweet);
			} catch (UnsupportedEncodingException e) {				
				e.printStackTrace();
			} catch (TweetTokeniserException e) {				
				e.printStackTrace();
			}
			List<String> tokens = t.getStringTokens();
			String company = YagoQueryUtils.yagoResourceToString(companyUris
					.get(i));
			List<ScoredAnnotation<HashMap<String, Object>>> subResult = anno.annotate(tokens);
			for(ScoredAnnotation<HashMap<String, Object>> sub: subResult){
				String foundCompany = (String) sub.annotation.get(EntityAnnotator.URI);
				assertEquals(foundCompany, YagoQueryUtils.yagoResourceToString(companyUris.get(i)));
			}
		}
	}

	/**
	 * Tests if the Filtered Search in QuickSearcher works using a ContextScorer as a proxy.
	 */
	@Test
	public void testQuickSearcherFilteredSearch() {
		contextScorer = getContextScorer();
		buildArrays();
		// build subset filter
		List<String> subset = new ArrayList<String>();
		int subsize = companyUris.size() - 5;
		for (int i = 0; i < subsize; i++) {
			subset.add(YagoQueryUtils.yagoResourceToString(companyUris.get(i)));
		}
		print("Subset: ");
		for (String s : subset)
			print(s);		
		for (int i = 0; i < tweets.size(); i++) {
			String tweet = tweets.get(i);
			print("Tweet: " + tweet);
			TweetTokeniser t = null;
			try {
				t = new TweetTokeniser(tweet);
			} catch (UnsupportedEncodingException e) {				
				e.printStackTrace();
			} catch (TweetTokeniserException e) {				
				e.printStackTrace();
			}
			List<String> tokens = t.getStringTokens();
			HashMap<String, Float> result = (HashMap<String, Float>) contextScorer
					.getScoresForEntityList(subset, tokens);
			//assertEquals(subset.size(), result.size());
			print("Hits :");
			for (String com : result.keySet()) {
				print(com+" : "+result.get(com));
				assertTrue(subset.contains(com));
			}
		}
	}

	private YagoEntityCandidateFinder getCandidateFinder() {
		try {
			return new YagoEntityCandidateFinderFactory(false).createFromAliasFile(YagoEntityCandidateMapFileBuilder.getDefaultMapFilePath());
		} catch (IOException e) {			
			e.printStackTrace();
		}
		return null;
	}
	
	
	private YagoEntityContextScorer getContextScorer() {
		try {
			if (indexEmpty())
				return buildYagoTestIndex();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(0);
		}
		buildArrays();
		try {
			return new YagoEntityContextScorerFactory(false).createFromIndexFile(index
					.getRoot().getAbsolutePath());
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private YagoEntityContextScorer buildYagoTestIndex() {
		YagoEntityContextScorer yci = null;
		ArrayList<String> companyUris = new ArrayList<String>();
		for (int i = 0; i < UriTweets.length; i++) {
			companyUris.add(UriTweets[i][0]);
		}
		try {
			yci = new YagoEntityContextScorerFactory(false).createFromYagoURIList(
					companyUris, index.getRoot().getAbsolutePath(),
					YagoQueryUtils.YAGO_SPARQL_ENDPOINT);
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return yci;
	}

	private boolean indexEmpty() throws IOException {
		if (index.getRoot().listFiles().length > 0)
			return false;
		else
			return true;
	}

	private void buildArrays() {
		companyUris = new ArrayList<String>();
		tweets = new ArrayList<String>();
		for (int i = 0; i < UriTweets.length; i++) {
			companyUris.add(UriTweets[i][0]);
			tweets.add(UriTweets[i][1]);
		}
	}
	
	private void print(String message){
		if(verbose)System.out.println(message);
	}
}
