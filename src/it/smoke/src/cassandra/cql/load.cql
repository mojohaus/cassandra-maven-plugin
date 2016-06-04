create keyspace TestKeyspace with replication = {'class':'SimpleStrategy', 'replication_factor':1};

use TestKeyspace;
create table Test (key uuid PRIMARY KEY);