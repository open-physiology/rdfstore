import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.net.URLDecoder;
import java.net.URLEncoder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.HttpURLConnection;

public class Rdfstore
{
  /*
   * Variables that can be specified by command-line argument
   */
  public String templateDir;         // Directory with sparql templates.  Default: ./templates
  public boolean helpOnly;           // Whether to quit after displaying help info.  Default: false
  public String sparqlAddress;          // Address where to send sparql queries to.  Default: http://localhost
  public String sparqlMethod;        // Method to use for sparql queries (get or post).  Default: get
  public String sparqlUpdateAddress;   // Address where to send sparql updates to.  Default: Mimic sparqlAddress
  public String sparqlUpdateMethod; // Method to use for sparql updates (get or post).  Default: Mimic sparqlMethod
  public String sparqlFormat;           // Format to apply to sparl queries.  Default: %s
  public String preprocessor;         // URL of a service that will preprocess template entries
  public int port;                    // Port for Ricord Rdfstore.java server to listen on.  Default: 20060

  /*
   * Variables not specified by command-line
   */
  ArrayList<SparqlTemplate> templates;

  public class SparqlTemplate
  {
    public String text;
    public String name;
    public String url;
    public Map<String,String> configs;
  }

  public static void main(String [] args) throws Exception
  {
    Rdfstore r = new Rdfstore();
    r.run(args);
  }

  public void run(String [] args)
  {
    initRdfstore(args);

    if ( helpOnly == true )
      return;

    System.out.println( "Initiating server..." );

    HttpServer srv;
    try
    {
      srv = HttpServer.create(new InetSocketAddress(port), 0 );
    }
    catch( Exception e )
    {
      System.out.println( "Unable to initiate server.  Is the port already in use?" );
      return;
    }

    for ( SparqlTemplate t : templates )
      srv.createContext( "/"+t.url, new Rdfstore_NetHandler( t ) );

    srv.createContext( "/gui", new Rdfstore_NetHandler( null ) );

    srv.setExecutor(null);
    srv.start();

    System.out.println( "Server initiated." );

    /*
     * The program will now go dormant, waking up to handle incoming connections.
     */
  }

  public void initRdfstore(String [] args)
  {
    parseCommandline(args);

    if ( helpOnly == true )
      return;

    loadTemplateFiles();
  }

