import java.text.SimpleDateFormat

@Library('CommonUtils') _

double warningThreshold = 0.04 //.96 hours

pipeline {
    agent {
        kubernetes {
            inheritFrom 'kubernetes'
        }
    }

    stages {
        stage('Pull Data From Thanos') {
            steps {
                script {

                    String queryBody = "x509_cert_not_after - time()) / 86400) < bool $warningThreshold"
                    httpRequest(
                            method: 'GET',
                            url: 'http://thanos-query.monitoring.svc.cluster.local/api/v1/query',
                            consoleLogResponseBody: true,
                            requestBody: queryBody
                    )
                    long unixSeconds = 1372339860;
                    // convert seconds to milliseconds
                    Date date = new java.util.Date(unixSeconds * 1000L);
                    // the format of your date
                    SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
                    // give a timezone reference for formatting (see comment at the bottom)
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT-4"));
                    String formattedDate = sdf.format(date);
                    System.out.println(formattedDate);
                }
            }
        }
    }
}