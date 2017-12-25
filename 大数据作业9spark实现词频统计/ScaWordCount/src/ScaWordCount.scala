import org.apache.spark.SparkContext  
import org.apache.spark.SparkConf

object ScaWordCount {
  def main(args: Array[String]): Unit = {  
      val inputFile = "/user/hadoop/sparktest1/sparkinput.txt"  
      val conf = new SparkConf().setAppName("wordcount").setMaster("local[2]")  
      val sc = new SparkContext(conf)  
      val textFile = sc.textFile(inputFile)  
      val wordcount = textFile.flatMap(line => line.split("\t")).filter(x => x!="1").map(word => (word,1)).reduceByKey((a,b)=>(a+b))  
      wordcount.foreach(println)
      val sortWords = wordcount.map(x => (x._2,x._1)).sortByKey(false).map(x => (x._2,x._1))
      val chooseWords =sortWords.take(4)
      chooseWords.foreach(println)
      sortWords.saveAsTextFile("/user/hadoop/sparkout")
  }  

}