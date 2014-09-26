Rdfstore is a frontend that allows users to query a SPARQL without actually writing SPARQL
queries, by means of templates.  This is an alpha-version.  Rdfstore was first written by
Sarala Wimaralatne, rewritten by Sam Alexander.  This is an unofficial release, please wait
for a later release for a license and better documentation.

INSTALLATION

1. Ensure you have a java runtime environment and a java compiler installed.
2. Git clone this repository into a working directory of your choice
3. Within the directory, "make" to compile.
   Alternately you can manually compile with "javac -g Rdfstore.java"
4. Once compiled, run "java Rdfstore -help" for further details.
   In order to get Rdfstore running, it is assumed there is a particular SPARQL
   endpoint already running that you want Rdfstore to talk to.
   See the "example" below.
5. For convenience, once Rdfstore is running, it includes a simple GUI.
   For example, if you're running Rdfstore on port 20060 (the default), you can
   access the GUI at http://localhost:20060/gui

Example:

Suppose you have a Fuseki sparql endpoint running on localhost:3030.
You manually query it and find that the query URL looks like this:
     
http://localhost:3030/ddmore/query?query=SELECT+DISTINCT+%3Fp+WHERE+%7B%3Fs+%3Fp+%3Fo%7D&output=tsv&stylesheet=&force-accept=text%2Fplain

Rearrange the tokens so that the query token comes last:

http://localhost:3030/ddmore/query?output=tsv&stylesheet=&force-accept=text%2Fplain&query=SELECT+DISTINCT+%3Fp+WHERE+%7B%3Fs+%3Fp+%3Fo%7D

Remove the query itself but leave the "&query=" part in:

http://localhost:3030/ddmore/query?output=tsv&stylesheet=&force-accept=text%2Fplain&query=

That's the address you'll give Rdfstore to use to talk to the SPARQL endpoint.
To run Rdfstore with no additional configuration changes:

java Rdfstore -endpoint "http://localhost:3030/ddmore/query?output=tsv&stylesheet=&force-accept=text%2Fplain&query="
