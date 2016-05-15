# SocketCamera
It's an app to make your phone become an IP Camera!

# Breifly Introduce
## support methods
1. HTTP Server Push
2. TCP Socket send JPEG data 

## will support methods
1. HLS
2. RTMP
3. ...

# Detailed Introduce
## HTTP Server Push

the key to realize HTTP Server Push is: `Content-Type: multipart/x-mixed-replace`

**Notice:** not all browsers support this feature.and pay attention to the '\r\n'
```
#An typical HTTP Server PUSH Response
HTTP/1.1 200 OK
Content-Type: multipart/x-mixed-replace; boundary=myboundary

--myboundary
Content-Type: image/jpeg
Content-length: 12345

[image 1 encoded jpeg data]


--myboundary
Content-Type: image/jpeg
Content-length: 45678

[image 2 encoded jpeg data]

...
```
## screen shot
wait me!

[HttpWebResponse with MJPEG and multipart/x-mixed-replace; boundary=--myboundary](http://stackoverflow.com/questions/2060953/httpwebresponse-with-mjpeg-and-multipart-x-mixed-replace-boundary-myboundary)

## TCP Socket send JPEG data

when the client connect to the server ,then the server continous to send the JPEG data to client via the socket,and the client 
decode every frame JPEG data into Bitmap ,and draw it to the screen .
