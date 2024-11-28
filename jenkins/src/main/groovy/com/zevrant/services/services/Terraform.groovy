package com.zevrant.services.services

String updateTfVars(String tfvars, String secretName, String secretValue) {
    secretValue = secretValue.replace('"', '\\"')
    //Managing formatting using gstrings was not working consistently and since we need to write it to a file anyhow this seemed a better solution
    StringWriter stringWriter = new StringWriter()
    BufferedWriter tfVarsWriter = new BufferedWriter(stringWriter)
    boolean matches = false
    List<String> tfLines = []
    tfvars.split('\n').each {//this is more cleanup code than anything
        List<String> malformedLines = it.findAll('\\w+ = "[\\w\\h]+"')
        if (malformedLines.size() > 0) {
            malformedLines.each { line -> tfLines.add(line) }
        } else {
            tfLines.add(it)
        }
    }
    //making the list unique will prevent us from ending up with duplicates and having failures during the next terraform apply
    tfLines.toUnique().each { line ->
        if (line != '' || !line.matches('\\h*')) {
            boolean localMatches = line.matches("^${secretName}\\h*=\\h*.+\$")
            matches = matches || localMatches
            if (localMatches) {
                println "Overriding secret value"
                tfVarsWriter.writeLine(secretName + ' = "' + secretValue + '"')
//do not convert to GString, this will cause potential for secret spillage
            } else {
                tfVarsWriter.writeLine(line)
            }
        }
    }
//not necessary but definitely useful for troubleshooting the job should an error occur
    println "secret found: $matches"
    if (!matches) {
        println "Adding secret to file"
        tfVarsWriter.writeLine(secretName + ' = "' + secretValue + '"')
        //do not convert to GString, this will cause potential for secret spillage
    }
    tfVarsWriter.flush()
    tfVarsWriter.close()
    return stringWriter.toString()
}

void planTerraform(String environment, String rootDir = 'envs', String gitCredentialsId) {
    dir("${rootDir}/$environment") {
        sh 'mkdir -p ~/.ssh'
        String planName = "${environment}.plan"
        sh 'touch encrypted.tfvars'
        sshagent(credentials: [gitCredentialsId]) {
            sh 'terraform init'
            sh "terraform plan -out ${planName} --parallelism=100 --var-file terraform.tfvars --var-file encrypted.tfvars"
            sh "terraform show -json ${planName} | jq . > ${environment}.json"
        }
    }
}

void applyTerraform(String environment, String rootDir = 'envs') {

    if (!fileExists(file: "envs/${environment}/${environment}.plan")) {
        planTerraform(environment, rootDir)
    }
    dir("${rootDir}/$environment") {
        sh "terraform apply --parallelism=100 --auto-approve -var-file terraform.tfvars -var-file encrypted.tfvars -state=${environment}.plan"
    }
}

String scrubPlan(String jsonPlanFilePath, String plan) {
    List<String> secrets = findSecrets(jsonPlanFilePath, plan, null)
    String jsonPlan = (plan == null || plan == '') ? readFile(file: jsonPlanFilePath) : plan
    if (!secrets.isEmpty()) {
        secrets.each { secret ->
            jsonPlan = jsonPlan.replace(secret.toString(), "*****************")
        }
        jsonPlan.replaceAll('"db_instance_root_pass": ".*"', '"db_instance_root_pass": "************"')
    }
    jsonPlan.replaceAll('"db_instance_root_pass": ".*"', '"db_instance_root_pass": "************"')
    return jsonPlan
}

List<String> findSecrets(String jsonPlanFilePath, String tfPlanString = null, def currentLevel = null) {
    List<String> secrets = []
    def planJson
    if (currentLevel != null) {
        planJson = currentLevel
    } else if (tfPlanString != null && tfPlanString != '') {
        planJson = readJSON(text: tfPlanString.toString())['planned_values']['root_module']
    } else {
        planJson = readJSON(file: jsonPlanFilePath)['planned_values']['root_module']
    }
    if (planJson.containsKey('child_modules')) {
        planJson['child_modules'].each { module ->
            secrets.addAll(findSecrets(jsonPlanFilePath, null, module))
        }
    }
    if (!planJson.containsKey('resources') && !planJson.containsKey('child_modules')) {
        throw new RuntimeException('Modules that don\'t have any resources or modules doesn\'t make sense...')
    }
    secrets.addAll(
        planJson.resources
            .findAll({ resource -> resource.type == 'google_secret_manager_secret_version' })
            .collect({ resource -> resource.values['secret_data'] }) as Collection<? extends String>
    )

    return secrets
}

