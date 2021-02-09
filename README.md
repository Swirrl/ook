# OOK (ONS trade search)

## Running the project

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

## License

Copyright © 2021 Swirrl

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
