grails.project.dependency.resolver = "maven"

grails.project.dependency.resolution = {
    inherits "global" // inherit Grails' default dependencies
    log "warn"

    repositories {
        grailsHome()
        grailsCentral()

        mavenCentral()
        mavenRepo "https://repository.intuitive-collaboration.com/nexus/content/repositories/pillarone-public/"
        mavenRepo("https://repository.intuitive-collaboration.com/nexus/content/repositories/pillarone-public-snapshot/") {
            updatePolicy System.getProperty('snapshotUpdatePolicy') ?: 'daily'
        }
        mavenRepo "http://repo.spring.io/milestone/" //needed for spring-security-core 2.0-rc2 plugin
    }


    plugins {
        runtime ":background-thread:1.3"
        runtime ":hibernate:3.6.10.3"
        runtime ":joda-time:0.5"
        runtime ":release:3.0.1"
        runtime ":quartz:1.0.1"
        runtime ":spring-security-core:2.0-RC2"
        runtime ":tomcat:7.0.42"

        test ":code-coverage:1.2.7"

        if (appName == "risk-analytics-graph-core") {
            runtime "org.pillarone:risk-analytics-core:1.9-SNAPSHOT"
        }
    }
    dependencies {
        test 'hsqldb:hsqldb:1.8.0.10'
    }
}

grails.project.dependency.distribution = {
    String password = ""
    String user = ""
    String scpUrl = ""
    try {
        Properties properties = new Properties()
        String version = new GroovyClassLoader().loadClass('RiskAnalyticsGraphCoreGrailsPlugin').newInstance().version
        properties.load(new File("${userHome}/deployInfo.properties").newInputStream())
        user = properties.get("user")
        password = properties.get("password")

        if (version?.endsWith('-SNAPSHOT')) {
            scpUrl = properties.get("urlSnapshot")
        } else {
            scpUrl = properties.get("url")
        }
        remoteRepository(id: "pillarone", url: scpUrl) {
            authentication username: user, password: password
        }
    } catch (Throwable t) {
    }
}

coverage {
    exclusions = [
            'models/**',
            '**/*Test*',
            '**/com/energizedwork/grails/plugins/jodatime/**',
            '**/grails/util/**',
            '**/org/codehaus/**',
            '**/org/grails/**',
            '**GrailsPlugin**',
            '**TagLib**'
    ]


}
