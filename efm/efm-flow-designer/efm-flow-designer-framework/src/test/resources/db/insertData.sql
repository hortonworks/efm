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

INSERT INTO FD_FLOW (
  id,
  agent_class,
  root_process_group_id,
  created,
  updated
  )
  VALUES (
    '1',
    'Class 1',
    'root-group-1',
    parsedatetime('2018-07-26 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-07-26 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    '2',
    'Class 2',
    'root-group-2',
    parsedatetime('2018-07-26 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-07-26 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  );

INSERT INTO FD_FLOW_EVENT (
    id,
    flow_id,
    flow_revision,
    event_type,
    event_description,
    flow_content,
    flow_content_format,
    component_id,
    user_identity,
    nifi_registry_url,
    nifi_registry_bucket_id,
    nifi_registry_flow_id,
    nifi_registry_flow_version,
    created,
    updated
  )
  VALUES (
    '1',
    '1',
    1,
    'COMPONENT_ADDED',
    'Created root process group',
    '{ "identifier" : "root-group-1", "name" : "root" }',
    'JACKSON_JSON_V1',
    'root-group-1',
    'system',
    'UNPUBLISHED',
    'UNPUBLISHED',
    'UNPUBLISHED',
    0,
    parsedatetime('2018-07-26 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-07-26 12:51:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    '2',
    '2',
    2,
    'COMPONENT_ADDED',
    'Created root process group',
    '{ "identifier" : "root-group-2", "name" : "root" }',
    'JACKSON_JSON_V1',
    'root-group-2',
    'system',
    'UNPUBLISHED',
    'UNPUBLISHED',
    'UNPUBLISHED',
    0,
    parsedatetime('2018-07-26 12:52:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-07-26 12:52:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    '3',
    '2',
    1,
    'COMPONENT_ADDED',
    'Created root process group',
    '{ "identifier" : "root-group-2", "name" : "root" }',
    'JACKSON_JSON_V1',
    'root-group-2',
    'system',
    'UNPUBLISHED',
    'UNPUBLISHED',
    'UNPUBLISHED',
    0,
    parsedatetime('2018-07-26 12:53:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-07-26 12:53:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  ),
  (
    '4',
    '2',
    3,
    'FLOW_PUBLISHED',
    'Created root process group',
    '{ "identifier" : "root-group-2", "name" : "root" }',
    'JACKSON_JSON_V1',
    'root-group-2',
    'system',
    'UNPUBLISHED',
    'UNPUBLISHED',
    'UNPUBLISHED',
    0,
    parsedatetime('2018-07-26 12:54:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z'),
    parsedatetime('2018-07-26 12:54:00.000 UTC', 'yyyy-MM-dd HH:mm:ss.SSS z')
  )
  ;