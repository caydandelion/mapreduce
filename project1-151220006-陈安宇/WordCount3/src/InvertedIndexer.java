import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;  
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;  
import java.util.HashSet;
import java.util.Hashtable;  
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;  
import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.fs.Path;  
import org.apache.hadoop.io.IntWritable;  
import org.apache.hadoop.io.LongWritable;  
import org.apache.hadoop.io.Text;  
import org.apache.hadoop.mapreduce.Job;  
import org.apache.hadoop.mapreduce.Mapper;  
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;  
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;  
import org.apache.hadoop.mapreduce.lib.input.FileSplit;  
      
import java.util.Iterator;  
import org.apache.hadoop.mapreduce.Reducer;  
import org.apache.hadoop.util.GenericOptionsParser;  
import org.apache.hadoop.util.StringUtils;
      
    public class InvertedIndexer{ 
      
          
          
        public static class InvertedIndexMapper extends Mapper<LongWritable, Text, Text, Text>   
        {  
        	 private boolean caseSensitive;
        	    private Set<String> patternsToSkip = new HashSet<String>();

        	    private Configuration conf;
        	    private BufferedReader fis;
        	    private Text word = new Text();

        	    @Override
        	    public void setup(Context context) throws IOException,
        	        InterruptedException {
        	      conf = context.getConfiguration();
        	      caseSensitive = conf.getBoolean("wordcount.case.sensitive", false);
        	      if (conf.getBoolean("wordcount.skip.patterns", false)) {
        	        URI[] patternsURIs = Job.getInstance(conf).getCacheFiles();
        	        for (URI patternsURI : patternsURIs) {
        	          Path patternsPath = new Path(patternsURI.getPath());
        	          String patternsFileName = patternsPath.getName().toString();
        	          parseSkipFile(patternsFileName);
        	        }
        	      }
        	    }

        	    private void parseSkipFile(String fileName) {
        	      try {
        	        fis = new BufferedReader(new FileReader(fileName));
        	        String pattern = null;
        	        while ((pattern = fis.readLine()) != null) {
        	          patternsToSkip.add(pattern);
        	        }
        	      } catch (IOException ioe) {
        	        System.err.println("Caught exception while parsing the cached file '"
        	            + StringUtils.stringifyException(ioe));
        	      }
        	    }

        	
        	@Override  
            public void map(LongWritable key, Text value, Context context)    
                    throws IOException, InterruptedException   
               
            {     
                //FileSplit fileSplit = (FileSplit)context.getInputSplit();  
                //String fileName = fileSplit.getPath().getName();  
                  
                String temp; 
                String url = new String();
                IntWritable frequence=new IntWritable();  
                int one=1;  
                Hashtable<String,Integer> hashmap=new Hashtable();    //key关键字选择String而不是Text，选择Text会出错   
                String line = (caseSensitive) ?
                        value.toString() : value.toString().toLowerCase();
                  WordSeg wd = new WordSeg();
                  List<String> lis = null;        
                //StringTokenizer itr = new StringTokenizer(line);  
                  String[] itr = line.split("\t");
              	  int t = itr.length;
              	  if(t > 5)
              {
              	  url = itr[t-1];
              	  int m = 4;
                  while(m < t-1)
                 {    
                	for (String pattern : patternsToSkip) {
                      	  itr[m] = itr[m].replaceAll(pattern, "");
                    }
                	    lis = wd.seg(itr[m]);
                      	for(int i = 0 ; i < lis.size() ; i++)
                      	{
                      		
                      	if((!(lis.get(i).equals(" ")))&&(!(lis.get(i).equals("")))&&(lis.get(i).length()!= 1))  
                      	{
                          word.set(lis.get(i));
                        if(hashmap.containsKey(word)){  
                          hashmap.put(word.toString(),hashmap.get(word)+1);    //由于Map的输入key是每一行对应的偏移量，  
                                                                              //所以只能统计每一行中相同单词的个数，  
                        }else{  
                        hashmap.put(word.toString(), one);                         
                        } 
                      	}
                      	}
                	m = m + 1;
                  
                }  
                  
                for(Iterator<String> it=hashmap.keySet().iterator();it.hasNext();){  
                    temp=it.next();  
                    //System.out.println("文本："+temp);
                    frequence=new IntWritable(hashmap.get(temp));  
                    Text fileName_frequence = new Text("***"+url+"@"+frequence.toString());    
                    context.write(new Text(temp),fileName_frequence);         
                }  
                  
            }  
        }  
     }
      
        public static class InvertedIndexCombiner extends Reducer<Text,Text,Text,Text>{  
              
              
            protected void reduce(Text key,Iterable<Text> values,Context context)  
                            throws IOException ,InterruptedException{      //合并mapper函数的输出  
               
                String fileName="";  
                int sum=0;  
                String num;  
                String s;  
                String UrlList = new String();
                for (Text val : values) {  
                        s= val.toString();  
                        fileName=s.substring(0, val.find("@"));  
                        num=s.substring(val.find("@")+1, val.getLength());      //提取“doc1@1”中‘@’后面的词频  
                        sum+=Integer.parseInt(num);  
                        UrlList = UrlList + "***"+ fileName;
                }  
            IntWritable frequence=new IntWritable(sum);  
            context.write(key,new Text(UrlList+"@"+frequence.toString()));  
            }  
        }  
          
          
          
          
      
        public static class InvertedIndexReducer extends Reducer<Text, Text, Text, Text>   
        {   @Override  
            protected void reduce(Text key, Iterable<Text> values, Context context)  
                    throws IOException, InterruptedException   
            {   Iterator<Text> it = values.iterator();  
                StringBuilder all = new StringBuilder();  
                if(it.hasNext())  all.append(it.next().toString());  
                for(;it.hasNext();) {  
                    all.append("***");  
                    all.append(it.next().toString());                     
                }  
                context.write(key, new Text(all.toString()));  
            } 
        }  
      
        public static void main(String[] args) throws Exception {   
            /*if(args.length!=2){  
                System.err.println("Usage: InvertedIndex <in> <out>");  
                System.exit(2);  
            }  */
              
                    Configuration conf = new Configuration();  
                    GenericOptionsParser optionParser = new GenericOptionsParser(conf, args);
                    String[] remainingArgs = optionParser.getRemainingArgs(); 
                    Job job = Job.getInstance(conf);
                    job.setJobName("invertedindexer");    
                    job.setJarByClass(InvertedIndexer.class);  
                    List<String> otherArgs = new ArrayList<String>();
                    for (int i=0; i < remainingArgs.length; ++i) {
                      if ("-skip".equals(remainingArgs[i])) {
                        job.addCacheFile(new Path(remainingArgs[++i]).toUri());
                        job.getConfiguration().setBoolean("wordcount.skip.patterns", true);
                      } else {
                        otherArgs.add(remainingArgs[i]);
                      }
                    }  
                      
                      
                      
                    job.setMapperClass(InvertedIndexMapper.class);  
                    job.setCombinerClass(InvertedIndexCombiner.class);  
                    job.setReducerClass(InvertedIndexReducer.class);  
                      
                    job.setOutputKeyClass(Text.class);  
                    job.setOutputValueClass(Text.class);  
                      
                    FileInputFormat.addInputPath(job, new Path(otherArgs.get(0)));  
                    FileOutputFormat.setOutputPath(job, new Path(otherArgs.get(1)));  
                      
                    System.exit(job.waitForCompletion(true) ? 0 : 1);  
           
            }
         
      
      
    }  