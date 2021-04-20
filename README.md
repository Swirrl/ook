# OOK (ONS trade search)

## Set-up

We're downloading RDF using drafter client. Although we're only using the public endpoint, you still need to prove AUTH0 credentials. You can configure an `AUTH0_SECRET` environmental variable with a dummy value if you like. You could also set it to the secret key for the [ook application](https://manage.auth0.com/dashboard/eu/swirrl-staging/applications/br25ZFYNX0wHK3z7FIql2mK91z8ZZcC8). You can store this locally in an encrypted file:

```bash
echo VALUE_OF_THE_SECRET | gpg -e -r YOUR_PGP_ID > resources/secrets/AUTH0_SECRET.gpg
```

You can use this pattern and the `ook.concerns.integrant/secret` reader to encrypt other secrets.

## Running the project

### Elasticsearch

You can use docker-compose to provide and manage elasticsearch.

The `bin/cider` and `bin/test` scripts provide a demonstration.

You can bring up a test index on port 9201 like this:

```bash
docker-compose up -d elasticsearch-test
```

Or one for dev on port 9200 like this:

```bash
docker-compose up -d elasticsearch-development
```

You can bring the services down like:

```bash
docker-compose down
```

You might also like to use the docker-compose `start` and `stop` commands. To see what's running use `docker-compose ps`.

### Server

Start a repl with the dev alias (using e.g. `bin/repl` or however you normally do) and run:

```clojure
(dev)
(go)
```

Visit `localhost:3000` in your browser (or whatever port you set if you overwrite it in `env/dev/resources/local.edn`).

### Clojurescript

If you don't have a recent version of the Yarn package manager installed, get it [here](https://classic.yarnpkg.com/en/docs/install/#mac-stable).

From the root directory of the project install JS dependencies:

```bash
yarn install
```

Then compile the cljs to JS and watch for changes:
```bash
yarn  watch
```

or, if you also want the tests:
```bash
yarn watch-all
```

With the shadow-cljs watcher running, cljs tests are run and watched at `localhost:8021`.

## Fixtures

You can load data from a drafter server into your elasticsearch development index from the shell as follows:

```bash
clojure -X:dev:ook.etl/fixtures
```

You can check that the indicies have some documents loaded with:

```bash
curl -X GET "localhost:9200/_cat/indices?v=true"
```

Alternatively you can create an integrant profile with the `:ook.index/data` component which will populate the database when the system is started. Use `:ook.etl/target-datasets` to scope the data to a set of `pmdcat:Dataset` URIs. See e.g. [resources/fixture-data.edn](resources/fixture-data.edn).

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

To load the data, ssh into the box and do:

```
cd /opt/ook
export AUTH0_SECRET=XXX
java -cp "ook.jar:lib/*" -Xmx3g clojure.main -m ook.index
```

It takes a couple of hours so you'll likely want to run this with gnu-screen/ tmux/ NOHUP so a drop in the connection doesn't kill the pipeline run.

## License

Copyright © 2021 Swirrl

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
