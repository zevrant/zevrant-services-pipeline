package com.zevrant.services.pojo

class GitRepo {

    public final String repoName
    public final String org
    public final String hostName
    public final String sshHostName
    public final String credentialsId

    public GitRepo(String hostName = 'gitea.zevrant-services.internal', String sshHostname = 'git@gitea.zevrant-services.internal:30121',
                   String org = 'zevrant-services', String repoName, String credentialsId = 'zevrant-services-jenkins') {
        this.repoName = repoName
        this.org = org
        this.hostName = hostName
        this.sshHostName = (sshHostname != null && sshHostName != '')? sshHostname : hostName
        this.credentialsId = credentialsId
    }

    String getSshUri() {
        return "ssh://${sshHostName}/${org}/${repoName}.git"
    }

    String getHttpsUri() {
        return "https://${hostName}/${org}/${repoName}.git"
    }

    String getRepoName() {
        return repoName
    }

    String getOrg() {
        return org
    }

    String getHostName() {
        return hostName
    }

    String getSshHostName() {
        return sshHostName
    }

    String getSshCredentialsId() {
        return credentialsId
    }
}
