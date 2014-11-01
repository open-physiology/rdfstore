
if(typeof String.prototype.trim !== 'function') {
  String.prototype.trim = function() {
    return this.replace(/^\s+|\s+$/g, '');
  }
}

/*
 * decodeEntities provided by Robert K from stackoverflow
 */
var decodeEntities = (function() {
  // this prevents any overhead from creating the object each time
  var element = document.createElement('div');

  function decodeHTMLEntities (str) {
    if(str && typeof str === 'string') {
      // strip script/html tags
      str = str.replace(/<script[^>]*>([\S\s]*?)<\/script>/gmi, '');
      str = str.replace(/<\/?\w(?:[^"'>]|"[^"]*"|'[^']*')*>/gmi, '');
      element.innerHTML = str;
      str = element.textContent;
      element.textContent = '';
    }

    return str;
  }

  return decodeHTMLEntities;
})();
/*
 * decodeEntities ends where
 */

function g(x)
{
 return document.getElementById(x);
}

function inputkey(e)
{
  if (e.keyCode=='13')
    rdfstore_run();
}

function rdfstore_run()
{
  var url = '/'+encodeURIComponent(g('pulldown').value)+'/';
  var fFirst = 0;
  var inputbox;

  for ( var i = 0; i <= 9; i++ )
  {
    inputbox = g("inputbox"+i);

    if ( inputbox.style.display != "none" )
    {
      if ( fFirst == 0 )
      {
        fFirst = 1;
        url += '?';
      }
      else
        url += '&';

      url += i + "=" + parse_iri(inputbox.value);
    }
  }

  var xmlhttp;

  if ( window.XMLHttpRequest )
    xmlhttp = new XMLHttpRequest();
  else
    xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");

  xmlhttp.onreadystatechange=function()
  {
    if ( xmlhttp.readyState==4 && xmlhttp.status==200 )
      g("rdfstore_results").innerHTML = xmlhttp.responseText + "<br>Click <a target='_blank' href='" + url + "'>here</a> to run the API command directly";
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
  var templatename = pulldown.value;

  @CHANGESELECTIONCODE
}

function parse_iri(x)
{
  if ( x.charAt(0) == '<' && x.charAt(x.length-1) == '>' )
    x = x.substring(1,x.length-1);

  return encodeURIComponent(x);
}

function hide_input_boxes()
{
  for ( var i = 0; i <= 9; i++ )
    g("inputbox"+i).style.display = 'none';
}

function show_input_boxes(x)
{
  for ( var i = 0; i <= 9; i++ )
    g("inputbox"+i).style.display = 'none';
  for ( var i = 0; i < x.length; i++ )
    g("inputbox"+x.charAt(i)).style.display = 'block';
}

function hide_input_button()
{
  g("buttondiv").style.visibility = 'hidden';
}

function show_input_button()
{
  g("buttondiv").style.visibility = 'visible';
}

function clear_placeholder(n)
{
  g("inputbox"+n).placeholder = "";
}

function set_placeholder(n,x)
{
  g("inputbox"+n).placeholder = decodeEntities(x);
}
