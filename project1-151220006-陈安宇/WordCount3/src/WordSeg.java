import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import java.io.InputStreamReader;

public class WordSeg{
	  private static final List<String> DIC = new ArrayList<>();
	   private static volatile int MAX_LENGTH = 0;
	    static{
	            System.out.println("开始初始化词典");
	            int max=1;
	            int count=0;
	            //List<String> lines = Files.readAllLines(Paths.get("/user/hadoop/dic2/chi_words.txt"), Charset.forName("utf-8"));
	            //for(String line : lines){
	    		FSDataInputStream fsr = null;
	    		BufferedReader bufferedReader = null;
	    		String line = null;
	    		Configuration conf = new Configuration();
	    		try
	    		{
	    			FileSystem fs = FileSystem.get(URI.create("/user/hadoop/dic2/chi_words.txt"),conf);
	    			fsr = fs.open(new Path("/user/hadoop/dic2/chi_words.txt"));
	    			bufferedReader = new BufferedReader(new InputStreamReader(fsr));		
	    			while ((line = bufferedReader.readLine()) != null)
	    			{
	            
	                DIC.add(line);
	                count++;
	                if(line.length()>max){
	                    max=line.length();
	                }
	            }
	            MAX_LENGTH = max;
	            System.out.println("完成初始化词典，词数目："+count);
	            System.out.println("最大分词长度："+MAX_LENGTH);
	        } catch (IOException ex) {
	            System.err.println("词典装载失败:"+ex.getMessage());
	        }
	         
	    }
	    public List<String> seg(String text){        
	        List<String> result = new ArrayList<>();
	        while(text.length()>0){
	            int len=MAX_LENGTH;
	            if(text.length()<len){
	                len=text.length();
	            }
	            //取指定的最大长度的文本去词典里面匹配
	            String tryWord = text.substring(0, 0+len);
	            while(!DIC.contains(tryWord)){
	                //如果长度为一且在词典中未找到匹配，则按长度为一切分
	                if(tryWord.length()==1){
	                    break;
	                }
	                //如果匹配不到，则长度减一继续匹配
	                tryWord=tryWord.substring(0, tryWord.length()-1);
	            }
	            result.add(tryWord);
	            //从待分词文本中去除已经分词的文本
	            text=text.substring(tryWord.length());
	        }
	        return result;
	    }
}