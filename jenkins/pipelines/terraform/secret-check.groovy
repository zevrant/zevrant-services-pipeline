package terraform
/*<<<
[{
	"jobName": "secret-check",
	"logFileDays": 14,
	"description": "Check expected Secret value against what is in GCP",
	"displayName": "Secret Checker",
	"jenkinsFileName": "source/main/pipelines/terraform/secret-check.groovy",
	"pipelineParameters": [{
			"type": "ChoicePipelineParameter",
			"name": "ENVIRONMENT",
			"description": "Primary region for storage accounts",
			"choices": [
				"sandbox",
				"dev",
				"qa",
				"stage",
				"prod"
			]
		},
		{
			"type": "PipelineParameter",
			"name": "SECRET_NAME",
			"description": "name of the secret in terraform, should match the regexp ^[a-zA-Z0-9_]+$"
		}
	],
	"passwordName": "SECRET_VALUE"
}]
>>>*/
@Library("CommonUtils")


if(ENVIRONMENT == '' || ENVIRONMENT == null) {
    throw new RuntimeException('Environment must be provided')
}

if(SECRET_VALUE == '' || SECRET_VALUE == null) {
    throw new RuntimeException('Secret Value must be provided')
}
pipeline {
    agent {
        kubernetes {
            inheritFrom 'gcloud'
        }
    }

    stages {
        stage ('Get Secret') {
            steps {
                script {
                    container('gcloud') {
                        gCloud.withContext({
                            if(gCloud.doesSecretExist("${ENVIRONMENT}_${SECRET_NAME}", gcpProject.getId()) ) {
                                String secretValue = gCloud.getSecretValue("${ENVIRONMENT}_${SECRET_NAME}", gcpProject.getId())
                                if (SECRET_VALUE != secretValue) {
                                    currentBuild.result = hudson.model.Result.UNSTABLE.toString()
                                    currentBuild.description = "Secret value mismatch detected"
                                }
                            } else {
                                currentBuild.result = hudson.model.Result.UNSTABLE.toString()
                                currentBuild.description = "Secret $SECRET_NAME could not be found"
                            }
                        })
                    }
                }
            }
        }
    }
}
