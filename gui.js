if(typeof String.prototype.trim !== 'function') {
  String.prototype.trim = function() {
    return this.replace(/^\s+|\s+$/g, '');
  }
}

function g(x)
{
 return document.getElementById(x);
}

function inputkey(e)
{
  if (e.keyCode=='13' && g("inputbox").value != "")
  {
    rdfstore_run(g('pulldown').value+'/'+parse_iri(g('inputbox').value));
    //g("inputbox").value="";
  }
}

function rdfstore_run(query)
{
  var url = encodeURIComponent(query);
  var xmlhttp;

  if ( window.XMLHttpRequest )
    xmlhttp = new XMLHttpRequest();
  else
    xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");

  xmlhttp.onreadystatechange=function()
  {
    if ( xmlhttp.readyState==4 && xmlhttp.status==200 )
      g("rdfstore_results").innerHTML = xmlhttp.responseText;
    else if ( xmlhttp.readyState==4 && xmlhttp.status != 200 )
      g("rdfstore_results").innerHTML = "There was an error getting the results.  Some possible reasons:<ol><li>You entered an invalid IRI</li><li>The ricordo rdfstore server is down</li><li>The rdf store itself is down</li><li>Your internet is disconnected</li></ol>";
  }

  xmlhttp.open("GET", url, true);

  xmlhttp.send();

  g("rdfstore_results").innerHTML = "...Loading...";
}

function pulldownchange()
{
  var pulldown = g("pulldown");
  var inputbox = g("inputbox");
  var templatename = pulldown.value;

  @CHANGESELECTIONCODE
}

function parse_iri(x)
{
  if ( x.charAt(0) == '<' && x.charAt(x.length-1) == '>' )
    x = x.substring(1,x.length-1);

  return x;
}
