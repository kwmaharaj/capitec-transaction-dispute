# Transaction Dispute Backend
## Docker
- ```docker compose down -v```
- ```docker compose up -d --build OR docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build```
- ```docker compose logs -f backend```

## Tests
- ```./gradlew clean test```

# Find blocking calls - used to double-check we didn't accidently introduce usage of block*
```grep -RIn --exclude-dir=build --exclude-dir=.gradle "\.block\(\)|\.blockOptional\(\)|toFuture\(\)" backend/src/main/java/za/co/capitec/transactiondispute/*```

# Insert and upsert
Using Spring Data R2DBC for reactive persistence. For ingestion, we use custom Postgres upsert SQL (INSERT … ON CONFLICT DO UPDATE) 
via DatabaseClient because Spring Data repositories don’t provide a clean bulk upsert abstraction. This way it does remain reactive, efficient, 
and atomic at the database level, ie no issue with conflicts on update or code to handle that, or having to check if entity exists then save logic, or try insert if fail then update.
Regardless sql is still generated and executed. Not clean can be thought of as not having a single method in the api that does bulk work for example.
While batch is available, its not the best. 

# Sql injection
No concatenation - using named param so data is not treated as executable sql.
