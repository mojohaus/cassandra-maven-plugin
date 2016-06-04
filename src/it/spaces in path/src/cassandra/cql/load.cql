create keyspace TestKeyspaceWithSpace with replication = {'class':'SimpleStrategy', 'replication_factor':1};

use TestKeyspaceWithSpace;
create table Test (key uuid PRIMARY KEY);