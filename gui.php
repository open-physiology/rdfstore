<html>
<head>
<title>RDFStore Test GUI</title>

<link rel="stylesheet" href="//www.semitrivial.com/base.css">
<link rel="stylesheet" href="//www.semitrivial.com/skeleton.css">

@JAVASCRIPT

</head>
<body>

<div class='container' id='maindiv'>
  <div class='sixteen columns'>
    <h1>RDFStore Test Gui</h1>
  </div>
  <div class='sixteen columns'>
    <hr>
  </div>

  <div class='seven columns' id='leftdiv'>
    <h2>Select Template</h2>
    <div>
      Select a query template for querying the triple store.  (The "get_resources" and "get_relations" templates can be used to see what sort of
      things are available on the sandbox.)
    </div>
    <hr>
    <div>
      @PULLDOWNMENU
    </div>
  </div>

  <div class='nine columns' id='rightdiv'>
    <h2>Query using selected template</h2>
    <div>
      Specify which IRI to pass to the query.
    </div>
    <hr>
    <div id='rightpadding'>
      <input id="inputbox" onKeyUp="inputkey(event);" style="width:100%">
      <div>
        <button onclick="rdfstore_run(g('pulldown').value+'/'+parse_iri(g('inputbox').value));" style="width:100%">Enter</button>
      </div>
      <div id="rdfstore_results">
      </div>
    </div>
  </div>

</div>

</body>
