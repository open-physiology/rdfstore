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
  public String template_dir;         // Directory with sparql templates.  Default: ./templates
  public boolean help_only;           // Whether to quit after displaying help info.  Default: false
  public String sparql_addy;          // Address where to send sparql queries to.  Default: http://localhost
  public String sparql_method;        // Method to use for sparql queries (get or post).  Default: get
  public String sparql_update_addy;   // Address where to send sparql updates to.  Default: Mimic sparql_addy
  public String sparql_update_method; // Method to use for sparql updates (get or post).  Default: Mimic sparql_method
  public String sparql_fmt;           // Format to apply to sparl queries.  Default: %s
  public String preprocessor;         // URL of a service that will preprocess template entries
  public int port;                    // Port for Ricord Rdfstore.java server to listen on.  Default: 20060

  /*
   * Variables not specified by command-line
   */
  ArrayList<Sparql_template> templates;

  public class Sparql_template
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
    init_rdfstore(args);

    if ( help_only == true )
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

    for ( Sparql_template t : templates )
      srv.createContext( "/"+t.url, new Rdfstore_NetHandler( t ) );

    srv.createContext( "/gui", new Rdfstore_NetHandler( null ) );

    srv.setExecutor(null);
    srv.start();

    System.out.println( "Server initiated." );

    /*
     * The program will now go dormant, waking up to handle incoming connections.
     */
  }

  public void init_rdfstore(String [] args)
  {
    parse_commandline(args);

    if ( help_only == true )
      return;

    load_template_files();
  }

  public void parse_commandline( String [] args )
  {
    /*
     * Set default values
     */
    template_dir = "./templates";
    help_only = false;
    sparql_addy = "http://localhost";
    sparql_update_addy = "http://localhost";
    sparql_method = "get";
    sparql_update_method = "get";
    sparql_fmt = "%s";
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
        help_only = true;
        return;
      }

      if ( flag.equals("templates") || flag.equals("temp") || flag.equals("template") )
      {
        if ( i+1 >= args.length )
        {
          System.out.println( "Specify a path to a directory containing sparql template files." );
          help_only = true;
          return;
        }
        template_dir = args[++i];
        System.out.println( "Using "+template_dir+" as template directory." );
        continue;
      }

      if ( flag.equals("addy") || flag.equals("address") || flag.equals("sparql_addy") || flag.equals("sparql_address") || flag.equals("endpt") || flag.equals("endpoint") )
      {
        if ( i+1 >= args.length )
        {
          System.out.println( "Specify the address of the SPARQL query endpoint." );
          help_only = true;
          return;
        }
        sparql_addy = args[++i];
        System.out.println( "Using "+sparql_addy+" as SPARQL query endpoint." );

        if ( fSparqlUpdateAddy == false )
          sparql_update_addy = sparql_addy;

        continue;
      }

      if ( flag.equals("update" ) || flag.equals("upd") )
      {
        fSparqlUpdateAddy = true;

        if ( i+1 >= args.length )
        {
          System.out.println( "Specify the address of the sparql update endpoint." );
          help_only = true;
          return;
        }
        sparql_update_addy = args[++i];
        System.out.println( "Using "+sparql_update_addy+" as SPARQL update endpoint." );

        continue;
      }

      if ( flag.equals("method") || flag.equals("mthd") || flag.equals("sparql_method") )
      {
        if ( i+1 >= args.length || (!args[i+1].equals("get") && !args[i+1].equals("post") && !args[i+1].equals("GET") && !args[i+1].equals("POST")) )
        {
          System.out.println( "Valid methods are:  GET, or POST" );
          help_only = true;
          return;
        }
        sparql_method = args[++i].toLowerCase();
        System.out.println( "Using "+args[i]+" as SPARQL query HTTP method" );

        if ( fSparqlUpdateMethod == false )
          sparql_update_method = sparql_method;

        continue;
      }

      if ( flag.equals("updatemethod") || flag.equals("update_method") || flag.equals("updatemthd") || flag.equals("update_mthd") )
      {
        fSparqlUpdateMethod = true;

        if ( i+1 >= args.length || (!args[i+1].equals("get") && !args[i+1].equals("post") && !args[i+1].equals("GET") && !args[i+1].equals("POST")) )
        {
          System.out.println( "Valid update methods are:  GET, or POST" );
          help_only = true;
          return;
        }
        sparql_update_method = args[++i].toLowerCase();
        System.out.println( "Using "+args[i]+" as SPARQL update HTTP method" );

        continue;
      }

      if ( flag.equals("fmt") || flag.equals("format") || flag.equals("sparql_fmt") || flag.equals("sparql_format") )
      {
        if ( i+1 >= args.length || !args[i+1].contains("%s") )
        {
          System.out.println( "Specify a format for sparql queries.  The format should be a string, with \"%s\" where you want the actual query itself to be filled in." );
          help_only = true;
          return;
        }
        sparql_fmt = args[++i];
        System.out.println( "Using "+sparql_fmt+" as sparql format." );

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
            help_only = true;
            return;
          }
          System.out.println( "Rircordo Rdfstore will listen on port "+port );
        }
        else
        {
          System.out.println( "Which port do you want the server to listen on?" );
          help_only = true;
          return;
        }

        continue;
      }

      if ( flag.equals("preprocessor") || flag.equals("preproc") )
      {
        if ( i+1 >= args.length )
        {
          System.out.println( "Specify the URL of the preprocessor you want Rdfstore to use." );
          help_only = true;
          return;
        }
        preprocessor = args[++i];
        System.out.println( "Using "+preprocessor+" as preprocessor." );
        continue;
      }

      if ( flag.equals("get") || flag.equals("post") )
      {
        String [] reargs = {"method", flag};
        parse_commandline( reargs );
        continue;
      }

      System.out.println( "Unknown command-line argument: \""+flag+"\"" );
      help_only = true;
      return;
    }
  }

  public void load_template_files( )
  {
    File folder;
    File[] templatefiles = null;

    try
    {
      folder = new File(template_dir);
      templatefiles = folder.listFiles();
    }
    catch(Exception e)
    {
      folder = null;
    }

    if ( folder == null || templatefiles == null )
    {
      System.out.println( "Couldn't open SPARQL template directory "+template_dir+"." );
      System.out.println( "If that's not the correct template directory, rerun Rdfstore with commandline -templates <path to template directory>" );
      System.out.println( "Please also make sure Java has permission to view the directory." );
      System.out.println( "" );
      help_only = true;
      return;
    }

    System.out.println( "Loading templates..." );

    boolean fFile = false;
    templates = new ArrayList<Sparql_template>();

    for ( File f : templatefiles )
    {
      if ( f.isFile() )
      {
        add_template( f );
        fFile = true;
      }
    }

    if ( !fFile ) // INFO: GRS modified.
    {
      System.out.println( "The SPARQL template directory, "+template_dir+", does not seem to contain any template files!" );
      System.out.println( "If that's not the correct template directory rerun Rdfstore with commandline -template <path to template directory>" );
      System.out.println( "" );
      help_only = true;
    }
  }

  public void add_template( File f )
  {
    Sparql_template t = new Sparql_template();

    try
    {
      t.text = new Scanner(f).useDelimiter("\\A").next();
      t.name = parse_template_name(f.getName());
      t.url = parse_template_url(f.getName());
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
    check_for_template_commands(t);

    templates.add( t );
  }

  public void check_for_template_commands(Sparql_template t)
  {
    if ( t.text.length() >= 1 && t.text.charAt(0) == '\n' )
    {
      t.text = t.text.substring(1,t.text.length());
      check_for_template_commands(t);
      return;
    }

    if ( t.text.length() < 1 || t.text.charAt(0) != '#' )
      return;

    int carriage = t.text.indexOf("\n");

    if ( carriage == -1 || carriage == t.text.length()-1 )
      return;

    int equalsign = t.text.indexOf("=");

    if ( equalsign <= 1 || equalsign >= carriage-1 )
      return;

    String variablename = t.text.substring(1,equalsign).trim();
    String valuename = t.text.substring(equalsign+1,carriage).trim();

    if ( variablename.length() == 0 || valuename.length() == 0 )
      return;

    t.configs.put(variablename, valuename);

    t.text = t.text.substring(carriage+1,t.text.length());

    check_for_template_commands(t);
  }

  public String parse_template_url( String x )
  {
    String retval;

    if ( x.length() > 4 && x.substring( x.length() - 4 ).equals( ".txt" ) )
      retval = x.substring( 0, x.length() - 4 );
    else
      retval = x;

    return retval;
  }

  public String parse_template_name( String x )
  {
    String retval = parse_template_url( x );

    retval = retval.replace("_", " ");

    return retval;
  }

  class Rdfstore_NetHandler implements HttpHandler
  {
    Sparql_template tmplt;

    public Rdfstore_NetHandler( Sparql_template t )
    {
      this.tmplt = t;
    }

    public void handle(HttpExchange t) throws IOException
    {
      Headers requestHeaders = t.getRequestHeaders();

      if ( this.tmplt == null )
      {
        send_gui( t );
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

      Map<String, String> params = get_args(req, t);

      String query = tmplt.text;
      boolean fMultiple = false;

      for ( Map.Entry<String,String> entry : params.entrySet() )
      {
        if ( entry.getValue() == "" && entry.getKey() != "" )
        {
          send_response( t, "Error: An entry in the template was left blank." );
          return;
        }

        if ( preprocessor != null )
        {
          String value = call_preprocessor( preprocessor, entry.getValue() ).trim();

          if ( value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']' )
          {
            if ( fMultiple )
            {
              send_response( t, "Error: At most one form in a template is allowed to refer to multiple classes" );
              return;
            }

            if ( !is_normalform( tmplt.text ) )
            {
              send_response( t, "Error: Multiple-class inputs are only allowed in templates in normal form. (See documentation)" );
              return;
            }

            fMultiple = true;
            query = fill_in_multi_input( query, "<["+entry.getKey()+"]>", value );
          }
          else
          {
            query = query.replace("["+entry.getKey()+"]", value );
            if ( query.equals("No results.") )
            {
              send_response( t, "No results." );
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
              send_response( t, "Error: Template entry '"+escapeHTML(tmplt.configs.get(""+i))+"' is missing" );
            else
              send_response( t, "Error: Template entry "+i+" is missing" );

            return;
          }
        }
      }

      String sparql_answer;

      if ( is_update_query(query) )
      {
        if ( sparql_update_method.equals("get") )
          sparql_answer = sparql_query_using_get(query, sparql_update_addy);
        else
          sparql_answer = sparql_query_using_post(query, sparql_update_addy, "update");
      }
      else
      {
        if ( sparql_method.equals("get") )
          sparql_answer = sparql_query_using_get(query, sparql_addy);
        else
          sparql_answer = sparql_query_using_post(query, sparql_addy, "query");
      }

      sparql_answer = escapeHTML(sparql_answer);

      if ( sparql_answer.charAt(0) == '?' )
      {
        if ( sparql_answer.contains("\n") )
          sparql_answer = sparql_answer.substring(sparql_answer.indexOf("\n") );
      }

      if ( sparql_answer.trim().length() == 0 )
        sparql_answer = "No results.";

      send_response( t, "<pre>"+sparql_answer+"</pre>" );
    }

    public String sparql_query_using_post( String query, String url, String keyname )
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

    public String sparql_query_using_get(String query, String url)
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

  static public void send_response( HttpExchange t, String response )
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

  public void send_gui( HttpExchange t )
  {
    String the_html, the_js;

    try
    {
      the_html = new Scanner(new File("gui.php")).useDelimiter("\\A").next();
      the_js = new Scanner(new File("gui.js")).useDelimiter("\\A").next();
    }
    catch(Exception e)
    {
      send_response( t, "The GUI could not be sent, due to a problem with the html file or the javascript file." );
      return;
    }

    the_html = the_html.replace("@JAVASCRIPT", "<script type='text/javascript'>"+the_js+"</script>");

    String the_menu = "<select id='pulldown' onchange='pulldownchange();'><option value='nosubmit' selected='selected'>Choose Template</option>";
    for ( Sparql_template tmplt : templates )
      the_menu += "<option value='"+tmplt.url+"'>"+tmplt.name+"</option>";

    the_menu += "</select>";

    the_html = the_html.replace( "@PULLDOWNMENU", the_menu );

    String ifchecks = "if ( templatename == 'nosubmit' )\n    hide_input_boxes();\n  else\n";
    for ( Sparql_template tmplt : templates )
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

    the_html = the_html.replace("@CHANGESELECTIONCODE", ifchecks);

    send_response( t, the_html );
  }

  public static Map<String, String> get_args(String query, HttpExchange t)
  {
    Map<String, String> result = new HashMap<String, String>();

    parse_parameters( query, result );
    String body = post_body( t );

    parse_parameters( body, result );

    return result;
  }

  static void parse_parameters( String raw, Map<String,String> result )
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

  public static boolean is_update_query( String q )
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

  public String call_preprocessor( String url, String orig )
  {
    URL u;
    HttpURLConnection c;
    String orig_encoded;

    try
    {
      u = new URL(url + URLEncoder.encode(orig, "UTF-8") );
      c = (HttpURLConnection) u.openConnection();

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

  public static boolean is_normalform(String x)
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

  public static String fill_in_multi_input( String query, String needle, String values )
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
      String one_piece = splitted[i].trim();

      if ( one_piece.length() < 3 )
        continue;

      filter += (i==0? "" : " || ") + "?multival = <" + one_piece.substring(1,one_piece.length()-1) + ">";
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

  static String post_body( HttpExchange t )
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
