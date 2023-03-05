//package com.zevrant.services.services
//
//import com.google.auth.oauth2.OAuth2Credentials
//import com.google.auth.oauth2.ServiceAccountCredentials
//import com.zevrant.services.pojo.AppEdit
//import groovy.json.JsonSlurper
//
//import java.security.KeyStore
//import java.security.PrivateKey
//
//String baseUrl = "https://androidpublisher.googleapis.com"
//
//AppEdit createEdit(String packageName) {
//
//    def response = httpRequest(
//            url: "${}/androidpublisher/v3/applications/${packageName}/edits",
//    )
//
//    return (AppEdit) new JsonSlurper().parseText(response.content);
//}
//
//OAuth2Credentials createGoogleCredential() {
//    withCredentials([file(credentialsId: 'jenkins-gcp', variable: 'keystoreFile'), string(credentialsId: 'jenkins-gcp-password', variable: 'password')]) {
//
//        KeyStore keystore = KeyStore.getInstance("PKCS12");
//        keystore.load(new BufferedInputStream(new FileInputStream((String) keystoreFile)), ((String) password).toCharArray())
//
//        PrivateKey privateKey = (PrivateKey) keystore.getKey("privatekey", ((String) password).toCharArray())
//
//        ServiceAccountCredentials credential =
//                ServiceAccountCredentials.newBuilder()
//                        .setClientEmail('jenkins-service-account@pc-api-8638677702475896862-728.iam.gserviceaccount.com')
//                        .setPrivateKey(privateKey)
//                        .build()
//
//        sourceCredentials = (ServiceAccountCredentials) credential
//                .createScoped(Arrays.asList("https://www.googleapis.com/auth/iam"))
//
//        return credential.getAccessToken();
//    }
//}