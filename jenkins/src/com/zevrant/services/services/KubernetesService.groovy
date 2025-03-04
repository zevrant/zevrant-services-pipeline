package com.zevrant.services.services

import com.zevrant.services.pojo.PodStatus

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

class KubernetesService extends Service {

    KubernetesService(Object pipelineContext) {
        super(pipelineContext)
    }

    int getDeployTimeout(int replicas) {
        int timeout = replicas * 360
        return timeout
    }

    String getPodName(String label, String environment, PodStatus status = PodStatus.NONE) {
        String withStatus = (status != PodStatus.NONE) ? "| grep ${status.statusName}" : ''
        pipelineContext.sh "kubectl -n $environment get pods --no-headers=true -l $label $withStatus > pods"
        String pod = pipelineContext.readFile(file: 'pods').trim()
        pipelineContext.println pod
        if (pod.split('\\n').length - 1 > 1) {
            throw new RuntimeException('Error: multiple pods returned, use getPods instead to return a list of matching pods')
        }
        List<String> pods = pod.split('\\n')
        if (pods.size() > 0) {
            return pods[pods.size() - 1].split('\\h')[0]
        } else {
            return null
        }
    }

    List<String> getPodNames(String label, String environment, PodStatus status = PodStatus.NONE) {
        String withStatus = (status != PodStatus.NONE) ? "| grep ${status.statusName}" : ''
        pipelineContext.sh "kubectl -n $environment get pods --no-headers=true -l $label $withStatus > pods"
        String pods = pipelineContext.readFile(file: 'pods').trim()

        return pods.split('\\n')
                .collect { pod -> pod.split('\\h')[0] }
    }

    void execPod(String namespace, String podName, String command, List<String> args) {
        execPod(namespace, podName, command.concat(' ').concat(args.join(' ')))
    }

    void execPod(String namespace, String podName, String command) {
        pipelineContext.sh "kubectl exec -n $namespace -i $podName -- $command"
    }

    void copyFileToPod(String namespace, String sourceFilePath, String podName, String destinationFilePath) {
        pipelineContext.sh "kubectl cp -n $namespace '$sourceFilePath' '${podName}:${destinationFilePath}'"
    }

    void copyFileFromPod(String namespace, String sourceFilePath, String podName, String destinationFilePath) {
        pipelineContext.sh "kubectl cp -n $namespace '${podName}:${sourceFilePath}' '$destinationFilePath'"
    }

    void waitForPodTermination(String podName, String namespace, int timeout = 60) {
        int i = 0
        try {
            while (i < timeout) {
                pipelineContext.sh "kubectl get pods -n $namespace $podName > results 2>&1" //redirect stderr to stdout
                i += 5
                sleep(5)
            }
        } catch (Exception ex) {
            String results = pipelineContext.readFile(file: 'results')
            pipelineContext.println results
            if (!results.contains("Error from server (NotFound): pods \"${podName}\" not found")) {
                throw ex
            }
        }
    }

    String getSecretValue(String secretName, String valueName, String namespace) {
        pipelineContext.sh "kubectl get secret -o json ${secretName} -n ${namespace} > output.json"
        def output = pipelineContext.readJSON(file: 'output.json')
        return new String(Base64.getDecoder().decode(output.data[valueName] as String) as byte[], StandardCharsets.UTF_8);
    }

    String getServiceIp() {
        pipelineContext.sh 'kubectl get services --all-namespaces --no-headers | awk \'{ print $4 }\' | grep 10 > serviceIps'
        List<String> serviceIps = pipelineContext.readFile(file: 'serviceIps').split("\n")
        String ipAddressPartial = "10.96.0."
        for (int i = 1; i <= 255; i++) {
            String ipAddress = ipAddressPartial + "$i"
            if (!serviceIps.contains(ipAddress)) {
                return ipAddress
            }
        }
        throw new RuntimeException("Failed to get available ip address within cidr block 10.96.0.0/24")
    }

    boolean requestCertificate(String certName, String environment, List<String> dnsNames) {
        String getCertCommand = "kubectl get certs -n ${environment} ${certName} --no-headers | awk '{print \$2}' | grep True"
        int status = pipelineContext.sh returnStatus: true, script: getCertCommand
        if (status == 0) {
            pipelineContext.println("Cert $certName already exists and is valid no action needed")
            return true
        }
        Map<String, Object> certificateRequest = [
                apiVersion: "cert-manager.io/v1",
                kind      : "Certificate",
                metadata  : [
                        name     : certName,
                        namespace: environment,
                ],
                spec      : [
                        secretName : "${certName}-tls",
                        issuerRef  : [
                                name: "acme-issuer"
                        ],
                        privateKey : [
                                algorithm     : "ECDSA",
                                size          : 256,
                                rotationPolicy: "Always"
                        ],
                        duration   : "24h",
                        renewBefore: "11h",
                        dnsNames   : dnsNames
                ]
        ]

        pipelineContext.writeYaml(file: 'certificateRequest.yml', data: certificateRequest)
        pipelineContext.println(pipelineContext.writeYaml(returnText: true, data: certificateRequest))
        pipelineContext.sh "kubectl apply -f certificateRequest.yml -n $environment"
        LocalDateTime end = LocalDateTime.now().plusMinutes(2)
        status = pipelineContext.sh returnStatus: true, script: getCertCommand
        while (status != 0 && LocalDateTime.now().isBefore(end)) {
            pipelineContext.sleep 1
            status = pipelineContext.sh returnStatus: true, script: getCertCommand
        }

        if (status != 0) {
            pipelineContext.sh "kubectl describe cert $certName"
            throw new RuntimeException("Failed to obtain certificate $certName for requested service")
        }
        return true
    }
}