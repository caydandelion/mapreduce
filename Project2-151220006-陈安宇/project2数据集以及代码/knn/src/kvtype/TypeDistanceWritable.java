package kvtype;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;


public class TypeDistanceWritable implements WritableComparable<TypeDistanceWritable>,Cloneable {
//	private Logger log = LoggerFactory.getLogger(KData.class);
	// k个类别
	private int[] type;
	// k个距离
	private float[] distance;
	private int k;
	
	
	public TypeDistanceWritable(){// 空指针异常
	}

	public TypeDistanceWritable(int[] type,float[] distance) {
//		log.info("in KData(float[][] data,float[] distance");
		this.k=type.length;
		this.type=type;
		this.distance=distance;
	}
	public  void setValues(int[] type,float[] distance) {
		this.k=type.length;
		this.type=type;
		this.distance=distance;
	}
	
	public void setValues(TypeDistance td){
		this.k=td.getDistances().length;
		this.type=td.getType();
		this.distance=td.getDistances();
	}
	
	
	public void readFields(DataInput arg0) throws IOException {
		k=arg0.readInt();

		type = new int[k];
		distance = new float[k];
		for(int i=0;i<type.length;i++){
			type[i]=arg0.readInt();
			distance[i]=arg0.readFloat();
		}
	}
	public void write(DataOutput arg0) throws IOException {
		arg0.writeInt(k);
		for(int i=0;i<type.length;i++){
			arg0.writeInt(type[i]);
			arg0.writeFloat(distance[i]);
		}
	}
	public int compareTo(TypeDistanceWritable o) {//
			
		return 1; 
	}
	
	
	@Override
	public int hashCode(){
		int hashCode =0;
		for(int i=0;i<type.length;i++){
			
			hashCode+=type[i];
			
			hashCode=+Float.floatToIntBits(distance[i]);
		}
		return hashCode;
	}

	public float[] getDistance() {
		return distance;
	}

	public void setDistance(float[] distance) {
		this.distance = distance;
	}

	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}

	public int[] getType() {
		return type;
	}

	public void setType(int[] type) {
		this.type = type;
	}
	
}