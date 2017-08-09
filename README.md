# njt-alerts-gtfs-rt
This module builds GTFS-RT alerts for NJ Transit

In order to run this module, you need to set the following enviromental variables
| Environmental Variable | Description|
|: ------------:|:-------------:|
|${AWS_ACCESS_KEY_ID}| AWS S3 access key| 
|${AWS_SECRET_ACCESS_KEY}| AWS S3 secret access key|
|${NJB_URL}| New Jersey Buses XML alert feed URL|
|${NJT_URL}| New Jersey Rail XML alert feed URL|
|${S3BUCKET}| Name of the S3 bucket with GTFS data|
|${S3DIR}| Name of the S3 directory with GTFS data|
