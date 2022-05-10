# http-proxy

Steps:

1) Certificate creation:
   - Setup CA and Server certificate
     - cd cert
     - openssl req -x509 -config openssl-ca.cnf -newkey rsa:4096 -sha256 -nodes -out cacert.pem -outform PEM # creates cacert.pem for CA operations and cackey.pem, which is CA's private key
     - openssl req -config openssl-server.cnf -newkey rsa:2048 -sha256 -nodes -out servercert.csr -outform PEM # creates servercert.csr and servercert.pem, which is your private key
     - touch index.txt
     - echo '01' > serial.txt
     - openssl ca -config openssl-ca.cnf -policy signing_policy -extensions subordinate_signing_req -out servercert.pem -infiles servercert.csr # CA command to sign the request. CA signs the request with Basic Constraints set to CA: True. This is need for proxy server to perform MITM
   - Create PKCS12 create store using the following command
     - openssl pkcs12 -export -inkey serverkey.pem -in servercert.pem -name aws-proxy -out aws-proxy.p12 
   - Concatenate servercert.pem and cacert.pem to aws-proxy.pem. This is useful for using in curl
     - cat servercert.pem cacert.pem > aws-proxy.pem
2) mvn clean install
3) java -jar target/httpproxy-1.0-SNAPSHOT-shaded.jar 
4) curl https://wttr.in?Herndon?0 --proxy localhost:9090 --cacert cert/aws-proxy.pem




References:
1) https://stackoverflow.com/questions/21297139/how-do-you-sign-a-certificate-signing-request-with-your-certification-authority