  public void parseCommandline( String [] args )
  {
    /*
     * Set default values
     */
    templateDir = "./templates";
    helpOnly = false;
    sparqlAddress = "http://localhost";
    sparqlUpdateAddress = "http://localhost";
    sparqlMethod = "get";
    sparqlUpdateMethod = "get";
    sparqlFormat = "%s";
    port = 20060;
    preprocessor = null;

    int i;
    String flag;
    boolean fSparqlUpdateAddy = false;
    boolean fSparqlUpdateMethod = false;

    for ( i = 0; i < args.length; i++ )
    {
      if ( args[i].length() > 2 && args[i].substring(0,2).equals("--") )
        flag = args[i].substring(2).toLowerCase();
      else if ( args[i].length() > 1 && args[i].substring(0,1).equals("-") )
        flag = args[i].substring(1).toLowerCase();
      else
        flag = args[i].toLowerCase();

      if ( flag.equals("help") || flag.equals("h") )
      {
        System.out.println( "Command line options are as follows:"                  );
        System.out.println( "------------------------------------"                  );
        System.out.println( "-templates <path to directory>"                        );
        System.out.println( "(Specifies path to directory with query templates)"    );
        System.out.println( "(Default: ./templates)"                                );
        System.out.println( "------------------------------------"                  );
        System.out.println( "-endpoint <URL>"                                       );
        System.out.println( "(Specifies the sparql query endpoint location)"        );
        System.out.println( "(Default: http://localhost)"                           );
        System.out.println( "------------------------------------"                  );
        System.out.println( "-method GET, or -method POST"                          );
        System.out.println( "(Specifies which HTTP method to use for queries)"      );
        System.out.println( "(Default: GET)"                                        );
        System.out.println( "------------------------------------"                  );
        System.out.println( "-update <URL>"                                         );
        System.out.println( "(Specifies the sparql update endpoint location)"       );
        System.out.println( "(For cases when sparql update has different URL than"  );
        System.out.println( " query, e.g. Fuseki)"                                  );
        System.out.println( "(Default: Mimics 'endpoint')"                          );
        System.out.println( "------------------------------------"                  );
        System.out.println( "-updatemethod GET, or -updatemethod POST"              );
        System.out.println( "(Which HTTP method to use for updates)"                );
        System.out.println( "(Default: Mimics 'method')"                            );
        System.out.println( "------------------------------------"                  );
        System.out.println( "-format <format>"                                      );
        System.out.println( "(A string, containing %s.  The %s will be replaced by" );
        System.out.println( " the query itself, and the resulting string will be"   );
        System.out.println( " sent to the sparql endpoint.  Good for things like"   );
        System.out.println( " triplestore-specific preambles, etc.)"                );
        System.out.println( "(Default: %s)"                                         );
        System.out.println( "------------------------------------"                  );
        System.out.println( "-preprocessor <URL>"                                   );
        System.out.println( "(URL pointing to a preprocessor.  If this is set,"     );
        System.out.println( "all form entries will be routed through that"          );
        System.out.println( "preprocessor, which might mutate them, before they"    );
        System.out.println( "are used in SPARQL.  See documentation for details.)"  );
        System.out.println( "(Default: none)"                                       );
        System.out.println( "------------------------------------"                  );
        System.out.println( "-port <number>"                                        );
        System.out.println( "(Which port should this Rdfstore program listen on?)"  );
        System.out.println( "(Default: 20060)"                                      );
        System.out.println( "------------------------------------"                  );
        System.out.println( "-help"                                                 );
        System.out.println( "(Displays this helpfile)"                              );
        System.out.println();
        helpOnly = true;
        return;
      }

      if ( flag.equals("templates") || flag.equals("temp") || flag.equals("template") )
      {
        if ( i+1 >= args.length )
        {
          System.out.println( "Specify a path to a directory containing sparql template files." );
          helpOnly = true;
          return;
        }
        templateDir = args[++i];
        System.out.println( "Using "+templateDir+" as template directory." );
        continue;
      }

      if ( flag.equals("addy") || flag.equals("address") || flag.equals("sparql_addy") || flag.equals("sparql_address") || flag.equals("endpt") || flag.equals("endpoint") )
      {
        if ( i+1 >= args.length )
        {
          System.out.println( "Specify the address of the SPARQL query endpoint." );
          helpOnly = true;
          return;
        }
        sparqlAddress = args[++i];
        System.out.println( "Using "+sparqlAddress+" as SPARQL query endpoint." );

        if ( fSparqlUpdateAddy == false )
          sparqlUpdateAddress = sparqlAddress;

        continue;
      }

      if ( flag.equals("update" ) || flag.equals("upd") )
      {
        fSparqlUpdateAddy = true;

        if ( i+1 >= args.length )
        {
          System.out.println( "Specify the address of the sparql update endpoint." );
          helpOnly = true;
          return;
        }
        sparqlUpdateAddress = args[++i];
        System.out.println( "Using "+sparqlUpdateAddress+" as SPARQL update endpoint." );

        continue;
      }

      if ( flag.equals("method") || flag.equals("mthd") || flag.equals("sparql_method") )
      {
        if ( i+1 >= args.length || (!args[i+1].equals("get") && !args[i+1].equals("post") && !args[i+1].equals("GET") && !args[i+1].equals("POST")) )
        {
          System.out.println( "Valid methods are:  GET, or POST" );
          helpOnly = true;
          return;
        }
        sparqlMethod = args[++i].toLowerCase();
        System.out.println( "Using "+args[i]+" as SPARQL query HTTP method" );

        if ( fSparqlUpdateMethod == false )
          sparqlUpdateMethod = sparqlMethod;

        continue;
      }

      if ( flag.equals("updatemethod") || flag.equals("update_method") || flag.equals("updatemthd") || flag.equals("update_mthd") )
      {
        fSparqlUpdateMethod = true;

        if ( i+1 >= args.length || (!args[i+1].equals("get") && !args[i+1].equals("post") && !args[i+1].equals("GET") && !args[i+1].equals("POST")) )
        {
          System.out.println( "Valid update methods are:  GET, or POST" );
          helpOnly = true;
          return;
        }
        sparqlUpdateMethod = args[++i].toLowerCase();
        System.out.println( "Using "+args[i]+" as SPARQL update HTTP method" );

        continue;
      }

      if ( flag.equals("fmt") || flag.equals("format") || flag.equals("sparql_fmt") || flag.equals("sparql_format") )
      {
        if ( i+1 >= args.length || !args[i+1].contains("%s") )
        {
          System.out.println( "Specify a format for sparql queries.  The format should be a string, with \"%s\" where you want the actual query itself to be filled in." );
          helpOnly = true;
          return;
        }
        sparqlFormat = args[++i];
        System.out.println( "Using "+sparqlFormat+" as sparql format." );

        continue;
      }

      if ( flag.equals("port") || flag.equals("p") )
      {
        if ( i+1 < args.length )
        {
          try
          {
            port = Integer.parseInt(args[++i]);
          }
          catch( Exception e )
          {
            System.out.println( "Port must be a number." );
            helpOnly = true;
            return;
          }
          System.out.println( "Rircordo Rdfstore will listen on port "+port );
        }
        else
        {
          System.out.println( "Which port do you want the server to listen on?" );
          helpOnly = true;
          return;
        }

        continue;
      }

      if ( flag.equals("preprocessor") || flag.equals("preproc") )
      {
        if ( i+1 >= args.length )
        {
          System.out.println( "Specify the URL of the preprocessor you want Rdfstore to use." );
          helpOnly = true;
          return;
        }
        preprocessor = args[++i];
        System.out.println( "Using "+preprocessor+" as preprocessor." );
        continue;
      }

      if ( flag.equals("get") || flag.equals("post") )
      {
        String [] reargs = {"method", flag};
        parseCommandline( reargs );
        continue;
      }

      System.out.println( "Unknown command-line argument: \""+flag+"\"" );
      helpOnly = true;
      return;
    }
  }

