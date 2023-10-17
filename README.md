# Java21 Virtual Thread + Apatch HttpCore echo service

Similar to the well-known echo service defined in RFC862, but works on HTTP/1.1.

Once a HTTP connection is established, any data received as request body is sent back as response body.

This works as full-duplex streaming thanks to HTTP/1.1 chunked encoding. However, note that not every HTTP/1.1 client can handle this kind of full-duplex HTTP streaming well.

"Classic" API of Apache HttpCore is built upon blocking I/O, so it can be a good guinea pig for experimenting with Java21 Virtual Thread.
Unfortunately, they don't provide an easy way to swap internal thread executors, so I had to modify a few of the classes in org.apache.hc.core5.http.impl.bootstrap package.

