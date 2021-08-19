node {
    //sh "git clone  --mirror git@github.com:zevrant/zevrant-services-pipeline.git /var/lib/jenkins/cache/zevrant-services-pipeline"
    boolean usedReference = fileExists '.git/objects/info/alternates'

    if(!usedReference) {
        throw new RuntimeException("Didn't use reference repository")
    }
}