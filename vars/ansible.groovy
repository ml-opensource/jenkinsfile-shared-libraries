def run(String playbook, String extra_vars, String aws_credentials) {
    
    withCredentials([sshUserPrivateKey(credentialsId: 'github-jenkins-key', keyFileVariable: 'GITHUB_KEY')]) {
        sh "cp ${env.GITHUB_KEY} ~/.ssh/id_rsa"
    }

    git.clone("git@github.com:fuzz-productions/fuzz-ansible.git")

    awsCreds("${aws_credentials}") {
        slack.qbash "cd fuzz-ansible \
        && ansible-playbook \
        -i inventory/hosts ${playbook} \
        --extra-vars env=${extra_vars} \
        -vv"
    }

}
