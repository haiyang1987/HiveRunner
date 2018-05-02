DROP TABLE IF EXISTS bar;

CREATE TABLE bar AS
SELECT a
from foo
where a = "GREEN";
