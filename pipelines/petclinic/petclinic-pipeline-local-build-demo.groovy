node {
   def mvnHome

   stage('Pulling Source Code') { 
      echo '**** Pulling Source Code ****'

      git 'https://github.com/drtran/forked-spring-petclinic.git'
      mvnHome = tool 'M3'
   }

   stage('Scan with SonarQube') {
      echo '**** Scan with SonarQube ****'
      
      def site = "--site http://localhost:9000"
      def expectedCoverage = "--expected-coverage 83"

      if (isUnix()) {
        sh "'${mvnHome}/bin/mvn' clean test verify sonar:sonar"
        sh "'/home/kiet/minishift/go-projects/sonarqube_client' check-coverage ${site} ${expectedCoverage} > status"
        
      } else {
         bat(/"${mvnHome}\\bin\\mvn" clean test verify sonar:sonar/)
      }

      def scanStatus = readFile('status').trim()
      

      if (scanStatus.toLowerCase().contains("failed")) {
          error ("Code Coverage: ${scanStatus}")
      }
      
      echo "Code Coverage: ${scanStatus}"
   }

  stage('Build Locally') {
      echo '**** Build Locally ****'

      if (isUnix()) {
         sh "'${mvnHome}/bin/mvn' package"
      } else {
         bat(/"${mvnHome}\\bin\\mvn" package/)
      }
   }
   
  stage('Deploy Locally for Testing') {
    echo '**** Deploy Locally for Testing ****'

      if (isUnix()) {
        sh "'${mvnHome}/bin/mvn' tomcat7:undeploy"
        sh "'${mvnHome}/bin/mvn' tomcat7:deploy-only"
      } else {
        bat(/"${mvnHome}\\bin\\mvn" tomcat7:undeploy/)
        bat(/"${mvnHome}\\bin\\mvn" tomcat7:deploy-only/)
      }
   }

   stage('Finalize Results for Petclinic Project') {
      echo '**** Finalize Results for Petclinic Project ****'

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


   stage('Deploy Automated Acceptance Tests') {
      echo 'Deploy Automated Acceptance Tests'
      echo '**** Pulling Source Code ****'

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

   stage('Finalize Results for Automated Acceptance Tests') {
      echo '**** Finalize Results for Automated Acceptance Tests ****'

      publishHTML(target: [
        allowMissing: true, 
        alwaysLinkToLastBuild: false, 
        keepAll: true, 
        reportDir: 'target/site/serenity/', 
        reportFiles: 'index.html', 
        reportName: 'Acceptance Tests Report', 
        reportTitles: ''])
   }
}
