import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import javax.xml.xpath.*;

import org.xml.sax.InputSource;


public class App {
	private String appName ;
	private String dataType ;
	
	private String dbpath ;
	private String idField ;
	
	private InputSource source ;
	
	public App(String name, String data, InputStream source) throws IOException {
		appName = name ;
		dataType = data ;

		
		/* printing inputStream
		BufferedReader in = new BufferedReader(new InputStreamReader(xml));
		String inputLine;
		try {
			while ((inputLine = in.readLine()) != null)
			    System.out.println(inputLine);
			in.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		*/
	
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();

		
		try {
			// TODO arreglar problemas con InputSource... fijarse de cargarlo en un String o algo similar
			source.reset();
			InputSource connectorsConf = new InputSource(source) ;
			dbpath = xpath.evaluate("/connectors/app[@key='"+name+"']/dtype[@key='"+data+"']/dbpath", connectorsConf);
			source.reset();
			connectorsConf = new InputSource(source) ;
			idField = xpath.evaluate("/connectors/app[@key='"+name+"']/dtype[@key='"+data+"']/id", connectorsConf);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	
	}
	

	public String getPath() {
		return dbpath ;
	}
	
	
	public String getIdField() {
		return idField ;
	}
	
	public boolean validate( String JSON ) {
		return true ;
	}
	
	
}
