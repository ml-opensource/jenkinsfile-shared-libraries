/**
 * Execute an AWS cli command <code>aws secretsmanager get-secret-value</code> 
 * through shell under the given AWS profile, region, and version of the secret.
 * <p>
 *     <a href="https://docs.aws.amazon.com/cli/latest/reference/secretsmanager/index.html">
 *         There are more details options and information regarding the 
 *         aws secretsmanager get-secret-value cli command.
 *     </a>
 * </p>
 *
 * @param aws_credentials is the aws credintails that are stored in jenkins
 * @param secretId is the aws id of the secret
 * @param aws_region is the aws region that the secret resides
 * @param version_stage is the secret version 
 * @return the stdout of the shell script
 */
def awsGetSecretString(String aws_credentials, String secret_id, String aws_region = 'us-east-1', String version_stage = 'AWSCURRENT') {
    awsCreds("${aws_credentials}") {
        sh(
            script: "aws secretsmanager get-secret-value \
            --secret-id ${secret_id} \
            --version-stage ${version_stage} \
            --region ${aws_region} \
            | jq '.SecretString' \
            | tr -d '\"' \
            | tr -d '\n'",
            returnStdout: true
        )
    }
}