  public void loadTemplateFiles( )
  {
    File folder;
    File[] templateFiles = null;

    try
    {
      folder = new File(templateDir);
      templateFiles = folder.listFiles();
    }
    catch(Exception e)
    {
      folder = null;
    }

    if ( folder == null || templateFiles == null )
    {
      System.out.println( "Couldn't open SPARQL template directory "+templateDir+"." );
      System.out.println( "If that's not the correct template directory, rerun Rdfstore with commandline -templates <path to template directory>" );
      System.out.println( "Please also make sure Java has permission to view the directory." );
      System.out.println( "" );
      helpOnly = true;
      return;
    }

    System.out.println( "Loading templates..." );

    boolean fFile = false;
    templates = new ArrayList<SparqlTemplate>();

    for ( File f : templateFiles )
    {
      if ( f.isFile() )
      {
        addTemplate( f );
        fFile = true;
      }
    }

    if ( !fFile ) // INFO: GRS modified.
    {
      System.out.println( "The SPARQL template directory, "+templateDir+", does not seem to contain any template files!" );
      System.out.println( "If that's not the correct template directory rerun Rdfstore with commandline -template <path to template directory>" );
      System.out.println( "" );
      helpOnly = true;
    }
  }

  public void addTemplate( File f )
  {
    SparqlTemplate t = new SparqlTemplate();

    try
    {
      t.text = new Scanner(f).useDelimiter("\\A").next();
      t.name = parseTemplateName(f.getName());
      t.url = parseTemplateURL(f.getName());
    }
    catch( Exception e )
    {
      e.printStackTrace();
    }

    if ( t.url.contains( " " ) || t.url.contains( "/" ) || t.url.contains( "\\" ) )
    {
      System.out.println( "Warning: Template file \""+f.getName()+"\" contains illegal characters in its name, and is not being added as a template." );
      return;
    }

    t.configs = new HashMap<String,String>();

    t.text = t.text.replace("\n\r", "\n");
    t.text = t.text.replace("\r\n", "\n");
    t.text = t.text.replace("\r", "\n" );
    checkForTemplateCommands(t);

    templates.add( t );
  }

