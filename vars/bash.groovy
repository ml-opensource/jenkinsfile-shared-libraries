def call(String command) {
    sh "#!/bin/bash\n source ~/.bash_profile \n  ${command}"
}