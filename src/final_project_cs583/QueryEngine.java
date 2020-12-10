
/*
 * Xiang Zhang 
 * CS583 - Fall
 * Homework #3
 */

package final_project_cs583;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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
	static String EngineMode="Self";
	static String simScore="BM25";
	boolean indexExists = false; // determine if the index is built or not
	String inputFolderPath = ""; // store the input folder path for building the index
	String IndexFolder = "IndexLucene"; // define the IndexFolder here, note it should attach this EngineMode when using

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

		String fullIndexFolder=IndexFolder+"_"+EngineMode;


		// if the IndexFolder is not exists, we need to update the terms-inverse-index
		// via inputFile
		if (!new File(fullIndexFolder).exists()) {
			indexExists = true;
			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
			String[] pathnames;
			File fc = new File(inputFolder);
			pathnames = fc.list();
			
			StandardAnalyzer analyzer = new StandardAnalyzer();
	        Analyzer analyzerStem = CustomAnalyzer.builder()
	                .withTokenizer("standard")
	                .addTokenFilter("lowercase")
	                .addTokenFilter("stop")
	                .addTokenFilter("porterstem")
	                .build();
	        
	        IndexWriterConfig config;
	        
	        if (EngineMode.equals("lemmOnly") || EngineMode.equals("NoneAll")) {
	        	config = new IndexWriterConfig(analyzer);
	        } else {
			config = new IndexWriterConfig(analyzerStem);
	        }		
			
			Directory index = FSDirectory.open(Paths.get(fullIndexFolder));
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
										addDoc(w, content, currDocName);
										content = "";
										currDocName = nextDocName;
									}
								}
							}

							if (EngineMode.equals("Self")) {
							String[] arr = readFile.replaceAll("[^A-Za-z ]", "").toLowerCase().split("\\s+");;//replaceAll("[^A-Za-z0-9 ]", "")
							readFile="";
							for (int i = 0; i < arr.length; i++) {
								readFile = readFile + arr[i] + " ";
							}
							}
							
							if (EngineMode.equals("stemOnly") || EngineMode.equals("NoneAll")) {
								content = content + readFile + " ";
							}  else {
								CoreDocument document = pipeline.processToCoreDocument(readFile);			
								for (CoreLabel tok : document.tokens()) {
									content = content + tok.lemma() + " ";
								}
								content = content + readFile + " ";
							}
							
							
						}
					}
					addDoc(w, content, currDocName);

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

	private static void addDoc(IndexWriter w, String content, String docName) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("content", content, Field.Store.YES));
		doc.add(new StringField("docName", docName, Field.Store.YES));
		w.addDocument(doc);
	}

	public static void main(String[] args) {
		try {
			EngineMode="Self";
			simScore="BM25";
            if (args.length>=1 ) {
            	if (args[0].equals("stemOnly")){
            	EngineMode="stemOnly";
            	}
            	if (args[0].equals("lemmOnly")){
                	EngineMode="lemmOnly";
                	}
            	if (args[0].equals("bothStemLemm")){
                	EngineMode="bothStemLemm";
                	}
            	if (args[0].equals("Self")){
            	EngineMode="Self";
            	}	
            	
            	if (args[0].equals("NoneAll")){
                	EngineMode="NoneAll";
                	}	
            	
            	if (args[1].equals("BM25")){
            		simScore="BM25";
                	}	
            	
            	if (args[1].equals("tfidf")){
            		simScore="tfidf";
                	}
            	
            	if (args[1].equals("Boolean")){
            		simScore="Boolean";
                	}
            }
            
            System.out.println("EngineMode: " + EngineMode);
            System.out.println("simScore: " + simScore);
			
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
				
				ResultClass ansSim=objQueryEngine.runQuery(query1, query2, pipeline);


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

		String ans="";
		double similarityScore=0;
		StandardAnalyzer analyzer = new StandardAnalyzer();
		try {
			String fullIndexFolder=IndexFolder+"_"+EngineMode;
		Directory index = FSDirectory.open(Paths.get(fullIndexFolder));
		
		String querystr = query1 + " " + query2;
		String querystrNLP="";

		if (EngineMode.equals("Self")) {	
		String[] arr = querystr.replaceAll("[^A-Za-z ]", "").toLowerCase().split("\\s+");//replaceAll("[^A-Za-z0-9 ]", "")	
		querystr="";
		for (int i = 0; i < arr.length; i++) {
			querystr = querystr + arr[i] + " ";
		}	
		}
		//System.out.println(querystr);
		
		if (EngineMode.equals("lemmOnly") || EngineMode.equals("bothStemLemm") || EngineMode.equals("Self") ) {			
		CoreDocument document = pipeline.processToCoreDocument(querystr);
		pipeline.annotate(document);
		for (CoreLabel tok : document.tokens()) {
			querystrNLP = querystrNLP + tok.lemma() + " ";
			}
		querystr=querystrNLP;
		}
		//System.out.println(querystr);
		
		
		if (EngineMode.equals("stemOnly") || EngineMode.equals("bothStemLemm") || EngineMode.equals("Self") ) {
	        Analyzer analyzerStem = CustomAnalyzer.builder()
	                .withTokenizer("standard")
	                .addTokenFilter("lowercase")
	                .addTokenFilter("stop")
	                .addTokenFilter("porterstem")
	                .build();
	        String querystrStem="";
	        TokenStream tokenStream = analyzerStem.tokenStream(null, querystr);
	        
	        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
	        tokenStream.reset();
	        while(tokenStream.incrementToken()) {
	        	querystrStem = querystrStem + attr.toString() + " ";
	        }    
	        querystr=querystrStem;
		}
		//System.out.println(querystr);
		
		
		org.apache.lucene.search.Query q;

			q = new QueryParser("content", analyzer).parse(querystr);
			
//			System.out.println("query is:" + q);
			int hitsPerPage = 1;
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			
        	
			if (simScore.equals("tfidf")) {
			ClassicSimilarity CS = new ClassicSimilarity();
			searcher.setSimilarity(CS);
			}
			if (simScore.equals("Boolean")) {
			BooleanSimilarity BS = new BooleanSimilarity();
			searcher.setSimilarity(BS);
			}
			
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
