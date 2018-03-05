node {
   def mvnHome
   stage('Preparation') { 
      git 'https://github.com/drtran/forked-spring-petclinic.git'
      mvnHome = tool 'M3'
   }
   stage('Scan') {
       echo "Running SonarQube scan ..."
   }
   stage('Build') {
      // Run the maven build
      if (isUnix()) {
         sh "'${mvnHome}/bin/mvn' clean package"
      } else {
         bat(/"${mvnHome}\bin\mvn" -Dmaven.test.failure.ignore clean package/)
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
