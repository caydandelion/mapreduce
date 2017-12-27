import org.apache.spark.sql.{Row, SparkSession}  
import org.apache.spark.sql.types._  
  
import scala.collection.mutable  
import java.text.SimpleDateFormat
object selectWord {  
  
   def main(args: Array[String]): Unit = {  
  
    /** *************************************************************************************************************** 
      * sparksession 
      */  
    val spark = SparkSession  
      .builder()  
      .master("local")  
      .appName("test") 
      .config("spark.sql.shuffle.partitions", "5")  
      .getOrCreate()  
      
    val WordCountSchema: StructType = StructType(mutable.ArraySeq(    
      StructField("num", IntegerType, nullable = false),      
      StructField("Sname", StringType, nullable = false)       
    ))
    val WordData = spark.sparkContext.textFile("/user/hadoop/sparktable/WordCountOutput.txt").map{  
      lines =>  
        val line = lines.split("\t")  
        Row(line(0).toInt,line(1))  
    }
    val WordTable = spark.createDataFrame(WordData, WordCountSchema)  
    WordTable.createOrReplaceTempView("Word")
    spark.sql("SELECT * FROM Word WHERE num>500").show()
   }
}