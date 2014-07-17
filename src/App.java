import java.io.IOException;
import java.io.InputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.InputSource;


public class App {
	private String dbpath ;

	public App(String name, String data, InputStream source) throws IOException {
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		
		
		try {
			// TODO arreglar problemas con InputSource... fijarse de cargarlo en un String o algo similar
			source.reset();
			InputSource connectorsConf = new InputSource(source) ;
			dbpath = xpath.evaluate("/connectors/app[@key='"+name+"']/dtype[@key='"+data+"']/dbpath", connectorsConf);
			
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	
	}
	
	public String getPath() {
		return dbpath ;
	}
	
	
}