  public void checkForTemplateCommands(SparqlTemplate t)
  {
    if ( t.text.length() >= 1 && t.text.charAt(0) == '\n' )
    {
      t.text = t.text.substring(1,t.text.length());
      checkForTemplateCommands(t);
      return;
    }

    if ( t.text.length() < 1 || t.text.charAt(0) != '#' )
      return;

    int carriage = t.text.indexOf("\n");

    if ( carriage == -1 || carriage == t.text.length()-1 )
      return;

    int equalSign = t.text.indexOf("=");

    if ( equalSign <= 1 || equalSign >= carriage-1 )
      return;

    String variableName = t.text.substring(1,equalSign).trim();
    String valueName = t.text.substring(equalSign+1,carriage).trim();

    if ( variableName.length() == 0 || valueName.length() == 0 )
      return;

    t.configs.put(variableName, valueName);

    t.text = t.text.substring(carriage+1,t.text.length());

    checkForTemplateCommands(t);
  }

  public String parseTemplateURL( String x )
  {
    String retVal;

    if ( x.length() > 4 && x.substring( x.length() - 4 ).equals( ".txt" ) )
      retVal = x.substring( 0, x.length() - 4 );
    else
      retVal = x;

    return retVal;
  }

  public String parseTemplateName( String x )
  {
    String retVal = parseTemplateURL( x );

    retVal = retVal.replace("_", " ");

    return retVal;
  }

  class Rdfstore_NetHandler implements HttpHandler
  {
    SparqlTemplate tmplt;

    public Rdfstore_NetHandler( SparqlTemplate t )
    {
      this.tmplt = t;
    }

