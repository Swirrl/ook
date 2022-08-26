# Ook Objekt Katalog (Structural Search) <img src="resources/ook-logo.png" align="right" height="256" />

> if someone ever reported that there was an orang-utan in the Library, the wizards would probably go and ask the Librarian if he'd seen it.
>
> Terry Pratchett - Night Watch

OOK is a structural search engine for data cubes.

Typically search engines allow users to find data by matching against terms in the dataset-metadata. For example a query like "Balance of Payments" would be needed to match that publication's title or summary.

OOK goes deeper, indexing the reference data terms that describe and identify each numerical observation within a datacube. This let's users find data with queries like "imports of cars from Germany". Users can search without first needing to know how data was packaged and published.

OOK also understands the structure of data cubes so users can cross-filter for different facets, asking things like "what's the trade-off between geographic precision and recency?".

OOK is powered by linked-data written to match the [PublishMyData Application Profile](https://swirrl.github.io/PMD-AP/index.html). We extract data from a triplestore using SPARQL then transform this into compacted and framed JSON-LD before loading it into Elasticsearch for querying. The ETL process and front-end are written in Clojure.

## Running OOK

### Elasticsearch Database

OOK uses [Elasticsearch](https://www.elastic.co/elasticsearch/) as it's database.

We provide a docker-compose file for running elasticsearch in your local development environment.

The `bin/cider` and `bin/test` scripts provide a demonstration.

You can bring up a test index on port 9201 like this:

```bash
docker-compose up -d elasticsearch-test
```

Or one for dev on port 9200 like this:

```bash
docker-compose up -d elasticsearch-development
```

You can bring the services down with:

```bash
docker-compose down
```

You might also like to use the docker-compose `start` and `stop` commands. To see what's running use `docker-compose ps`.

### Clojurescript Front-end

The front-end is written in [Clojurescript](https://clojurescript.org/guides/quick-start). You'll need to compile this to JavaScript.

Using a recent version of the [Yarn](https://yarnpkg.com/getting-started) package manager, you can install the JavaScript dependencies:

```bash
yarn install
```

Then compile the CLJS to JS:

```bash
yarn compile
```

If you want to develop the CLJS you can have yarn watch for changes and recompile as necessary:

```bash
yarn watch
```
or, if you also want the tests:
```bash
yarn watch-all
```

With the shadow-cljs watcher running, cljs tests are run and reloaded at `localhost:8021`.

### Clojure Application Server

The application server is written in [Clojure](https://clojure.org/guides/getting_started). You can run it locally by starting a clojure REPL with the dev alias using e.g. `bin/repl` (or `bin/cider` if you're using [emacs/cider](https://cider.mx/)). Within the REPL, you can load and start the server with:

```clojure
(dev)
(go)
```

Visit `localhost:3000` in your browser (or whatever port you set if you overwrite it in `env/dev/resources/local.edn`).

## Loading Data

You'll need to run the ETL pipeline to populate your Elasticsearch database.

We provide configurations for extracting data from the [Integrated Data Service](https://beta.gss-data.org.uk/).

For a small set of fixtures you can use:

```bash
clojure -X:dev:ook.etl/fixtures
```

Or to load all trade datasets you can use:

```bash
clojure -X:dev:ook.etl/trade
```

You can check that the indicies have some documents loaded with:

```bash
curl -X GET "localhost:9200/_cat/indices?v=true"
```

Alternatively you can create an integrant profile with the `:ook.etl/load`
component which will populate the database when the system is started. Use
`:ook.etl/target-datasets` to scope the data to a vector of `pmdcat:Dataset`
URIs (e.g. [resources/fixture/data.edn](resources/fixture/data.edn)) or provide
a SPARQL query to set the scope (e.g.
[resources/trade/data.edn](resources/trade/data.end)).

## Testing

Run the tests with the alias:

```bash
clojure -M:dev:test
```

Clojurescript tests can be built and viewed in dev as described above. To build/run them from the command line you need to have Chrome installed and run:
```bash
yarn build-ci
node_modules/karma/bin/karma start --single-run
```
Or, if you have the [karma cli](http://karma-runner.github.io/latest/index.html) installed globally, just
```bash
karma start --single-run
```

This runs the cljs tests in a way that can be reported programatically for CI.

## Deployment

See the [deployment readme](./deploy/README.md).

## Drafter Authentication

We're downloading RDF using drafter client.

Since we're only using the public endpoint by default, the AUTH0 credentials are being ignored. You can configure an `AUTH0_SECRET` environmental variable with a dummy value if you like.

If you need to use a draft endpoint then you can specify AUTH0 credentials using e.g. a secret key for the ook application (e.g. on [cogs staging](https://manage.auth0.com/dashboard/eu/swirrl-staging/applications/br25ZFYNX0wHK3z7FIql2mK91z8ZZcC8) or [idp beta](https://manage.auth0.com/dashboard/eu/swirrl-ons-prod/applications/OS2GgkrjYyb7EXdawNfk6HViXznpf7Dh/settings)). You can store this locally in an encrypted file:

```bash
echo VALUE_OF_THE_SECRET | gpg -e -r YOUR_PGP_ID > env/dev/resources/secrets/AUTH0_SECRET.gpg
```

You can use this pattern and the `ook.concerns.integrant/secret` reader to encrypt other secrets.


## License

Copyright © 2021 Swirrl

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
