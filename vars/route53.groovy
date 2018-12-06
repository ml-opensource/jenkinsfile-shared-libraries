def call() {
	sh 'aws route53 change-resource-record-sets --hosted-zone-id Z1R8UBAEXAMPLE --change-batch file://C:\awscli\route53\change-resource-record-sets.json'	
}

def create() {
	
}

def delete() {
	
}

/***

{
  "Comment": "optional comment about the changes in this change batch request",
  "Changes": [
    {
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "jenkinsfile.culinistas.fuzzhq.com",
        "Type": "A",
        "ResourceRecords": [
          {
            "Value": "d3n5l77pyeune7.cloudfront.net"
          }
        ]
      }
    }
  ]
}

***/