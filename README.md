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

or, if you also want the devcards:
```bash
yarn watch-all
```

With the shadow-cljs watcher running, devcards are available at `localhost:8000`.

## Testing

Run the tests with the alias:

```bash
clojure -A:test
```

## License

Copyright © 2021 Swirrl

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
