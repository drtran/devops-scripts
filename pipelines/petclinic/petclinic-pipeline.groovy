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

   }
   stage('Build') {
      // Run the maven build
      if (isUnix()) {
         sh "'${mvnHome}/bin/mvn' package"
      } else {
         bat(/"${mvnHome}\bin\mvn" package/)
      }
   }
   stage('Deploy') {
       echo "deploying ..."
       if (isUnix()) {
           sh "cp target/petclinic.war /home/kiet/csd-work/bin/apache-tomcat-8.5.28/webapps/."
       } else {
           bat "copy target/petclinic.war c:/csd-work/bin/apache-tomcat-8.5.28/webapps/."
       }
   }
   stage('Results') {
      echo 'Archiving ...'
      junit '**/target/surefire-reports/TEST-*.xml'
      archive 'target/*.war'
   }
}
