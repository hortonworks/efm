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

# Database Migration Scripts

The EFM server requires a relational database for persisting its application data.

Our intention is to support the following database vendors:
 - H2 embedded
 - MySQL 5.6, 5.7
 - PostgreSQL 9.6, later?

H2 is the default as it is an embedded database that runs in the same JVM as an EFM server instance without anything additional to install and run.

That said, MySQL or PostgreSQL is recommended for users running in a production environment. 
It is also required to use one of these external databases when running multiple load balanced instances in a HA deployment.

The database schema is automatically created and migrated to the latest version every time the EFM server starts.
This is managed by Flyway, a DB schema version management tool. 

Flyway loads the versioned schema scripts from the classpath (they are bundled with the app). 
The default location is `classpath:db/migration`. We have broken this into subdirectories.

As much as possible, we are trying to use a single set of vendor-agnostic migration scripts. These live in `classpath:db/migration/common`.
Vendor-specific sql scripts, when needed, can be placed in `classpath:db/migration/{vendor}`.

The default search location for migration scripts on startup is:
`classpath:db/migration/common,classpath:db/migration/{vendor}`

The vendor is determined based on the JDBC connection URL configured for the DataSource in efm.properties.

Any files found are applied in version order from 1 to N. The same version should never exist in common and a vendor folder; 
however, the same version can exist in multiple vendor folders as only one vendor folder should be loaded at a time.
In other words, if for the next release we are unable to migration using common SQL, we can place VN+1__Migration.sql scripts in each vendor folder.

If, in the future, we need to add support for a new DB vendor and the existing common SQL scripts do not work for that vendor, the Flyway search location can be overridden using:

`spring.flyway.locations=classpath:db/migration/{new-vendor}`

Thereby excluding the common SQL directory.

## Tips for generating common SQL

- Avoid reserved keywords for table and column names. [SQL Style Guide](https://www.sqlstyle.guide) has a list of these, as well as some other useful tips.
- [SQL Fiddle](http://sqlfiddle.com/) is a useful website for quickly checking sql against multiple vendors, though it is not perfect (there are some false negatives and positives)
    - One thing to note with SQLFiddle is that the providers set a max column size of 8000 bytes for any data type, so types like LONGTEXT do not work correctly. 
      They offer a Format SQL feature that will convert any provided SQL to fit these limitations.
- When testing, use the strict setting on the DB configuration so as to maintain compatibility with most real-world installs. 
- MySQL, when running in STRICT, NO_ZERO_DATE mode, does not allow a zero default for TIMESTAMP fields.
- MySQL is case-sensitive for table and column names.
- MySQL 5.6 does not support keys longer than 255 bytes by default. This is the total key length, so for a composite key you must sum all column lengths.
