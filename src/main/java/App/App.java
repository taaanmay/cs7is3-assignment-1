package App;

import java.io.IOException;
import java.util.Scanner;

import org.apache.lucene.queryparser.classic.ParseException;
public class App {

    public static void main(String[] args) throws IOException, ParseException {
        Scanner scanner = new Scanner(System.in);
        SearchEngine searchEngine = new SearchEngine(selectAlgorithm(scanner));
        searchEngine.buildIndex(args);
        searchEngine.runQueries();
//        if (shouldRunAllQueries(scanner))
//            searchEngine.runAllQueries();
//        else
//            searchEngine.queryLoop(scanner);
        searchEngine.shutdown();
        scanner.close();
    }

    public static SearchEngine.ScoringAlgorithm selectAlgorithm(Scanner scanner) {

        SearchEngine.ScoringAlgorithm algorithm = null;
        while (algorithm == null) {
            System.out.println(
                    "Select scoring method:\n"
                            + "1)\tClassic Similarity\n"
                            + "2)\tBM25 Similarity\n"
                            + "3)\tBoolean Similarity\n"
                            + "4)\tLM Dirichlet Similarity\n"
                            + "5)\tDFI Similarity\n");
            String userResponse = scanner.nextLine();
            switch (userResponse) {
                case "1":
                    algorithm = SearchEngine.ScoringAlgorithm.Classic;
                    break;
                case "2":
                    algorithm = SearchEngine.ScoringAlgorithm.BM25;
                    break;
                case "3":
                    algorithm = SearchEngine.ScoringAlgorithm.Boolean;
                    break;
                case "4":
                    algorithm = SearchEngine.ScoringAlgorithm.LMDirichlet;
                    break;
                case "5":
                    algorithm = SearchEngine.ScoringAlgorithm.DFISimilarity;
                    break;
                default:
                    break;
            }
        }
        return algorithm;
    }

    public static boolean shouldRunAllQueries(Scanner scanner) {
        boolean allQueries = false;
        boolean modeChosen = false;
        while (!modeChosen) {
            System.out.println(
                    "Choose query mode:\n"
                            + "[a]\tRun all Cranfield queries.\n"
                            + "[b]\tType query.\n");
            String userResponse = scanner.nextLine();
            switch (userResponse) {
                case "a":
                    allQueries = true;
                    modeChosen = true;
                    break;
                case "b":
                    modeChosen = true;
                    break;
                default:
                    break;
            }
        }
        return allQueries;
    }



}
