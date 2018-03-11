node {
   def mvnHome
   stage('Preparation') { 
      git 'https://github.com/drtran/forked-spring-petclinic.git'
      mvnHome = tool 'M3'
   }

   stage('Scan with SonarQube') {
      echo "Running SonarQube scan ..."
      
      if (isUnix()) {
         sh "'${mvnHome}/bin/mvn' clean test verify sonar:sonar"
      } else {
         bat(/"${mvnHome}\bin\mvn" clean test verify sonar:sonar/)
      }

      sh '/home/kiet/minishift/sonarqube_client check-coverage --site http://localhost:9000 --expected-coverage 84 > status'
      def scanStatus = readFile('status').trim()

      if (scanStatus.toLowerCase().contains("failed")) {
          error ("Scan Status: ${scanStatus}")
      }
      
      echo "Scan Status: ${scanStatus}"
   }

   stage('Local Build') {
      // Run the maven build
      if (isUnix()) {
         sh "'${mvnHome}/bin/mvn' package"
      } else {
         bat(/"${mvnHome}\bin\mvn" package/)
      }
   }
   
   /**
    * Here we should build an image - tag it somehow.
    * Then we should deploy that image on a dev environment
    * 
    */

   stage('OpenShift Build') {
      if (isUnix()) {
         sh "'/home/kiet/minishift/oc' login --username=dev --password=dev"
         sh "'/home/kiet/minishift/oc' start-build pet-clinic -n pet-clinic"
      } else {
         bat(/"${mvnHome}\bin\mvn" package/)
      }  
   }
   
   stage('Deploy on my local box') {
       echo "deploying ..."
       if (isUnix()) {
           sh "cp target/petclinic.war /home/kiet/csd-work/bin/apache-tomcat-8.5.28/webapps/."
       } else {
           bat "copy target\\petclinic.war c:\\dev\\bin\\apache-tomcat-8.5.28\\webapps\\."
       }
   }
   stage('Results') {
      echo 'Archiving ...'
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
}
