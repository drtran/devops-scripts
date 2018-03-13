/**
 *
 * String parameter named KUBERNETES_SERVICE_HOST and set the
 * 	Default Value as your OpenShift host URL.
 * Boolean parameter named SKIP_TLS checked by default.
 * Password parameter named AUTH_TOKEN set to your token value. 
 * 	You can login to the same OpenShift server using oc login on your workstation 
 * 	and run oc whoami -t to get the token value to use here.
 *
 */
node {
    // need string parameter PROJECT_NAME
    // need string parameter APP_NAME
    stage ('preamble') {

    }

    stage ('cleanup') {

    }

    stage ('create') {
        
    }
    
    stage ('build') {
    	openshiftBuild(namespace: '${PROJECT_NAME}', buildConfig: '${APP_NAME}', showBuildLogs: 'true')
    }
    
    stage ('deploy') {
    	openshiftDeploy(namespace: '${PROJECT_NAME}', deploymentConfig: '${APP_NAME}')
    	openshiftScale(namespace: '${PROJECT_NAME}', deploymentConfig: '${APP_NAME}',replicaCount: '2')
    	openshiftVerifyDeployment(namespace: '${PROJECT_NAME}', deploymentConfig: '${APP_NAME}')
	}
	
	stage ('expose service') {
	    if (isUnix()) {
	    	try {
	        	sh "'${mypath}/oc' expose services/${APP_NAME}'"
	        } catch (e) {
	        	echo 'service may already exist'
	        }
	    } else {
	        
	    }
	}
	
	stage ('tag') {
	    // consider tag the current image with a unique value.
	}
}