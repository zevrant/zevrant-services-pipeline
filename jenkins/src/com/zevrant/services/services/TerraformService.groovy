package com.zevrant.services.services

import com.zevrant.services.enumerations.SecretType
import com.zevrant.services.pojo.SecretMapping
import com.zevrant.services.pojo.codeunit.TerraformCodeUnit

class TerraformService extends Service {

    private final SecretsService secretsService

    TerraformService(Object pipelineContext) {
        super(pipelineContext)
        this.secretsService = new SecretsService(pipelineContext)
    }

    void initTerraform(String environmentName) {
        pipelineContext.dir("terraform-${environmentName}") {
            pipelineContext.sh('mkdir -p ~/.ssh')
            pipelineContext.sshagent(credentials: ['jenkins-git']) {
                pipelineContext.sh('terraform init')
            }
        }
    }

    void planTerraform(String environmentName) {
        pipelineContext.dir("terraform-${environmentName}") {
            pipelineContext.sh("terraform plan -out ${environmentName}.plan")
            pipelineContext.sh("terraform show -json ${environmentName}.plan | jq . > ${environmentName}.json")
        }
    }

    /**
     *
     * This method uses the SecretsService to pull the credentials listed for a given code unit and environment
     * and populates them using the TF_VAR prefix allowing the credentials to be passed in without needing to write them
     * to disk and risk spillage
     *
     * @param terraformCodeUnit - which code unit to pull credentials for
     * @param environmentName - which environment in that code unit should the credentials be mapped to
     */
    void populateTfEnvVars(TerraformCodeUnit terraformCodeUnit, String environmentName, Closure runWithVars) {
        Map<String, Object> environmentConfig = terraformCodeUnit.getConfigForEnv(environmentName)
        String vaultToken = ""

        pipelineContext.withCredentials([pipelineContext.usernamePassword(credentialsId: 'local-vault', passwordVariable: 'password', usernameVariable: 'username')]) {
            vaultToken = secretsService.getLocalApiToken(pipelineContext.username, pipelineContext.password)
        }

        List<String> configMappings = []

        environmentConfig.keySet().each { key ->
            Object value = environmentConfig.get(key)
            if (value instanceof SecretMapping) {
                SecretMapping mapping = value as SecretMapping
                Map<String, Object> response = [:]
                pipelineContext.println("Secret type is ${mapping.secretType.name()}")
                if (mapping.secretType != SecretType.HCP_CLIENT || mapping.secretType != SecretType.VAULT_TOKEN) {
                    response = secretsService.getLocalSecret(vaultToken, mapping.getSecretPath())
                }
                switch (mapping.secretType) {
                    case SecretType.USERNAME_PASSWORD:
                        configMappings.add("TF_VAR_${key}_username=" + response.username)
                        configMappings.add("TF_VAR_${key}_password=" + response.password)
                        break
                    case SecretType.SECRET_TEXT:
                        configMappings.add("TF_VAR_${key}=" + response.password)
                        break
                    case SecretType.VAULT_TOKEN:
                        configMappings.add("TF_VAR_${key}=" + vaultToken)
                        break
                    case SecretType.HCP_CLIENT:
                        pipelineContext.withCredentials([pipelineContext.usernamePassword(credentialsId: 'local-vault', passwordVariable: 'password', usernameVariable: 'username')]) {
                            configMappings.add("TF_VAR_${key}_username=" + pipelineContext.username)
                            configMappings.add("TF_VAR_${key}_password=" + pipelineContext.password)
                        }
                        break
                    default:
                        throw new RuntimeException("Secret type for vault has not been implemented!")
                }

            } else {
                configMappings.add("TF_VAR_${key}=" + value.toString())
            }
        }

        pipelineContext.withEnv(configMappings) {
            runWithVars.run()
        }

    }
}
