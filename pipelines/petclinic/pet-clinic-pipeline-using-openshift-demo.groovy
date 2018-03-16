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
    // need string parameter RELEASE (a.b.c.d.etc.)
    
    if (!env.RELEASE) {
    	RELEASE  = "latest"
    }

    release_no_dots = RELEASE.replaceAll("\\.","")
    
    echo "RELEASE: ${RELEASE}"
    echo "release_no_dots: ${release_no_dots}"
    oc_app_name = "${APP_NAME}-${release_no_dots}"

    stage ('cleanup') {
    	if (!isUnix()) {
    		try {
    			bat(/oc delete route ${oc_app_name}/)
    			bat(/oc delete service ${oc_app_name}/)
    			bat(/oc delete dc ${oc_app_name}/)
    		} catch (e) {
    			echo 'Resources probably do not exist!'
    		}
    	}
    }

    stage ('create') {
        
    }
    
    stage ('build') {
    	// openshiftBuild(namespace: '${PROJECT_NAME}', buildConfig: '${APP_NAME}', showBuildLogs: 'true')
    }
    
    stage ('deploy') {
    	if (!isUnix()) {
    		release_image_name = "${APP_NAME}:${RELEASE}"
    		app_image_name = "imagestreams/${APP_NAME}"
    		bat(/oc describe ${app_image_name}/)
    		bat(/oc new-app ${release_image_name} --name=${oc_app_name}/)
    	}
	}
	
	stage ('expose service') {
	    if (!isUnix()) {
	    	try {
	        	bat(/oc expose services\/${oc_app_name}/)
	        } catch (e) {
	        	echo 'service may already exist'
	        }
	    }
	}
	
	stage ('tag') {
	    // consider tag the current image with a unique value.
	}
}