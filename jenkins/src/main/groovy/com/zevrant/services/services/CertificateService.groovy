package com.zevrant.services.services

import com.cloudbees.groovy.cps.NonCPS

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

class CertificateService extends Service {

    CertificateService(pipelineContext) {
        super(pipelineContext)
    }

    @NonCPS
    private static boolean isCertExpired(String beforeDate, String afterDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern('MMM d HH:mm:ss yyyy z')
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC)
        return now.isBefore(ZonedDateTime.parse(beforeDate.replace('  ', ' '), formatter)) || now.isAfter(ZonedDateTime.parse(afterDate.replace('  ', ' '), formatter).minusHours(1))
    }

    static String getMicroserviceUrl(String serviceName, String environment) {
        return "${serviceName}.${environment}.svc.cluster.local"
    }

    boolean isCertificateValid(String serviceUrl, int port = 443) {
        pipelineContext.timeout(time: 45, unit: 'SECONDS') {
            String certDates = getCertData(serviceUrl, port)
            String[] lines = certDates.split('\n')
            String beforeDate = lines[0].split('=')[1]
            String afterDate = lines[1].split('=')[1]
            return !isCertExpired(beforeDate, afterDate)
        }
    }

    private String getCertData(String serviceUrl, int port = 443) {
        String certDates = ''
        for( int i = 0; i < 5; i ++) {
            pipelineContext.sh "echo QUIT | openssl s_client -connect ${serviceUrl}:$port -servername ${serviceUrl} -verify 1 2>/dev/null | openssl x509 -noout -dates | tee certDates"

            certDates = pipelineContext.readFile(file: 'certDates')
            println(certDates)

            if(!certDates.toLowerCase().contains('Unable to load certificate'.toLowerCase())) {
                return certDates;
            }
        }
        throw new RuntimeException("Failed to get certificate for $serviceUrl")
    }

    @NonCPS
    String cleanCert(String cert) {
        StringReader stringReader = new StringReader(cert)
        BufferedReader bufferedReader = new BufferedReader(stringReader)
        boolean include = false
        StringWriter stringWriter = new StringWriter()
        BufferedWriter writer = new BufferedWriter(stringWriter)
        bufferedReader.readLines().findAll({ line ->
            if (line.contains('-----BEGIN CERTIFICATE-----')) {
                include = true
            } else if (line.contains('-----END CERTIFICATE-----')) {
                include = false
            }
            return include
        }).each { line ->
            writer.writeLine(line)
        }
        writer.flush()
        writer.close()
        return stringWriter.toString()
    }

    @NonCPS
    ZonedDateTime getEndTime(String serviceUrl, int port = 443) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern('MMM d HH:mm:ss yyyy z')
        String certDates = getCertData(serviceUrl, port)
        String[] lines = certDates.split('\n')
        String afterDate = lines[1].split('=')[1]
        ZonedDateTime.parse(afterDate.replace('  ', ' '), formatter)
    }

    @NonCPS
    String addSan(String opensslConf, String splitOn, String san) {
        StringWriter confWriter = new StringWriter()
        BufferedWriter writer = new BufferedWriter(confWriter)
        opensslConf.lines()
                .forEachOrdered({ line ->
                    if (line.contains(splitOn)) {
                        writer.writeLine('subjectAltName = @alt_names')
                        writer.writeLine('[alt_names]')
                        writer.writeLine("DNS.1 = ${san}")
                    }
                    writer.writeLine(line)
                })
        writer.flush()
        writer.close()
        return confWriter.toString()
    }
}