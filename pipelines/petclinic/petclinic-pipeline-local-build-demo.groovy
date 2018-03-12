node {
   def mvnHome

   stage('Pulling Source Code from GIT') { 
      echo '**** Pulling Source Code from GIT ****'

      git 'https://github.com/drtran/forked-spring-petclinic.git'
      mvnHome = tool 'M3'
   }

   stage('Scan source for technical debts & code coverage (SonarQube)') {
      echo '**** Scan source for technical debts & code coverage (SonarQube) ****'
      
      def site = "--site http://localhost:9000"
      def expectedCoverage = "--expected-coverage 83"

      if (isUnix()) {
        sh "'${mvnHome}/bin/mvn' clean test verify sonar:sonar"
        sh "'/home/kiet/minishift/go-projects/sonarqube_client' check-coverage ${site} ${expectedCoverage} > status"
        
      } else {
         bat(/"${mvnHome}\\bin\\mvn" clean test verify sonar:sonar/)
         bat(/"c:\\minishift\\sonarqube_client.exe" check-coverage ${site} ${expectedCoverage} > status/)
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
         sh "'${mvnHome}/bin/mvn' package"
      } else {
         bat(/"${mvnHome}\\bin\\mvn" package/)
      }

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

      git 'https://github.com/drtran/aat.git'
      mvnHome = tool 'M3'

      def testName = "-Dtest=gov.dhs.nppd.devsecops.aat.RunSerenityTest"
      
      if (isUnix()) {
        def chromeDriver = "-Dwebdriver.driver=chrome -Dwebdriver.chrome.driver=/home/kiet/csd-work/bin/misc/chromedriver"
        sh "'${mvnHome}/bin/mvn' clean ${testName} ${chromeDriver} verify"
      } else {
         def chromeDriver = "-Dwebdriver.driver=chrome -Dwebdriver.chrome.driver=c:\\dev\\bin\\misc\\chromedriver.exe"
         bat(/"${mvnHome}\\bin\\mvn" clean ${testName} ${chromeDriver} verify/) 
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
