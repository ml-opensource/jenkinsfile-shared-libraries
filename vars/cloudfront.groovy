def invalidate(id) {
  invalidation = sh(
    script: "aws cloudfront create-invalidation --distribution-id ${id} --paths /index.html",
    returnStdout: true
  )

  return invalidation
}

def getDistribution(domainName) {
  distributionID = sh(
    script: "aws cloudfront list-distributions --output text --query 'DistributionList.Items[].{DomainName: DomainName, OriginDomainName: Origins.Items[0].DomainName, Id: Id}[?contains(OriginDomainName, ${domainName})] | [0].Id'",
    returnStdout: true
  )

  if (distributionID.equals("None")) {
    return "No Cloudfront Distribution found"
  }

  return distributionID
}
