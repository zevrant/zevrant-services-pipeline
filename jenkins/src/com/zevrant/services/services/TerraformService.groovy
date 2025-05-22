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
        pipelineContext.sh("ls -l")
        pipelineContext.dir("terraform-${environmentName}") {
            pipelineContext.sh('mkdir -p ~/.ssh')
            pipelineContext.sshagent(credentials: ['jenkins-git']) {
                pipelineContext.sh('terraform init')
                pipelineContext.sh('pwd')
                pipelineContext.sh("terraform workspace select ${environmentName}")
            }
        }
    }

    void planTerraform(String environmentName) {
        pipelineContext.dir("terraform-${environmentName}") {
            pipelineContext.sh("terraform plan -out ${environmentName}.plan")
            pipelineContext.sh("terraform show -json ${environmentName}.plan | jq . > ${environmentName}.json")
        }
    }

    void applyTerraform(String environmentName) {
        pipelineContext.dir("terraform-${environmentName}") {
            pipelineContext.sh("terraform plan -out ${environmentName}.plan")
            pipelineContext.sh("terraform show -json ${environmentName}.plan | jq . > ${environmentName}.json")
            pipelineContext.sh("terraform apply -auto-approve ${environmentName}.plan")
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
            pipelineContext.println("Populating variable ${key} with value of class ${value.getClass()}")
            if (value.getClass() == ArrayList.class) {
                pipelineContext.println("and with a arraylist item class of ${((ArrayList) value).get(0).getClass()}")
            }
            if (value instanceof SecretMapping) {
                SecretMapping mapping = value as SecretMapping
                Map<String, Object> response = [:]
                pipelineContext.println("Secret type is ${mapping.secretType.name()}")
                if (mapping.secretType != SecretType.HCP_CLIENT && mapping.secretType != SecretType.VAULT_TOKEN) {
                    response = secretsService.getLocalSecret(vaultToken, mapping.getSecretPath())
                }
                String prefix = (mapping.stripPrefix) ? '' : 'TF_VAR_'
                switch (mapping.secretType) {
                    case SecretType.USERNAME_PASSWORD:
                        configMappings.add("${prefix}${key}_username=" + response.username)
                        configMappings.add("${prefix}${key}_password=" + response.password)
                        break
                    case SecretType.SECRET_TEXT:
                        configMappings.add("${prefix}${key}=" + response.password)
                        break
                    case SecretType.VAULT_TOKEN:
                        configMappings.add("${prefix}${key}=" + vaultToken)
                        break
                    case SecretType.HCP_CLIENT:
                        pipelineContext.withCredentials([pipelineContext.usernamePassword(credentialsId: 'vault-cloud-credentials', passwordVariable: 'password', usernameVariable: 'username')]) {
                            configMappings.add("${prefix}${key}_username=" + pipelineContext.username)
                            configMappings.add("${prefix}${key}_password=" + pipelineContext.password)
                        }
                        break
                    default:
                        throw new RuntimeException("Secret type for vault has not been implemented!")
                }

            } else if (value instanceof Map && key != 'trigger' || (value instanceof ArrayList && value.get(0) instanceof LinkedHashMap)) {
                String valueJson = pipelineContext.writeJSON(json: value, returnText: true)
                valueJson = valueJson.replace(": ", "= ")
                        .replace('":"', '"="')
                configMappings.add(
                        "TF_VAR_${key}=" + valueJson)
            } else {
                configMappings.add("TF_VAR_${key}=" + value.toString())
            }
        }

        pipelineContext.withEnv(configMappings) {
            runWithVars.run()
        }

    }
}
