--
-- (c) 2018-2019 Cloudera, Inc. All rights reserved.
--
--  This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
--  Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
--  to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
--  properly licensed third party, you do not have any rights to this code.
--
--  If this code is provided to you under the terms of the AGPLv3:
--   (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
--   (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
--       LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
--   (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
--       FROM OR RELATED TO THE CODE; AND
--   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
--       DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
--       TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
--       UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
--

insert into agent_manifest (
  id,
  content,
  created,
  updated
  )
  VALUES (
    'agent-manifest-1',
    'SHOULD BE JSON',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    'agent-manifest-2',
    'SHOULD BE JSON',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    'agent-manifest-3',
    'SHOULD BE JSON',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
);

insert into agent_class (
  id,
  description,
  created,
  updated
  )
  VALUES (
    'Class A',
    'A test agent class',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    'Class B',
    'A second test agent class',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
);

insert into agent_class_manifest
    (agent_class_id, agent_manifest_id)
  VALUES
    ('Class A', 'agent-manifest-1'),
    ('Class A', 'agent-manifest-2'),
    ('Class B', 'agent-manifest-3');

insert into agent (
  id,
  agent_name,
  first_seen,
  last_seen,
  agent_class,
  agent_manifest_id,
  agent_status,
  created,
  updated
  )
  VALUES (
    'agent-1',
    'Agent One',
    parsedatetime('2018-04-11 12:52:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:53:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    'Class A',
    'agent-manifest-1',
    'SHOULD BE JSON',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:54:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
);

insert into flow (
  id,
  registry_url,
  registry_bucket_id,
  registry_flow_id,
  registry_flow_version,
  flow_content,
  flow_content_format,
  created,
  updated
  )
  values (
    '1',
    'http://localhost:18080',
    'Bucket1',
    'Flow1',
    1,
    'SHOULD BE YAML',
    'YAML_V2_TYPE',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
);

insert into flow (
  id,
  registry_url,
  registry_bucket_id,
  registry_flow_id,
  registry_flow_version,
  flow_content,
  flow_content_format,
  created,
  updated
  )
  values (
    '2',
    'http://localhost:18080',
    'Bucket1',
    'Flow1',
    2,
    'SHOULD BE YAML',
    'YAML_V2_TYPE',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
);

insert into flow_mapping(
    agent_class,
    flow_id,
    created,
    updated
  )
  values (
    'Class A',
    '1',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  );


insert into operation (
    id,
    operation_type,
    operand,
    target_agent_id,
    state,
    created_by,
    created,
    updated
  )
  values (
    'operation-1',
    'CLEAR',
    'connection',
    'agent-1',
    'READY',
    'admin',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    'operation-2',
    'CLEAR',
    'repository',
    'agent-1',
    'NEW',
    'admin',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  );

insert into
  operation_arg
    (operation_id, arg_key, arg_value)
  values
    ('operation-1', 'name', 'connection-1'),
    ('operation-2', 'name', 'flowFileRepository');

insert into
  operation_dependency (operation_id, dependency_id)
  values ('operation-2', 'operation-1');

insert into heartbeat (
    id,
    device_id,
    agent_manifest_id,
    agent_class,
    agent_id,
    flow_id,
    content,
    created
  )
  values (
    'heartbeat-1',
    'device-1',
    'agent-manifest-1',
    'Class A',
    'agent-1',
    'flow-1',
    'SHOULD BE JSON',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    'heartbeat-2',
    'device-1',
    'agent-manifest-2',
    'Class B',
    'agent-2',
    'flow-2',
    'SHOULD BE JSON',
    parsedatetime('2018-04-11 12:52:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    'heartbeat-3',
    'device-2',
    'agent-manifest-1',
    'Class A',
    'agent-1',
    'flow-2',
    'SHOULD BE JSON',
    parsedatetime('2018-04-11 12:53:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    'heartbeat-4',
    null,
    null,
    null,
    null,
    null,
    'SHOULD BE JSON',
    parsedatetime('2018-04-11 12:54:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    'heartbeat-5',
    null,
    null,
    null,
    null,
    null,
    'SHOULD BE JSON',
    parsedatetime('2018-04-11 12:55:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    'heartbeat-6',
    null,
    null,
    null,
    null,
    null,
    'SHOULD BE JSON',
    parsedatetime('2018-04-11 12:56:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  );

insert into device (
    id,
    device_name,
    first_seen,
    last_seen,
    machine_arch,
    physical_mem,
    v_cores,
    network_id,
    hostname,
    ip_address,
    created,
    updated
  )
  values (
    'device-1',
    'Device 1',
    parsedatetime('2018-04-11 12:52:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:53:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    'x86_64',
    2000000000,
    4,
    '00:11:22:33:44:55',
    'device01.domain',
    '10.0.0.1',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:54:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    'device-2',
    'Device 2',
    parsedatetime('2018-04-11 12:52:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:53:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    'ARM',
    1000000000,
    2,
    '00:11:22:33:44:56',
    'device02.domain',
    'ABCD:ABCD:ABCD:ABCD:ABCD:ABCD:192.168.158.190',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:54:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  );

insert into event (
    id,
    severity,
    event_type,
    message,
    source_type,
    source_id,
    agent_class,
    created,
    updated
  )
  values (
    'event-1',
    'DEBUG',
    'Heartbeat',
    'This is a debug event.',
    'Agent',
    'agent-1',
    'Class A',
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    'event-2',
    'INFO',
    'Flow Update',
    'This is an info event.',
    'Agent',
    'agent-1',
    'Class A',
    parsedatetime('2018-04-11 12:52:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:52:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
    (
    'event-3',
    'WARN',
    'Agent Status',
    'This is a warn event.',
    'Agent',
    'agent-2',
    'Class B',
    parsedatetime('2018-04-11 12:53:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:53:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    'event-4',
    'ERROR',
    'C2 Server Status',
    'This is an error event.',
    'C2Server',
    'localhost:8080',
    null,
    parsedatetime('2018-04-11 12:54:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-04-11 12:54:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  );

insert into
  event_tag
    (event_id, tag, tag_value)
  values
    ('event-1', 'foo', '10'),
    ('event-2', 'foo', '20'),
    ('event-2', 'bar', '100'),
    ('event-3', 'foo', '30'),
    ('event-3', 'bar', '200'),
    ('event-3', 'baz', '');

