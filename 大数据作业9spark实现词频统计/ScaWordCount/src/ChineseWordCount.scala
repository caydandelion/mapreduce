import java.io.File

import org.apache.spark.{SparkConf, SparkContext}
import scala.collection.Iterator
import scala.io.Source

/**
  * wordcount进行排序并排除停用词
  */
object ChineseWordCount {

   def main(args: Array[String]) {
     val conf = new SparkConf().setMaster("local[2]").setAppName("wordcount")
     val sc = new SparkContext(conf)

     val outFile = "/user/hadoop/sparkoutput"
     var stopWords:Iterator[String] = null
     val stopWordsFile = new File("/user/hadoop/input10/patterns.txt")

     if(stopWordsFile.exists()){
       stopWords =  Source.fromFile(stopWordsFile).getLines
     }
     val stopWordList = stopWords.toList

     val textFile = sc.textFile("/user/hadoop/sparkword/sparkword.txt")
     val result = textFile.flatMap(_.split(" ")).filter(!_.isEmpty).filter(!stopWordList.contains(_)).map((_,1)).reduceByKey(_+_).map{case (word,count) =>(count,word)}.sortByKey(false)

     result.saveAsTextFile(outFile)
   }

 }
