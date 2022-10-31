# cs7is3-assignment-1

### To run the application: 
`java -jar target/SearchEngine-1.0.jar cran/cran.all.1400`

### To evaluate the code using trec eval
`cd trec_eval`


`make`


`./trec_eval  ../cran/cranqrelformat.csv ../results/[RESULT FILE NAME]`

For example, for BM25:

`./trec_eval  ../cran/cranqrelformat.csv ../results/out-BM25.txt  `
