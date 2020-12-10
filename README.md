The code can be simply run by execute: 
>> java -jar SearchEngineWiki_xiang_zhang.jar EngineMode simScore


[Note] EngineMode specify the type of stemming and lemmatization, it can select from “NoneAll”, “stemOnly”, “lemmOnly”, “bothStemLemm”, and “Self”. “Self” has the best performance and will be selected by default. 
[Note] simScore specify the similarity score system we are using. It can select from “BM25”, “tfidf”, and "Boolean". “BM25” has the best performance and will be selected by default. 

[Note]  we provide the fully-built Lucene index in project folder with name “. /IndexLucene_” + EngineMode, if you do not have the corresponding folder, make sure that you have “. /wiki-subset-20140602” in the project folder, which will be used to build the Lucene index. 

Also, we assume that you put the “questions.txt” in the folder of the project. 
