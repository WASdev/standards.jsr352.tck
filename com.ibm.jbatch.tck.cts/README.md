# Instructions for building CTS (assumes up-to-date com.ibm.jbatch.tck module)

 1. mvn clean
 1. mvn process-sources -Dcts
 1. ant -Dtransform.dir=working.basedir/javaSource/
 1. mvn assembly:single -Dcts