    public void handle(HttpExchange t) throws IOException
    {
      Headers requestHeaders = t.getRequestHeaders();

      if ( this.tmplt == null )
      {
        sendGUI( t );
        return;
      }

      boolean fJson;
      if ( requestHeaders.get("Accept") != null && requestHeaders.get("Accept").contains("application/json") )
        fJson = true;
      else
        fJson = false;

      String response, req;
      try
      {
        req = t.getRequestURI().toString().substring(3+tmplt.url.length());
      }
      catch(Exception e)
      {
        req = "";
      }

      Map<String, String> params = getArgs(req, t);
	  
      String query = tmplt.text;
      String preproc;
      boolean fMultiple = false;

      for ( Map.Entry<String,String> entry : params.entrySet() )
      {
        if ( entry.getValue() == "" && entry.getKey() != "" )
        {
          sendResponse( t, "Error: An entry in the template was left blank." );
          return;
        }

        preproc = tmplt.configs.get("Preprocessor" + entry.getKey());

        if ( preproc == null )
          preproc = preprocessor;

        if ( preproc != null )
        {
          String value = callPreprocessor( preproc, entry.getValue() ).trim();

          if ( value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']' )
          {
            if ( fMultiple )
            {
              sendResponse( t, "Error: At most one form in a template is allowed to refer to multiple classes" );
              return;
            }

            if ( !isNormalform( tmplt.text ) )
            {
              sendResponse( t, "Error: Multiple-class inputs are only allowed in templates in normal form. (See documentation)" );
              return;
            }

            fMultiple = true;
            query = fillInMultiInput( query, "<["+entry.getKey()+"]>", value );
          }
          else
          {
            query = query.replace("["+entry.getKey()+"]", value );
            if ( query.equals("No results.") )
            {
              sendResponse( t, "No results." );
              return;
            }
          }
        }
        else
          query = query.replace("["+entry.getKey()+"]", entry.getValue());
      }

      if ( query.matches("(?s).*\\[[0-9]\\].*") )
      {
        for ( int i = 0; i <= 9; i++ )
        {
          if ( query.matches("(?s).*\\["+i+"\\].*" ) )
          {
            if ( tmplt.configs.containsKey(""+i) )
              sendResponse( t, "Error: Template entry '"+escapeHTML(tmplt.configs.get(""+i))+"' is missing" );
            else
              sendResponse( t, "Error: Template entry "+i+" is missing" );

            return;
          }
        }
      }

      String sparqlAnswer;

      if ( isUpdateQuery(query) )
      {
        if ( sparqlUpdateMethod.equals("get") )
          sparqlAnswer = sparqlQueryUsingGet(query, sparqlUpdateAddress);
        else
          sparqlAnswer = sparqlQueryUsingPost(query, sparqlUpdateAddress, "update");
      }
      else
      {
        if ( sparqlMethod.equals("get") )
          sparqlAnswer = sparqlQueryUsingGet(query, sparqlAddress);
        else
          sparqlAnswer = sparqlQueryUsingPost(query, sparqlAddress, "query");
      }

      sparqlAnswer = escapeHTML(sparqlAnswer);

      if ( sparqlAnswer.charAt(0) == '?' )
      {
        if ( sparqlAnswer.contains("\n") )
          sparqlAnswer = sparqlAnswer.substring(sparqlAnswer.indexOf("\n") );
      }

      if ( sparqlAnswer.trim().length() == 0 )
        sparqlAnswer = "No results.";

      sendResponse( t, "<pre>"+sparqlAnswer+"</pre>" );
    }

    public String sparqlQueryUsingPost( String query, String url, String keyname )
    {
      try
      {
        StringBuilder postData = new StringBuilder();
        postData.append(keyname+"="+URLEncoder.encode(query, "UTF-8"));
        byte[] postDataBytes = postData.toString().getBytes("UTF-8");

        URL u = new URL(url);
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        c.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        c.setDoOutput(true);
        c.getOutputStream().write(postDataBytes);

        Reader in = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
        StringBuilder answer = new StringBuilder();
        for ( int x; (x = in.read()) >= 0; answer.append((char)x) );
        return answer.toString();
      }
      catch( Exception e )
      {
        return "500 Server Error.  Most likely, Rdfstore couldn't communicate with the SPARQL endpoint.";
      }
    }

    public String sparqlQueryUsingGet(String query, String url)
    {
      URL u;
      HttpURLConnection c;

      try
      {
        u = new URL(url + URLEncoder.encode(query, "UTF-8") );
        c = (HttpURLConnection) u.openConnection();

        c.setConnectTimeout(2000);  // To do: make this configurable.
        c.setReadTimeout(5000);     // This too.
      }
      catch(Exception e)
      {
        try
        {
          return "500 Server Error.  Rdfstore couldn't resolve the URL: "+url+URLEncoder.encode(query,"UTF-8")+".";
        }
        catch(Exception e2)
        {
          return "500 Server Error.  Rdfstore couldn't resolve the url to connect to the triplestore.";
        }
      }

      Scanner sc = null;
      try
      {
        sc = new Scanner(c.getInputStream( ) );
      }
      catch(Exception e)
      {
        return "500 Server Error.  Most likely, Rdfstore couldn't communicate with the SPARQL endpoint.";
      }

      return sc.useDelimiter("\\A").next();
    }
  }

