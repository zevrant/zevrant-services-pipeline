package com.zevrant.services.pojo

class GitRepo {

    public final String repoName
    public final String org
    public final String hostName
    public final String sshHostName

    public GitRepo(String hostName = 'gitea.zevrant-services.com', String sshHostname = 'git@ssh.gitea.zevrant-services.com:30121',
                   String org = 'zevrant-services', String repoName) {
        this.repoName = repoName
        this.org = org
        this.hostName = hostName
        this.sshHostName = (sshHostname != null && sshHostName != '')? sshHostname : hostName
    }

    public String getSshUri() {
        return "ssh://${sshHostName}/${org}/${repoName}.git"
    }

    public String getHttpsUri() {
        return "https://${hostName}/${org}/${repoName}/"
    }
}
