# http-proxy

Steps:

1) Certificate creation:
   - Setup CA and Server certificate
     - cd cert
     - openssl req -x509 -config openssl-ca.cnf -newkey rsa:4096 -sha256 -nodes -out cacert.pem -outform PEM # creates cacert.pem for CA operations and cakey.pem, which is CA's private key
     - openssl req -config openssl-server.cnf -newkey rsa:2048 -sha256 -nodes -out servercert.csr -outform PEM # creates servercert.csr and serverkey.pem, which is your private key
     - touch index.txt
     - echo '01' > serial.txt
     - openssl ca -config openssl-ca.cnf -policy signing_policy -extensions subordinate_signing_req -out servercert.pem -infiles servercert.csr # CA command to sign the request. CA signs the request with Basic Constraints set to CA: True. This is need for proxy server to perform MITM
   - Create PKCS12 create store using the following command
     - openssl pkcs12 -export -inkey serverkey.pem -in servercert.pem -name aws-proxy -out aws-proxy.p12 
   - The code expects a pem file to be placed with file name = aws-proxy.pem (to change file name, set alias in properties). This could be a dummy file as well. Only the p12 file is used for cert operations. You could just copy the servercert as aws-proxy.pem
     - cat servercert.pem > aws-proxy.pem
2) mvn clean install
3) java -jar target/httpproxy-1.0-SNAPSHOT-shaded.jar 
4) curl https://wttr.in?Herndon?0 --proxy localhost:9090 --cacert cert/cacert.pem # Just the root cert i.e. CA cert is needed. On a request, the proxy responds with the newly minted certificate as well as proxy server's cert (even if the p12 file has CA cert, the proxy responds only with minted cert + proxy server's cert). You only need CA cert in curl's truststore




References:
1) https://stackoverflow.com/questions/21297139/how-do-you-sign-a-certificate-signing-request-with-your-certification-authority
