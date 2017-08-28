# loganalytics-logback-appender
[![Build Status](https://travis-ci.org/omnecon/loganalytics-logback-appender.svg?branch=master)](https://travis-ci.org/omnecon/loganalytics-logback-appender)
[![codecov](https://codecov.io/github/omnecon/loganalytics-logback-appender/coverage.svg?branch=master)](https://codecov.io/github/omnecon/loganalytics-logback-appender?branch=master)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Logback appender for forwarding log messages to Azure Log Analytics

Currently this github project is in pre-release state. The code is basically working, but is currently under test. Once finalized, the library will be released to Maven Central. Until then, please use the snapshot release ore build from source.

Maven Dependency (SNAPSHOT)

	<dependency>
	    <groupId>com.github.omnecon</groupId>
	    <artifactId>loganalytics-logback-appender</artifactId>
	    <version>1.0-SNAPSHOT</version>
	</dependency> 
	
Snapshot repository:

	<repository>
	    <snapshots>
	        <enabled>true</enabled>
	    </snapshots>
	    <id>sonartype-snapshots</id>
	    <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
	</repository>  