  /*
   * escapeHTML thanks to Bruno Eberhard
   */
  public String escapeHTML(String s)
  {
    StringBuilder out = new StringBuilder(Math.max(16, s.length()));

    for (int i = 0; i < s.length(); i++)
    {
      char c = s.charAt(i);

      if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&' || c == '\'' )
      {
        out.append("&#");
        out.append((int) c);
        out.append(';');
      }
      else
        out.append(c);
    }
    return out.toString();
  }

  static public void sendResponse( HttpExchange t, String response )
  {
    try
    {
      Headers h = t.getResponseHeaders();
      h.add("Cache-Control", "no-cache, no-store, must-revalidate");
      h.add("Pragma", "no-cache");
      h.add("Expires", "0" );

      t.sendResponseHeaders(200,response.getBytes().length);
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
    catch(Exception e)
    {
      ;
    }
  }

  public void sendGUI( HttpExchange t )
  {
    String theHtml, theJS;

    try
    {
      theHtml = new Scanner(new File("gui.php")).useDelimiter("\\A").next();
      theJS = new Scanner(new File("gui.js")).useDelimiter("\\A").next();
    }
    catch(Exception e)
    {
      sendResponse( t, "The GUI could not be sent, due to a problem with the html file or the javascript file." );
      return;
    }

    theHtml = theHtml.replace("@JAVASCRIPT", "<script type='text/javascript'>"+theJS+"</script>");

    String theMenu = "<select id='pulldown' onchange='pulldownchange();'><option value='nosubmit' selected='selected'>Choose Template</option>";
    for ( SparqlTemplate tmplt : templates )
      theMenu += "<option value='"+tmplt.url+"'>"+tmplt.name+"</option>";

    theMenu += "</select>";

    theHtml = theHtml.replace( "@PULLDOWNMENU", theMenu );

    String ifchecks = "if ( templatename == 'nosubmit' )\n    hide_input_boxes();\n  else\n";
    for ( SparqlTemplate tmplt : templates )
    {
      if ( tmplt.text.matches("(?s).*\\[[0-9]\\].*") == false )
        ifchecks += "  if ( templatename == '"+tmplt.url+"' )\n    hide_input_boxes();\n  else\n";
      else
      {
        ifchecks += "  if ( templatename == '"+tmplt.url+"' )\n  {\n    show_input_boxes('";
        for ( int i = 0; i <= 9; i++ )
        {
          if ( tmplt.text.contains("["+i+"]") )
            ifchecks += i;
        }
        ifchecks += "');\n";

        for ( int i = 0; i <= 9; i++ )
        {
          if ( tmplt.text.contains("["+i+"]") )
          {
            if ( tmplt.configs.containsKey(""+i) )
              ifchecks += "    set_placeholder("+i+",\""+escapeHTML(tmplt.configs.get(""+i))+"\");\n";
            else
              ifchecks += "    clear_placeholder("+i+");\n";
          }
        }

        ifchecks += "  }\n  else\n";
      }
    }

    ifchecks += "    hide_input_boxes();\n";
    ifchecks += "  if ( templatename == 'nosubmit' )\n    hide_input_button();\n  else\n    show_input_button();";

    theHtml = theHtml.replace("@CHANGESELECTIONCODE", ifchecks);

    sendResponse( t, theHtml );
  }

  public static Map<String, String> getArgs(String query, HttpExchange t)
  {
    Map<String, String> result = new HashMap<String, String>();

    parseParameters( query, result );
    String body = postBody( t );

    parseParameters( body, result );

    return result;
  }

  static void parseParameters( String raw, Map<String,String> result )
  {
    try
    {
      for (String param : raw.split("&"))
      {
        String pair[] = param.split("=");
		if (pair[0].length() == 0) continue; // INFO: GRS modified.
		
        if (pair.length > 1)
          result.put(URLDecoder.decode(pair[0],"UTF-8"), URLDecoder.decode(pair[1],"UTF-8"));
        else
          result.put(URLDecoder.decode(pair[0],"UTF-8"), "");
      }
    }
    catch( Exception e )
    {
      ;
    }
  }

  public static boolean isUpdateQuery( String q )
  {
    String lower = q.toLowerCase();

    if ( lower.substring(0,6).equals("insert") )
      return true;

    if ( lower.substring(0,6).equals("delete") )
      return true;

    if ( lower.substring(0,4).equals("load") )
      return true;

    if ( lower.substring(0,6).equals("create") )
      return true;

    if ( lower.substring(0,5).equals("clear") )
      return true;

    if ( lower.substring(0,3).equals("add") )
      return true;

    if ( lower.substring(0,4).equals("move") )
      return true;

    if ( lower.substring(0,4).equals("drop") )
      return true;

    return false;
  }

  public String callPreprocessor( String url, String orig )
  {
    URL u;
    HttpURLConnection c;

    try
    {
      if ( url.contains( "%s" ) )
        url = url.replace("%s", URLEncoder.encode(orig, "UTF-8"));
      else
        url = url + URLEncoder.encode(orig, "UTF-8");

      u = new URL(url);
      c = (HttpURLConnection) u.openConnection();
      c.setRequestProperty("Accept", "application/json");

      /*
       * To do: make these configurable
       */
      c.setConnectTimeout(2000);
      c.setReadTimeout(5000);
    }
    catch(Exception e)
    {
      return null;
    }

    Scanner sc = null;
    try
    {
      sc = new Scanner(c.getInputStream() );
    }
    catch(Exception e)
    {
      return null;
    }

    return sc.useDelimiter("\\A").next();
  }

  public static boolean isNormalform(String x)
  {
    byte[] buf;
    int i, level=0;
    boolean fLevel = false;

    try
    {
      buf = x.getBytes("UTF-8");
    }
    catch(Exception e)
    {
      return false;
    }

    for ( i = 0; i < buf.length; i++ )
    {
      if ( buf[i] == '{' )
      {
        level++;
        fLevel = true;
      }
      else if ( buf[i] == '}' )
      {
        if ( level <= 0 )
          return false;

        level--;

        if ( level == 0 )
        {
          for ( int j = i+1; j < buf.length; j++ )
          {
            if ( buf[j] != ' ' && buf[j] != '\n' && buf[j] != '\r' )
              return false;
          }
          return true;
        }
      }
    }
    return ( level == 0 && fLevel );
  }

  public static String fillInMultiInput( String query, String needle, String values )
  {
    String dummied = query.replace( needle, "?multival" ).trim();

    dummied = dummied.substring(0,dummied.length()-1);

    values = values.substring(1,values.length()-1);

    String[] splitted = values.split(",");

    if (splitted.length <= 0)
      return "No results.";

    String filter = "";

    for ( int i = 0; i < splitted.length; i++ )
    {
      String onePiece = splitted[i].trim();

      if ( onePiece.length() < 3 )
        continue;

      filter += (i==0? "" : " || ") + "?multival = <" + onePiece.substring(1,onePiece.length()-1) + ">";
    }

    return dummied + " FILTER( "+filter+" ) }";
  }

  static String readLine( BufferedReader br )
  {
    try
    {
      return br.readLine();
    }
    catch( IOException e )
    {
      return null;
    }
  }

  static String postBody( HttpExchange t )
  {
    InputStreamReader is = null;
    StringBuilder sb = new StringBuilder();
    boolean fFirst = false;

    try
    {
      is = new InputStreamReader(t.getRequestBody(), "UTF-8");
    }
    catch( UnsupportedEncodingException e )
    {
      return "";
    }

    BufferedReader br = new BufferedReader(is);

    for ( String line = readLine(br); line != null; line = readLine(br) )
    {
      if ( fFirst )
        sb.append('\n');
      else
        fFirst = true;

      sb.append( line );
    }

    return sb.toString();
  }
}