///**
// * Pulls encrypted TfVars files and encryption keys for the specified environment and writes to the output directory specified.
// * @param environment
// * @param outputDir
// * @return -- boolean value corresponding to if the requisite secrets exist
// */
//private boolean pullEncryptedMaterials(final String environment, final GcpProject gcpProject, final String bucketName = 'bkt-tfstate-ft') {
//    final GCloud gCloud = (GCloud) ServiceLoader.load(binding, GCloud.class)
//    final String tfVarsSecretName = "encrypted-tf-vars-${environment}"
//    boolean exists = gCloud.cloudStorageObjectExists(bucketName, "tfvars/${tfVarsSecretName}")
//    exists = exists && gCloud.doesSecretExist("$tfVarsSecretName-private-key", gcpProject.id)
//    keysExists = gCloud.doesSecretExist("$tfVarsSecretName-symmetric-key", gcpProject.id)
//    //TODO enhancement check for expiry and rotate keys if expired
//    if (!keysExists) {
//        //if the encryption keys don't exist then run the job that creates them
//        throw new RuntimeException('Encryption Keys could not be found. Exiting!')
//    }
//    String privateKey = gCloud.getSecretValue("$tfVarsSecretName-private-key", gcpProject.id)
//    writeFile(file: 'privateKey.pem', text: privateKey)
//    String symmetricKey = gCloud.getSecretValue("$tfVarsSecretName-symmetric-key", gcpProject.id)
//    String symmetricKeyPassword = gCloud.getSecretValue("$tfVarsSecretName-symmetric-password", gcpProject.id)
//    String salt = gCloud.getSecretValue("$tfVarsSecretName-symmetric-salt", gcpProject.id)
//    String iv = gCloud.getSecretValue("$tfVarsSecretName-symmetric-iv", gcpProject.id)
//    writeFile(file: 'symmetric.key.enc.b64', text: symmetricKey)
//    writeFile(file: 'symmetric-key-password', text: symmetricKeyPassword)
//    writeFile(file: 'salt', text: salt)
//    writeFile(file: 'iv', text: iv)
//    if (exists) {
//        String tfVarsString = gCloud.getCloudStorageObject(bucketName, "tfvars/${tfVarsSecretName}")
//        writeFile(file: 'terraform.enc.txt', text: tfVarsString)
//    } else {
//        throw new RuntimeException('Failed to locate encrypted tf vars')
//    }
//    return keysExists
//}

//
//private void decryptTfVars(String environment) {
//    sh 'cat symmetric.key.enc.b64 | base64 --decode > symmetric.key.enc'
//    sh 'openssl rsautl -decrypt -inkey privateKey.pem -in symmetric.key.enc -out symmetric.key'
//    sh 'cat terraform.enc.txt | base64 --decode > terraform.enc'
//    sh 'set +x; openssl enc -aes-256-cbc -d -iter 1000000 -K "$(cat symmetric.key)" -in terraform.enc --pass file:symmetric-key-password -out encrypted.tfvars -iv "$(cat iv)" -S "$(cat salt)"'
//}

//void pullAndDecryptTfVars(String environment, GcpProject gcpProject, String outputDirectory) {
//    if (gcpProject.isNewVersion) {
//        println 'New keys are not yet setup, skipping'
//        return
//    }
//    dir(outputDirectory) {
//        pullEncryptedMaterials(environment, gcpProject)
//        decryptTfVars(environment)
//    }
//}

void generateAndArchiveChangeset(String environment, String pathToPlanJson = '') {
    String jsonFilePath = (pathToPlanJson == '') ? "envs/${environment}/${environment}.json" : pathToPlanJson
    writeFile(file: jsonFilePath, text: scrubPlan(jsonFilePath, null))
    sh "./gradlew visualize -Penvironment=${environment}"
    publishHTML(target: [allowMissing         : false,
                         alwaysLinkToLastBuild: true,
                         keepAll              : true,
                         reportDir            : jsonFilePath.split("/").findAll { !it.contains('.json') }.join("/") + '/terraform-visual-report',
                         reportFiles          : "**/index.html",
                         reportName           : "Terraform ${environment.capitalize()} Env Visual Inspection",
                         reportTitles         : 'Terraform Inspection'])
}
