
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class NaiveBayes {
	
	private static List<String> label = new ArrayList<>();
	private static List<String> frequence = new ArrayList<>();
	
	// part1-----------------------------------------------------
       public static class Mapper_Part1 extends Mapper<LongWritable, Text, Text, Text> {
    	
    	static Text one = new Text("1");
	    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
	    	String[] s1= value.toString().split("\t");
	    	context.write(new Text(s1[0]), one);
			String[] s2= s1[1].split(",");
			for(int j=0;j<s2.length;j++)
			{
				String s = new String();
				s = s1[0]+" "+j+" "+s2[j];
				context.write(new Text(s), one);
			}
	    }
	    	
	    }
       public static class Reduce_Part1 extends Reducer<Text, Text, Text, Text> {
    	   
    	   
           public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        	   float all = 0;
        	   String v1 = key.toString();
        	   String[] v2 = v1.split(" ");
        	   for (Text val : values) {
                   // 获取总的单词数。
                   all = all+Integer.parseInt(val.toString());
               }
        	   String s = v1 + " "+ all;
        	   if(v2.length == 1){
        		  label.add(s);
        	    }
        	   else{
        		  frequence.add(s);
        	   }
        	   context.write(key, new Text(s));
           }
       }
    // part2-----------------------------------------------------
       
       public static class Mapper_Part2 extends Mapper<LongWritable, Text, Text, Text> {
    	   public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
    		   String[] s1= value.toString().split("\t");
    		   String[] s2= s1[1].split(",");
    		   String labelit = "1";
    		   float max = -100;
    		   for(String lab : label)
    		   {
    			   float fij = 1;
    			   int bool = 0;
    			   String[] v1 = lab.split(" ");
    			   String labelname = v1[0];
    			   String labelvalue = v1[1];
    			   for(int i = 0;i < s2.length;i++)
    			   {
    				   float vexnum = Float.parseFloat(s2[i]);
    				   for(String val : frequence)
    				   {
    					   //System.out.println(val);
    					   String[] itr = val.split(" ");
    					   int num = Integer.parseInt(itr[1]);
    					   float fre1 = Float.parseFloat(itr[2]);
    					   if(itr[0].equals(labelname)&&(num == i)&&(vexnum == fre1)){
    						   fij = fij*(Float.parseFloat(itr[3])/500);
    						   bool = 1;
    					       break;
    					    }
    						   
    				   }
    				   if(bool == 0)
    					   fij = 0;
    			   }
    			   System.out.println(labelvalue+"***");
    			   System.out.println(Float.parseFloat(labelvalue));
    			   float t = (Float.parseFloat(labelvalue))*fij;
    			   System.out.println(labelname+"***");
    			   System.out.println(fij);
    			   if(t>max)
    			   {
    				   max = t;
    				   labelit = labelname;
    			   }
    	   }
    		   context.write(new Text(s1[0]),new Text(labelit));
    	   
       }

}
       public static class Reduce_Part2 extends Reducer<Text, Text, Text, Text> {
           public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
               for (Text val : values) {
                   context.write(key, val);
               }
           }
       }
       public static void main(String[] args) throws Exception {
           // part1----------------------------------------------------
           Configuration conf1 = new Configuration();
           Job job1 = Job.getInstance(conf1, "My_tdif_part1");
           job1.setJarByClass(NaiveBayes.class);
           job1.setMapperClass(Mapper_Part1.class);
           job1.setReducerClass(Reduce_Part1.class);
           job1.setMapOutputKeyClass(Text.class);
           job1.setMapOutputValueClass(Text.class);
           job1.setOutputKeyClass(Text.class);
           job1.setOutputValueClass(Text.class);
           FileInputFormat.addInputPath(job1, new Path(args[0]));
           FileOutputFormat.setOutputPath(job1, new Path(args[1]));
           job1.waitForCompletion(true);
           // part2----------------------------------------
           Configuration conf2 = new Configuration();
           Job job2 = Job.getInstance(conf2, "My_tdif_part2");
           job2.setJarByClass(NaiveBayes.class);
           job2.setMapOutputKeyClass(Text.class);
           job2.setMapOutputValueClass(Text.class);
           job2.setOutputKeyClass(Text.class);
           job2.setOutputValueClass(Text.class);
           job2.setMapperClass(Mapper_Part2.class);
           job2.setReducerClass(Reduce_Part2.class);
           job2.setNumReduceTasks(1);
           FileInputFormat.setInputPaths(job2, new Path(args[2]));
           FileOutputFormat.setOutputPath(job2, new Path(args[3]));
           job2.waitForCompletion(true);
       }
}
