package com.zevrant.services.services

import com.zevrant.services.pojo.PodStatus

import java.nio.charset.StandardCharsets

int getDeployTimeout(int replicas) {
    int timeout = replicas * 360
    return timeout
}

String getPodName(String label, String environment, PodStatus status = PodStatus.NONE) {
    String withStatus = (status != PodStatus.NONE)? "| grep ${status.statusName}" : ''
    sh "kubectl -n $environment get pods --no-headers=true -l $label $withStatus > pods"
    String pod = readFile(file: 'pods').trim()
    println pod
    if(pod.split('\\n').length - 1 > 1) {
        throw new RuntimeException('Error: multiple pods returned, use getPods instead to return a list of matching pods')
    }
    List<String> pods = pod.split('\\n')
    if(pods.size() > 0) {
        return pods[pods.size() - 1].split('\\h')[0]
    } else {
        return null
    }
}

List<String> getPodNames(String label, String environment, PodStatus status = PodStatus.NONE) {
    String withStatus = (status != PodStatus.NONE)? "| grep ${status.statusName}" : ''
    sh "kubectl -n $environment get pods --no-headers=true -l $label $withStatus > pods"
    String pods = readFile(file: 'pods').trim()

    return pods.split('\\n')
            .collect { pod -> pod.split('\\h')[0] }
}

void execPod(String namespace, String podName, String command, List<String> args) {
    execPod(namespace, podName, command.concat(' ').concat(args.join(' ')))
}

void execPod(String namespace, String podName, String command) {
    sh "kubectl exec -n $namespace -i $podName -- $command"
}

void copyFileToPod(String namespace, String sourceFilePath, String podName, String destinationFilePath) {
    sh "kubectl cp -n $namespace '$sourceFilePath' '${podName}:${destinationFilePath}'"
}

void copyFileFromPod(String namespace, String sourceFilePath, String podName, String destinationFilePath) {
    sh "kubectl cp -n $namespace '${podName}:${sourceFilePath}' '$destinationFilePath'"
}

void waitForPodTermination(String podName, String namespace, int timeout = 60) {
    int i = 0
    try {
        while (i < timeout) {
            sh "kubectl get pods -n $namespace $podName > results 2>&1" //redirect stderr to stdout
            i += 5
            sleep(5)
        }
    } catch (Exception ex) {
        String results = readFile(file: 'results')
        println results
        if(!results.contains("Error from server (NotFound): pods \"${podName}\" not found")) {
            throw ex
        }
    }
}

String getSecretValue(String secretName, String valueName, String namespace) {
    sh "kubectl get secret -o json ${secretName} -n ${namespace} > output.json"
    def output = readJSON(file: 'output.json')
    return new String(Base64.getDecoder().decode(output.data[valueName] as String) as byte[], StandardCharsets.UTF_8);
}