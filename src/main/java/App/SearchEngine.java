package App;

// Imports
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.DFISimilarity;
import org.apache.lucene.search.similarities.IndependenceStandardized;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.core.StopFilter;

public class SearchEngine {


    private ScoringAlgorithm selectedAlgorithm;
    // Define Directories
    private static String INDEX_DIRECTORY = "index";
    private String RESULTS_FILE = "results/out-";
    private static String RESULTS_DIR = "results/";
    private static String QUERY_FILE = "cran/cran.qry";

    private Analyzer analyzer;
    private Directory directory;
    private DirectoryReader ireader;
    private IndexSearcher isearcher;

    private static int MAX_RESULTS = 30;


    public enum ScoringAlgorithm { BM25, Classic, Boolean, LMDirichlet, DFISimilarity}



    public SearchEngine(ScoringAlgorithm algorithm){
        this.analyzer = new EnglishAnalyzer();
        this.selectedAlgorithm = algorithm;
        try {
            this.directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void buildIndex(String[] args) throws IOException
    {

        // Create a new field type which will store term vector information
        FieldType ft = new FieldType(TextField.TYPE_STORED);
        ft.setTokenized(true); //done as default
        ft.setStoreTermVectors(true);
        ft.setStoreTermVectorPositions(true);
        ft.setStoreTermVectorOffsets(true);
        ft.setStoreTermVectorPayloads(true);

        // create and configure an index writer
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iwriter = new IndexWriter(directory, config);

        // Call to populate Index
        populateIndex(args, iwriter, ft);


        System.out.println("Built Index");
        // close the writer
        iwriter.close();

        // Call to create searcher (Function)
        try {
            ireader = DirectoryReader.open(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        isearcher = new IndexSearcher(ireader);
        switch (selectedAlgorithm) {
            case BM25:
                isearcher.setSimilarity(new BM25Similarity());
                break;
            case Classic:
                isearcher.setSimilarity(new ClassicSimilarity());
                break;
            case DFISimilarity:
                isearcher.setSimilarity(new DFISimilarity(new IndependenceStandardized()));
                break;
            case LMDirichlet:
                isearcher.setSimilarity(new LMDirichletSimilarity());
            case Boolean:
                isearcher.setSimilarity(new BooleanSimilarity());

        }

    }


    public void populateIndex(String[] corpus, IndexWriter iwriter, FieldType ft){
        // List of Documents
        System.out.println("Indexing: + "+ corpus[0]);
        ArrayList<Document> documents = new ArrayList<Document>();

        String document = "";

        try {
            Path corpusPath = Paths.get(corpus[0]);
            document = new String(Files.readAllBytes(corpusPath));
            String[] splitDocuments = document.split(".I (?=[0-9]+)"); // Match .I followed by one or more numbers and store in an array

            for (String item : splitDocuments) {
                if (!item.equals(""))
                    documents.add(this.processDocuments(item, ft)); // Call to process documents
            }

            // Add documents
            if(documents.size() != 0){
                iwriter.addDocuments(documents);
            }else{
                System.out.println("No documents found to add");
            }


        } catch (IOException e) {
            System.out.println("Error with the corpus splitting");
            throw new RuntimeException(e);
        }
    }

    Document processDocuments(String item, FieldType fieldType) throws IOException {
        Document returnResult = new Document();

        String[] fields = item.split(".[TAWB](\r\n|[\r\n])", -1);
        returnResult.add(new StringField("index", fields[0].trim(), Field.Store.YES));
        returnResult.add(new StringField("filename", fields[1].trim(), Field.Store.YES));
        returnResult.add(new StringField("author(s)", fields[2].trim(), Field.Store.YES));
        returnResult.add(new StringField("metadata", fields[3].trim(), Field.Store.YES));
        returnResult.add(new Field("content", fields[4].trim(), fieldType));

        return returnResult;
    }


    public ScoreDoc[] buildQuery(String queryString, boolean print) throws ParseException, IOException {

        List<String> tokens = new ArrayList<String>();

        // Build analyzer
        TokenStream stream = analyzer.tokenStream("content", queryString);
        //CharArraySet stopWords = new CharArraySet()["i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"];
        stream = new StopFilter(stream, EnglishAnalyzer.getDefaultStopSet()); // Stop Filter
        stream = new LowerCaseFilter(stream); // Lower Case Filter
        //stream = new PorterStemFilter(stream);
        stream.reset();
        while (stream.incrementToken()) {
            tokens.add(stream.addAttribute(CharTermAttribute.class).toString()); // https://stackoverflow.com/questions/6334692/how-to-use-a-lucene-analyzer-to-tokenize-a-string
        }
        stream.close();

        if (print) System.out.println("terms:");
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        for (int i=0; i<tokens.size() ; i++) {
            //Print
            if (print) System.out.print(tokens.get(i) + " ");

            // Build Query
            Query queryTerm = new TermQuery(new Term("content", tokens.get(i)));
            booleanQuery.add(queryTerm, BooleanClause.Occur.SHOULD);
        }
        if (print)
            System.out.println();

        return isearcher.search(booleanQuery.build(), MAX_RESULTS).scoreDocs;

    }



    public void runQueries()  throws IOException, ParseException {
        File output = new File(RESULTS_DIR);
        if (!output.exists()) {
            output.mkdir();
        }
        PrintWriter writer = new PrintWriter(RESULTS_FILE+selectedAlgorithm+".txt", "UTF-8");

        String queryFile = new String(Files.readAllBytes(Paths.get(QUERY_FILE)));
        String[] queries = queryFile.split(".I (?=\\d+[\n\r]+)");

        int counter = 0;

        for (int index=0; index< queries.length ; index++) {

            // Trim the queries
            queries[index] = queries[index].trim();

            if (queries[index].length() > 0) {

                String[] question = queries[index].split(".W(\r\n|[\r\n])");

                ScoreDoc[] hits = buildQuery(question[1], false); // Call to build query
                if (hits.length == 0)
                    System.out.println("fail");
                for (int i = 0; i < hits.length; i++) {
                    Document hitDoc = isearcher.doc(hits[i].doc);

                    // Write in Results filels
                    writer.println(counter + " 0 " + hitDoc.get("index") + " " + (i + 1) + " "
                                    + hits[i].score
                                    + " STANDARD");
                }

            }
            counter++;
        }
        writer.close();

    }


    public void shutdown() throws IOException {
        directory.close();
    }



}
