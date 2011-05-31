Releasing new Plugin Version
========================================================================================================================
1. pull the most recent code base from the git repository
3. update the plugin versions within grails-app/conf/BuildConfig.groovy if necessary
2. adjust the version number in the file RiskAnalyticsGraphCoreGrailsPlugin.groovy to i.e. 0.7.10 in the line starting
   with 'def version ='
3. run the ant target cruise locally
4. if the cruise target succeeded run the Grails command (Ctrl + Alt + G) package-plugin
5. running the package-plugin command will modify the version number in plugin.xml and create a new zip file
   called grails-art-models-*.zip
6. commit all changes except the zip file to the git repository and tag the version
   git tag -m 'tagged release version 0.7.10' 'v0.7.10'
   git push origin master --tags