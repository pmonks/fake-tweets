
# fake-tweets

A small library that builds a Markov chain from a tweet archive in JSON format, then spits out any number of statistically similar, though fake, tweets.

## Trying it Out

Download a JSON tweet archive, for example via the [Twitter export function](https://help.twitter.com/en/managing-your-account/how-to-download-your-twitter-archive), or something like [this script](https://gist.github.com/manuchandel/bc8a6ca4b1527b7594945e5091013905), or [this archive of hot, toxic garbage](http://www.trumptwitterarchive.com/archive).

Clone this repo, then:

```shell
clj -r
```

```clojure
(require '[clojure.string :as s])
(require '[clojure.java.io :as io])
(require '[fake-tweets.core :as ft] :reload-all)
(def json-tweet-archive (io/file "hot-toxic-garbage.json"))
(def tweets (ft/load-tweets json-tweet-archive))
(def vocabulary (ft/vocabulary tweets))
(def markov-chain (ft/markov-chain tweets 3))   ; Degree 3 generally seems to give the best results
(ft/fake-tweet markov-chain 50)
```

## Contributor Information

[GitHub project](https://github.com/pmonks/fake-trump-tweets)

[Bug Tracker](https://github.com/pmonks/fake-trump-tweets/issues)

## License

Copyright © 2020 Peter Monks Some Rights Reserved

[![Creative Commons License](https://i.creativecommons.org/l/by-nc-sa/4.0/88x31.png)](http://creativecommons.org/licenses/by-nc-sa/4.0/)

This work is licensed under a [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License](http://creativecommons.org/licenses/by-nc-sa/4.0/).

SPDX-License-Identifier: [CC-BY-NC-SA-4.0](https://spdx.org/licenses/CC-BY-NC-SA-4.0.html)
