/**
 * In order to run this pipeline, you would need the following configuration
 * from JenkinsCI:
 *
 * - a JDK 
 * - access to a git executable
 * - Maven settings as M3.
 * - You would need a permission to deploy a war to a running tomcat server.
 *   This tomcat server is defined in the pom.xml file from the 
 *   forked-spring-petclinic project (i.e., userid/psw: deployer/Tomcat123)
 * - You would need to run a sonarqube server to process the static code scan
 *   This sonarqube server is defined in your .m2/settings.xml file.
 * - You should configure your pipeline to run with Parameters. 
 *   + MISC_BIN should be set to where the sonarqube_client.exe resides
 *   + PROJECT_SRC_URL:
 *   + SONARQUBE_SITE:
 *   + EXPECTED_COVERAGE:
 *   + AUTOMATED_AT_SRC_URL:
 *   + SELENIUM_SERVER_URL:
 *
 * - You would need to have selenium server running somewhere. For example,
 *   
 *   java -Dwebdriver.driver=chrome 
 *        -Dwebdriver.chrome.driver=%CSD_BIN%\misc\chromedriver.exe 
 *        -jar %CSD_BIN%\misc\selenium-server-standalone-3.11.0.jar
 */
node {
   def mvnHome

  stage('Pulling Source Code from GIT') { 
      echo '**** Pulling Source Code from GIT ****'

      git '${PROJECT_SRC_URL}'
      mvnHome = tool 'M3'
  }

  stage('Scan source for technical debts & code coverage (SonarQube)') {
      echo '**** Scan source for technical debts & code coverage (SonarQube) ****'
      
      def site = "--site ${SONARQUBE_SITE}"
      def expectedCoverage = "--expected-coverage ${EXPECTED_COVERAGE}"

      if (isUnix()) {
        sonarqube_client = "${MISC_BIN}/sonarqube_client"
        sh "'${mvnHome}/bin/mvn' clean test verify sonar:sonar"
        sh "'${sonarqube_client}' check-coverage ${site} ${expectedCoverage} > status"
        
      } else {
         sonarqube_client = "${MISC_BIN}/sonarqube_client.exe"
         bat(/"${mvnHome}\\bin\\mvn" clean test verify sonar:sonar/)
         bat(/"${sonarqube_client}" check-coverage ${site} ${expectedCoverage} > status/)
      }

      def scanStatus = readFile('status').trim()
      

      if (scanStatus.toLowerCase().contains("failed")) {
          error ("Code Coverage: ${scanStatus}")
      }
      
      echo "Code Coverage: ${scanStatus}"
  }

  stage('Build binaries & deploy for functional testing') {
      echo '**** Build binaries & deploy for functional testing ****'

      if (isUnix()) {
        sh "'${mvnHome}/bin/mvn' tomcat7:undeploy"
        sh "'${mvnHome}/bin/mvn' tomcat7:deploy-only"
      } else {
        bat(/"${mvnHome}\\bin\\mvn" tomcat7:undeploy/)
        bat(/"${mvnHome}\\bin\\mvn" tomcat7:deploy-only/)
      }
  }

  stage('Archiving binaries & reports (unit tests & code coverage)') {
      echo '**** Archiving binaries & reports (unit tests & code coverage) ****'

      publishHTML(target: [
        allowMissing: true, 
        alwaysLinkToLastBuild: false, 
        keepAll: true, 
        reportDir: 'target/site/jacoco/', 
        reportFiles: 'index.html', 
        reportName: 'Code Coverage Report', 
        reportTitles: ''])
      junit '**/target/surefire-reports/TEST-*.xml'
      archive 'target/*.war'
  }


   stage('Run Automated Acceptance Tests (Cucumber/Selenium)') {
      echo '**** Run Automated Acceptance Tests (Cucumber/Selenium) **** '

      git '${AUTOMATED_AT_SRC_URL}'
      mvnHome = tool 'M3'

      def testName = "-Dtest=gov.dhs.nppd.devsecops.aat.RunSerenityTest"
      try {
        if (isUnix()) {
            def chromeDriver = "-Dwebdriver.driver=chrome -Dwebdriver.chrome.driver=/home/kiet/csd-work/bin/misc/chromedriver"
            wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
              sh "'${mvnHome}/bin/mvn' clean ${testName} ${chromeDriver} verify"
            }
        } else {
            def chromeDriver = "-Dwebdriver.remote.url=${SELENIUM_SERVER_URL} " +
                               "-Dwebdriver.remote.driver=chrome " + 
                               "-Dwebdriver.remote.os=WINDOWS"
            bat(/"${mvnHome}\\bin\\mvn" clean ${testName} ${chromeDriver} verify/) 
        }
      } catch (e) {
          echo "making serenity report ..."
          if (isUnix()) {
            sh "'${mvnHome}/bin/mvn' -Dmaven.test.skip=true verify"
          } else {
            bat(/"${mvnHome}\\bin\\mvn" -Dmaven.test.skip=true verify/) 
          }
          publishHTML(target: [
          allowMissing: true, 
          alwaysLinkToLastBuild: false, 
          keepAll: true, 
          reportDir: 'target/site/serenity/', 
          reportFiles: 'index.html', 
          reportName: 'Serenity Report (Acceptance Tests)',
          reportTitles: ''])
          throw e
      }
   }

   stage('Archiving Serenity (Cucumber/Selenium) Report') {
      echo '**** Archiving Serenity (Cucumber/Selenium) Report ****'

      publishHTML(target: [
        allowMissing: true, 
        alwaysLinkToLastBuild: false, 
        keepAll: true, 
        reportDir: 'target/site/serenity/', 
        reportFiles: 'index.html', 
        reportName: 'Serenity Report (Acceptance Tests)',
        reportTitles: ''])
   }
}
