@Library('CommonUtils') _


pipeline {
    agent {
        label 'master-node'
    }
    stages {

        stage("Process Seed File") {
            steps {
                script {
                    sh 'ls -l'
                    jobDsl(
//                            targets: 'jenkins/seed.groovy',
                            removedJobAction: 'DELETE',
                            removedViewAction: 'DELETE',
                            removedConfigFilesAction: 'DELETE',
                            lookupStrategy: 'SEED_JOB',
                            failOnMissingPlugin: true,
                            additionalClasspath: 'jenkins/src/main/groovy', //only works with
                            useScriptText: true,
                            scriptText: """
import com.zevrant.services.enumerations.ApplicationType
import com.zevrant.services.enumerations.PipelineTriggerType
import com.zevrant.services.pojo.PipelineTrigger
import com.zevrant.services.pojo.PipelineParameter
import com.zevrant.services.pojo.Pipeline
import com.zevrant.services.pojo.containers.Image
import com.zevrant.services.pojo.PipelineCollection
import com.zevrant.services.pojo.KubernetesServiceCollection
import com.zevrant.services.pojo.codeunit.LibraryCodeUnitCollection
import com.zevrant.services.pojo.codeunit.AndroidCodeUnitCollection
import com.zevrant.services.pojo.codeunit.SpringCodeUnitCollection
import com.zevrant.services.pojo.codeunit.CodeUnit
import com.zevrant.services.services.JobDslService

 new JobDslService(this).createMultibranch(new CodeUnit([
        name: 'jenkins-cac',
        applicationType: ApplicationType.JENKINS_CAC
]))
"""
                    )
                }
            }
        }
    }
}
