<html>
<head>
<title>RDFStore Test GUI</title>

<link rel="stylesheet" href="stylesheets/base.css">
<link rel="stylesheet" href="stylesheets/skeleton.css">

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
    <hr>
    <div id='rightpadding'>
      <input id="inputbox0" onKeyUp="inputkey(event);" style="width:100%; display:none">
      <input id="inputbox1" onKeyUp="inputkey(event);" style="width:100%; display:none">
      <input id="inputbox2" onKeyUp="inputkey(event);" style="width:100%; display:none">
      <input id="inputbox3" onKeyUp="inputkey(event);" style="width:100%; display:none">
      <input id="inputbox4" onKeyUp="inputkey(event);" style="width:100%; display:none">
      <input id="inputbox5" onKeyUp="inputkey(event);" style="width:100%; display:none">
      <input id="inputbox6" onKeyUp="inputkey(event);" style="width:100%; display:none">
      <input id="inputbox7" onKeyUp="inputkey(event);" style="width:100%; display:none">
      <input id="inputbox8" onKeyUp="inputkey(event);" style="width:100%; display:none">
      <input id="inputbox9" onKeyUp="inputkey(event);" style="width:100%; display:none">
      <div style="visibility:hidden" id="buttondiv">
        <br>
        <button onclick="rdfstore_run();" style="width:100%">Enter</button>
      </div>
      <div id="rdfstore_results">
      </div>
    </div>
  </div>

</div>

</body>
