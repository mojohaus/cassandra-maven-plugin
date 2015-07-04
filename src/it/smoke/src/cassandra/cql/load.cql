create keyspace TestKeyspace
    with strategy_class = 'SimpleStrategy'
    and strategy_options:replication_factor = 1;

use TestKeyspace;
create columnfamily Test (key uuid PRIMARY KEY) with comparator='UTF8Type';