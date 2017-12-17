package mr;

import java.io.IOException;

import kvtype.TypeDistance;
import kvtype.TypeDistanceWritable;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

public class KnnCombiner extends Reducer<IntWritable, TypeDistanceWritable, IntWritable, TypeDistanceWritable> {
//	private Logger log = LoggerFactory.getLogger(KnnReducer.class);
	private int knn_k=-1;
	
	private TypeDistanceWritable tdw = new TypeDistanceWritable();
	
	@Override 
	public void setup(Context cxt){
		knn_k = cxt.getConfiguration().getInt("KNN_K", 5);
	}
	
	@Override// 更新k个紧邻值
	public void reduce(IntWritable key, Iterable<TypeDistanceWritable> values,Context cxt) 
			throws IOException, InterruptedException{
		// 初始化k个近邻值
		TypeDistance td = new TypeDistance(knn_k);
		
		for(TypeDistanceWritable d:values){ // 多个Mapper的值合并到一起，更新k紧邻值
			td.insertValues(d.getType()	, d.getDistance());
		}
		
		// 计算预测值,写入Output
		tdw.setValues(td);
		cxt.write(key, tdw);
	}

}
