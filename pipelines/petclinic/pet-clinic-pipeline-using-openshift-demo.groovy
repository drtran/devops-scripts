/**
 * These parameters are for Openshift:
 * 
 * - String parameter named KUBERNETES_SERVICE_HOST and set the
 * 	    Default Value as your OpenShift host URL.
 * - Boolean parameter named SKIP_TLS checked by default.
 * - Password parameter named AUTH_TOKEN set to your token value. 
 * 	    You can login to the same OpenShift server using oc login on your workstation 
 * 	    and run oc whoami -t to get the token value to use here.
 * 
 * These parameters are for the pipeline code:
 *
 * - String parameter PROJECT_NAME (nsd-poc)
 * - String parameter APP_NAME (pet-clinic)
 * - String parameter RELEASE (6.7.4.3)
 * - String parameter PROJECT_SRC_URL (https://github.com/drtran/forked-spring-petclinic)
 * - String parameter OPENSHIFT_BASE_IMAGE (openshift/wildfly:10.1)
 * - Boolean paramter TO_CREATE_BUILD (false)
 * - Boolean parameter TO_START_BUILD (false)
 * - Boolean parameter TO_TAG_IMAGE (false. ie.: latest --> RELEASE)
 * - MISC_BIN should be set to where these resides: oc, openshift_query
 * 
 * - You need to run minishift (or access to openshift platform):
 *      minishift start --vm-driver virtualbox
 *
 * - You need to have the following program in 
 */
node {
   
    def release = env.RELEASE
    def projectName = env.PROJECT_NAME
    def appName = env.APP_NAME
    def projectSrcUrl = env.PROJECT_SRC_URL
    def openshiftBaseImage = env.OPENSHIFT_BASE_IMAGE

    releaseNoDots = release.replaceAll("\\.","")
    
    echo "Release: ${release}"
    echo "releaseNoDots: ${releaseNoDots}"
    ocAppName = "${appName}-${releaseNoDots}"

    stage ('cleanup') {
        echo "Cleaning things up first ..."
    	if (!isUnix()) {
            oc = "${MISC_BIN}/oc"
            openshift_query = "${MISC_BIN}/openshift_query"

    		try {
    			bat(/${oc} delete route ${ocAppName}/)
    			bat(/${oc} delete service ${ocAppName}/)
    			bat(/${oc} delete dc ${ocAppName}/)

                if (env.TO_CREATE_BUILD == 'true') {
                    echo "Deleting a build ..."
                    bat(/${oc} delete imagestream ${appName}/)
                    bat(/${oc} delete buildConfig ${appName}/)
                }
    		} catch (e) {
    			echo 'Resources probably do not exist!'
    		}
    	}
    }

    stage ('create') {
        if (env.TO_CREATE_BUILD == 'true') {
            echo "Creating a build ..."
            if (!isUnix()) {
                bat(/${oc} new-build ${projectSrcUrl} --name="${appName}" --image-stream="${openshiftBaseImage}"/)
                bat(/${openshift_query} waitForBuild ${appName}/)
            }
        }
    }
    
    stage ('build') {
        
    	if (env.TO_START_BUILD == 'true') {
            echo "Starting a build ..."
            if (!isUnix()) {
                bat(/${oc} start-build "${appName}"/)
                bat(/${openshift_query} waitForBuild ${appName}/)
            }
        }
    }
    stage ('tag') {
        
        if (env.TO_TAG_IMAGE == 'true') {
            echo "Tagging an image ..."
            fromAppName = "${appName}:latest"
            toAppName = "${appName}:${release}"
            bat(/${oc} tag ${fromAppName} ${toAppName}/)
        }
    }
    
    stage ('deploy') {
        echo "Deploying an application ..."
    	if (!isUnix()) {
    		releaseImageName = "${APP_NAME}:${release}"
    		appImageName = "imagestreams/${APP_NAME}"
    		bat(/${oc} describe ${appImageName}/)
    		bat(/${oc} new-app ${releaseImageName} --name=${ocAppName}/)
    	}
	}
	
	stage ('expose service') {
        echo "Making application avaible for use ..."
	    if (!isUnix()) {
	    	try {
	        	bat(/${oc} expose services\/${ocAppName}/)
	        } catch (e) {
	        	echo 'service may already exist'
	        }
	    }
	}
	
	
}