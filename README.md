# Competition System

Installation:
```
wget -qO- https://raw.githubusercontent.com/pppepito86/noi/master/install/provision.sh | bash -s dev
```
To start/stop workers add your aws cretentials

~/.aws/credentials should look like
```
[default]
aws_access_key_id = ${AWS_ACCESS_KEY_ID}
aws_secret_access_key = ${AWS_SECRET_ACCESS_KEY}
```
~/.aws/config should look like
```
[default]
region = ${REGION}
```
Example region is 'eu-central-1'.
