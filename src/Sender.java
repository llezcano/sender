import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.xml.sax.InputSource;



/*
 * Ejemplos:
 * * Consultar todos los documentos en un path dado
 * 		http://localhost:9200/prueba/metrica/_search?pretty=true&q=*:* 
 * * 
 *
 */
@WebServlet("/SenderService")
public class Sender extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final String SEPARADOR = "-";
	public static final String TOOL = "tool";
	public static final String USER = "user";
	public static final String TIMESTAMP = "timeStampHook";
	public static final String DATA_TYPE = "dataType";
	public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS";
	
	public static final String PROPFILE = "sendingservice.properties";
	
	public static final String HOST = "dbhost";
	public static final String PATH_TO_STORE = "dbpathtostore";
	
	

	private Properties properties;
	private InputStream connectorsConf ;
//	private InputSource connectorsSource ;
	public Sender(){}

	/**
	 * Al iniciar el servlet deben cargarse los datos como por ejemplo
	 * host, user, pass, etc
	 * desde un archivo de properties
	 */
	@Override
	public void init() throws ServletException {
		properties = new Properties();
		try {
			properties.load( Thread.currentThread().getContextClassLoader().getResourceAsStream( PROPFILE ));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Cargo archivo de configuracion de los conectores	
		connectorsConf = this.getServletContext().getResourceAsStream("WEB-INF/config/connectors.xml") ;
		
	}
	

	/**
	 * Obtiene los datos en formato JSON de un request.
	 * @param request	Deberia usarse en un PUT request
	 * @return			Datos enviados en el PUT con "Content-Type" == "application/json"
	 */
	private String getJSONFromRequest( HttpServletRequest request ) {
		// Obtengo el JSON del POST request, ejemplo de un POST:
		//curl -POST http://localhost:8091/sender/SenderService -H "Content-Type: application/json" --data '{ "name" :"lea"}' 
		
		StringBuffer jb = new StringBuffer();
		String line = null;
		try {

			if ( ! (request.getContentType().equals("application/json")) ) {
				System.err.println("Content-Type is: "+ request.getContentType() + "!= application/json") ;	
				
				return null ; 
			}
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null) {
				jb.append(line);
			}
			
		} catch (Exception e) { /*report an error*/ }
		return jb.toString() ;
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.handleRequest(request);

	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.handleRequest(request);
	}

	/**
	 * Se encarga de procesar los request POST/GET que llegan al servidor
	 * @throws IOException 
	 */
	private void handleRequest(HttpServletRequest request) throws IOException {
		String appKey = "" ;
		String dataKey = "" ;
		
		/*Se obtienen todos los parametros (uno puede tener varios valores)*/
		Map<String, String[]> parameters = request.getParameterMap();

		/*Se queda con el primer valor de cada uno*/
		Map<String, String> paramToJSON = new LinkedHashMap<String, String>();
		
		
		for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
			paramToJSON.put(entry.getKey(), entry.getValue()[0]);
			// TODO ver si no se puede hacer tan cabeza (aunque tal vez este bien)
			if (entry.getKey().equals("app")) {
				appKey = entry.getValue()[0] ;
			} else if (entry.getKey().equals("data"))  {
				dataKey = entry.getValue()[0] ;				
			}			
		}

		// aqui insertamos el time stamp del hook
		//if( paramToJSON.get( TIMESTAMP ) == null ) {
		Date date = new Date();
		paramToJSON.put( TIMESTAMP , new Timestamp( date.getTime() ).toString() );
		//} 
		
		
		String DataJSON = this.getJSONFromRequest(request) ;
		System.out.println(DataJSON) ;
		
		
		App application = new App( appKey, dataKey, connectorsConf) ;
		
		System.out.println("ElasticSearch Path: " + application.getPath() ) ;
		System.out.println("Data ID: "+ application.getIdField() ) ;

		/*Se validan los datos (se buscan por determinados valores que deben existir)*/
		if( this.validateParam(paramToJSON) ) {
			// TODO esto solo se reduciria en reenviar el request, ya que solo cambia el path y host. El dato no.
			
			/*Se pasa el Map a JSON*/
			String jsonText = JSONValue.toJSONString(paramToJSON);

			/*Se genera el ID del dato que va a rpresentarlo en la DB*/
			String id = this.generateDataID(paramToJSON);
			
			
			
			try {
				//URI uri = new URI(  properties.getProperty( HOST ) + properties.getProperty( PATH_TO_STORE ) + id );
				id = application.getIdField() ; // TODO en realidad aca paso el campo, pero se tendria que extraer del JSON
				URI uri = new URI(  properties.getProperty( HOST ) + application.getPath() + id );

				//System.out.println(uri.toString());
				HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
			
				conn.setDoOutput(true);
				conn.setRequestMethod("PUT");
				conn.addRequestProperty("Content-Type", "application/json");
				conn.setRequestProperty("Accept", "application/json");
				OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
				//out.write(jsonText);
				out.write(DataJSON);
				out.flush();
				out.close();
				System.err.println(conn.getResponseCode());
			} catch(Exception err) {
				err.printStackTrace();
			}
		}
	}


	/**
	 * @param paramToJSON Mapa con todos los parametros que se reciben
	 * @return un ID unico par el dato, sin espacios
	 */
	private String generateDataID(Map<String, String> paramToJSON) {
		// TODO generalizar este metodo para que desde un archivo de configuración 
		// sepa como generar el ID de cada aplicacion para cada tipo de datos 
		String id = "";
		id += paramToJSON.get( TIMESTAMP );
		id += SEPARADOR;
		id += paramToJSON.get( DATA_TYPE);
		id += SEPARADOR;
		id += paramToJSON.get( USER);
		id = id.replaceAll( "\\s+", SEPARADOR ); //reempla espacios en blanco
		return id;
	}


	/**
	 * @param inputString un supuesto timestamp en formato string
	 * @return si el formato es correcto o no.
	 */
	public static boolean isTimeStampValid(String inputString) { 
		SimpleDateFormat format = new SimpleDateFormat( TIMESTAMP_FORMAT );
		try {
			format.parse(inputString);
			return true;
		}
		catch(ParseException e) {
			return false;
		}
	}

	
	/**
	 * @param paramToJSON Mapa con todos los parametros que se recibieron
	 * @return si la llamada al servicio se realizo con todos los datos correctos
	 */
	private boolean validateParam(Map<String, String> paramToJSON) {
		return true ;
		/*
		if( paramToJSON.get( USER) != null && paramToJSON.get( DATA_TYPE ) != null &&
				isTimeStampValid( paramToJSON.get( TIMESTAMP ))) {
			return true;
		}
		return false;
		*/
	}
	
}