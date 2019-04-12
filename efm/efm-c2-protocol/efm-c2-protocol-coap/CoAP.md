<!--
 ~ (c) 2018-2019 Cloudera, Inc. All rights reserved.  
 ~  
 ~  This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the  
 ~  Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized  
 ~  to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and  
 ~  properly licensed third party, you do not have any rights to this code.  
 ~  
 ~  If this code is provided to you under the terms of the AGPLv3:  
 ~   (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;  
 ~   (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT  
 ~       LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;  
 ~   (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING  
 ~       FROM OR RELATED TO THE CODE; AND  
 ~   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY  
 ~       DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED  
 ~       TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR  
 ~       UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.  
-->  
# CoAP ReadMe
CoAP is the Constrained Application Protocol[ \[1\]](https://tools.ietf.org/html/rfc7252). It defines a simple protocol that can provide smaller payloads. In the case of C2, we use the Datagram sockets ( UDP ), which provides us a confirmable heartbeat and acknowledgement payload. Since registration isn't well defined by the C2 interfaces, heartbeats that aren't recognized prompt the agent to submit a subsequent HTTP request. Further, updates to the flow should use this route as well.  Security will be provided via DTLS[\[2\]](https://tools.ietf.org/html/rfc7925).  This is a work in progress. 
## Configuration
To enable CoAP as an available protocol you must use the following options.

|Option  | Description Value| Required |
|--|--|--|
| efm.coap.server.port | CoAP port | Y |
| efm.coap.server.host | Hostname of the interface for CoAP connectivity* | N
| efm.coap.server.threads | Number of receiving threads. Default is 1 | N

* Note that efm.coap.server.host will default to localhost.

## Payload headers
All CoAP payloads begin with two bytes for the version, and one byte for the operation type. This will apply for heartbeats and operations. 
### Heartbeat
Hearbeats will always begin with the payload header then  the device information identifier, and the AgentInfo identifier. Note that strings are written as a size placeholder of two bytes, followed by the non-null terminated UTF-8 encoded string.  Component names ( as UTF-8 strings) and status ( as a single byte ) will follow the header. Queue sizes ( which consist of 4 64-bit network encoded values ) will come next, after which, the versionedFlowSnapshotURI will be written as a pair of strings for the bucket and flow ID. Note that the same tuple of size and UTF-8 encoded string will be used for the versionedFlowSnapshotURI. 
#### Heartbeat Responses
Heartbeat responses will contain the payload header following an operation ID string, operand string, and an unsigned 16 bit integer that provides the size of key and value strings contained in a listing. These key and value strings will provide operational arguments to the agents. 
<!-- todo: add a table-->
### Acknowledgements
Acknowledgements will always begin with the payload header, after which a string containing the operation ID will be sent. The Operation status ( a single byte providing the state ) will end the acknowledgment.
Values defined by the operation status are as follows

|Byte value  | Operation Status |
|--|--|
| 0 | Fully Applied |
| 1 | Partially Applied |
| 2 | Operation not understood |
| 3 | Not Applied |

[1] https://tools.ietf.org/html/rfc7252
[2] https://tools.ietf.org/html/rfc7925