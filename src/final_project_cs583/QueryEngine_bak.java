
/*
 * Xiang Zhang 
 * CS583 - Fall
 * Homework #3
 */

package final_project_cs583;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.util.Properties;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;

public class QueryEngine {
	boolean indexExists = false; // determine if the index is built or not
	String inputFolderPath = ""; // store the input folder path for building the index
	String IndexFolder = "IndexLucene"; // define the IndexFolder here

	/*
	 * void QueryEngine(String inputFile): This function initialize the query engine
	 * via inputFile, and build terms-inverse-index in "IndexFolder" folder .
	 * 
	 * @input: String;
	 * 
	 * @return: null;
	 */
	public QueryEngine(String inputFolder) {
		inputFolderPath = inputFolder;
		try {
			buildIndex(inputFolder);
		} catch (Exception ignore) {
		}
	}

	/*
	 * void buildIndex(): This function build the terms-inverse-index in IndexFolder
	 * 
	 * @input: null;
	 * 
	 * @return: null;
	 */
	private void buildIndex(String inputFolder) throws IOException {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		// if the IndexFolder is not exists, we need to update the terms-inverse-index
		// via inputFile
		if (!new File(IndexFolder).exists()) {
			indexExists = true;
			String[] pathnames;
			File fc = new File(inputFolder);
			pathnames = fc.list();

			StandardAnalyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			Directory index = FSDirectory.open(Paths.get(IndexFolder));
			IndexWriter w;
			w = new IndexWriter(index, config);

			// process: #REDIRECT
			// Build separate for ===

			for (String pathname : pathnames) {
				try {
					Scanner fileScanner = new Scanner(new File(inputFolder + pathname));// inputFilePath
					String currDocName = "";
					String nextDocName = "";
					String content = "";
					String categories="";
					while (fileScanner.hasNext()) {
						String readFile = fileScanner.nextLine();
						if (!readFile.isEmpty()) {
							if (readFile.length() >= 2) {
								if (readFile.substring(0, 2).equals("[[")) {
									readFile = readFile.substring(2, readFile.length() - 2);
									nextDocName = readFile.toString();
									if (currDocName.equals("")) {
										currDocName = nextDocName;
									} else {
										// first save currDocName and content
										addDoc(w, categories, content, currDocName);
//										addDoc(w, content, currDocName);
										// System.out.println("The word is: " + currDocName);
										// System.out.println("The content is: " + content);
										content = "";
										categories="";
										currDocName = nextDocName;
									}
								}
							}
							
							CoreDocument document = pipeline.processToCoreDocument(readFile);
							if(readFile.length() > 10) {
								if (readFile.substring(0,10).equals("CATEGORIES")) {
									for (CoreLabel tok : document.tokens()) {
									categories = categories + tok.lemma() + " ";
									}
								}
							}
							
							for (CoreLabel tok : document.tokens()) {
								content = content + tok.lemma() + " ";
							}
						}
					}
					addDoc(w, categories, content, currDocName);
//					addDoc(w, content, currDocName);
					currDocName = nextDocName;
					// System.out.println("The word is: " + currDocName);
					// System.out.println("The content is: " + content);
					System.out.println("finish loading data for file: " + inputFolder + pathname);
					fileScanner.close();
				} catch (Exception ignore) {
				}

			}

			w.close();
		}
	}

	/*
	 * void addDoc(IndexWriter w, String content, String docName): This function
	 * will add documents in IndexWriter. The docs has two fields, i.e. content and
	 * docName
	 * 
	 * @input: IndexWriter, String, String;
	 * 
	 * @return: void;
	 */

	private static void addDoc(IndexWriter w, String categories, String content, String docName) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("categories", categories, Field.Store.YES));
		doc.add(new TextField("content", content, Field.Store.YES));
		doc.add(new StringField("docName", docName, Field.Store.YES));
		w.addDocument(doc);
	}

	public static void main(String[] args) {
		try {
			String inputFileDirect = "./wiki-subset-20140602/"; //"./wiki-subset-20140602/";// ;
			QueryEngine objQueryEngine = new QueryEngine(inputFileDirect);
			String queryFile = "./questions.txt";
			Scanner fileScanner = new Scanner(new File(queryFile));
			int count_T=0;
			int count_F=0;
			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
			

			System.out.println("Below are the documents correctly found, and the corresponding similarity:");
			
			while (fileScanner.hasNext()) {
				String query1 = fileScanner.nextLine();
				String query2 = fileScanner.nextLine();
				String ans = fileScanner.nextLine();
				fileScanner.nextLine();
//				System.out.println("Query1:" + query1);
//				System.out.println("Query2:" + query2);
//				System.out.println("Answer:" + ans);
				
				
				ResultClass ansSim=objQueryEngine.runQuery(query1, query2, pipeline);
//				System.out.println("My Answer:" + ans_sim);

				if (ans.equals(ansSim.DocNameStr)) {
					count_T++;
					System.out.println("Document:" + ansSim.DocNameStr + "is found correctly. "+ "Its similarity score is: " + ansSim.simSore);
				} else {
					count_F++;
					//System.out.println("Document:" + ans + " But found: " + ansSim.DocNameStr);
				}


			}
			System.out.println("In summary, we have : " + count_T + " queries being found correctly.");
			System.out.println("We have " + count_F + " remaining queries being found wrong.");
			System.out.println("The work is done by Xiang Zhang. Thank you for reviewing.");

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	public ResultClass runQuery(String query1, String query2, StanfordCoreNLP pipeline) {
//		System.out.println("Start runQ1_1");
		String ans="";
		double similarityScore=0;
		StandardAnalyzer analyzer = new StandardAnalyzer();
		try {
		Directory index = FSDirectory.open(Paths.get(IndexFolder));
		//String querystr =  query1 + " " + query2; //;
		String querystr = "\"" + query1 + "\"^4"+ " " + "\"" + query2 + "\"";
		
		String querystrNLP = "";

		
		
		CoreDocument document = pipeline.processToCoreDocument(querystr);
		for (CoreLabel tok : document.tokens()) {
			querystrNLP = querystrNLP + tok.lemma() + " ";
							}
		
		
		org.apache.lucene.search.Query q;

			q = new QueryParser("content", analyzer).parse(querystrNLP);
			
//			System.out.println("query is:" + q);
			int hitsPerPage = 1;
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			ClassicSimilarity CS = new ClassicSimilarity();
			BooleanSimilarity BS = new BooleanSimilarity();
//			searcher.setSimilarity(CS);
			
			TopDocs docs = searcher.search(q, hitsPerPage);
			ScoreDoc[] hits = docs.scoreDocs;

			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				similarityScore=hits[i].score;
				Document doc = new Document();
				doc.add(new TextField("title", "", Field.Store.YES));
				doc.add(new StringField("docid", d.get("docName"), Field.Store.YES));
				ResultClass objResultClass = new ResultClass();
				objResultClass.DocName = doc;
				ans=d.get("docName");
			}
			reader.close();

		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
		
		ResultClass outcome = new ResultClass();
		outcome.DocNameStr=ans;
		outcome.simSore=similarityScore;
		return outcome;
	}



}
