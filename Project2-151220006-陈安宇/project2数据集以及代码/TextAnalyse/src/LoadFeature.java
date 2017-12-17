import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class LoadFeature{
	  public List<String> loadfe(){
		        List<String> Feature = new ArrayList<>();
	    		FSDataInputStream fsr = null;
	    		BufferedReader bufferedReader = null;
	    		String line = null;
	    		Configuration conf = new Configuration();
	    		try
	    		{
	    			FileSystem fs = FileSystem.get(URI.create("/user/hadoop/feature/total.txt"),conf);
	    			fsr = fs.open(new Path("/user/hadoop/feature/total.txt"));
	    			bufferedReader = new BufferedReader(new InputStreamReader(fsr));		
	    			while ((line = bufferedReader.readLine()) != null)
	    			{
	            
	                Feature.add(line);
	                
	            }
	            
	        } catch (IOException ex) {
	            System.err.println("失败:"+ex.getMessage());
	        }
	    		return Feature; 
	    }
	    
}
