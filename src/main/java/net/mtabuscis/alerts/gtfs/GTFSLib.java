package net.mtabuscis.alerts.gtfs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;

//This class loads all of the supported GTFS files for feed lookups
@Component
public class GTFSLib {
	
	private static Logger _log = LoggerFactory.getLogger(GTFSLib.class);

	private HashMap<String, ArrayList<GtfsRelationalDaoImpl>> _dao = new HashMap<String,  ArrayList<GtfsRelationalDaoImpl>>(25);
	
	public void load(String name, ArrayList<String> updateFiles, String bucket, String baseS3Path, AmazonS3 s3) throws IOException{
		
		_log.info("Loading new GTFS Files for feedId: "+name);
		
		//create a tmpfile for each file
		ArrayList<GtfsRelationalDaoImpl> daos = new ArrayList<GtfsRelationalDaoImpl>(5);
		for(String f : updateFiles){
			_log.info("Reading "+String.valueOf(f));
			File tempFile = File.createTempFile("tmpgtfslib", "zip");
			S3Object s3file = s3.getObject(bucket, baseS3Path+f);
			S3ObjectInputStream s3is = s3file.getObjectContent();
			IOUtils.copy(s3is, new FileOutputStream(tempFile));
			
			GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
		    GtfsReader reader = new GtfsReader();
		    reader.setEntityStore(dao);
		    reader.setInputLocation(tempFile);
    		reader.run();
    		reader.close();
    		daos.add(dao);
    		
			s3is.close();
			s3file.close();
			tempFile.delete();
		}
		
	    _dao.put(name, daos);
		_log.info("Finished loading feed id "+name);
		
	}
	
	//There is a possibility of a race condition if this gets updated on the fly...
	public ArrayList<GtfsRelationalDaoImpl> getDao(String name){
		return _dao.get(name);
	}
	
	
	
}
