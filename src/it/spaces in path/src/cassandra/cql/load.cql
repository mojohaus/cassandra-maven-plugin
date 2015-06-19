create keyspace TestKeyspaceWithSpace
    with strategy_class = 'SimpleStrategy'
    and strategy_options:replication_factor = 1;

use TestKeyspaceWithSpace;
create columnfamily Test (key uuid PRIMARY KEY) with comparator='UTF8Type';