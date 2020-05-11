
# fake-tweets

A small library that builds a Markov chain from a tweet archive in JSON format, then spits out any number of statistically similar, though fake, tweets.

## Trying it Out

Download a JSON tweet archive, for example via [Twitter's export function](https://help.twitter.com/en/managing-your-account/how-to-download-your-twitter-archive), using something like [this script](https://gist.github.com/manuchandel/bc8a6ca4b1527b7594945e5091013905), or from [this archive of hot, toxic garbage](http://www.trumptwitterarchive.com/archive).

The JSON must be an array of objects, with each object containing either a `text` or `full_text` key, whose value is the full text of that tweet.  No other keys are used.

e.g.

```JSON
[
  {
    "source": "Twitter for iPhone",
    "text": "Who can figure out the true meaning of \"covfefe\" ???  Enjoy!",
    "created_at": "Wed May 31 10:09:22 +0000 2017",
    "retweet_count": 85555,
    "favorite_count": 209543,
    "is_retweet": false,
    "id_str": "869858333477523458"
  },
  {
    "source": "Twitter for iPhone",
    "text": "Despite the constant negative press covfefe",
    "created_at": "Wed May 31 04:06:25 +0000 2017",
    "retweet_count": 127507,
    "favorite_count": 162788,
    "is_retweet": false,
    "id_str": "869766994899468288"
  }
]
```

Clone this repo, start a REPL, then use the various fns in the `fake-tweets.core` ns:

```shell
$ git clone https://github.com/pmonks/fake-tweets.git
$ cd fake-tweets
$ clj -r
```

```clojure
(require '[clojure.java.io :as io])
(require '[fake-tweets.core :as ft] :reload-all)

(def json-tweet-archive (io/file "hot-toxic-garbage.json"))
(def tweets (ft/load-tweets json-tweet-archive))
(def markov-chain (ft/markov-chain tweets 3))   ; Degree 3 generally seems to give the best results

; Generate 100 "words" of fake hot, toxic garbage
(ft/fake-tweet markov-chain 100)

; How big is the vocabulary of the tweeter?  Note: punctuation, numbers, emojis, hashtags, @mentions etc. are all included in the count
(def vocabulary (ft/vocabulary tweets))
(count vocabulary)
```

## Contributor Information

[GitHub project](https://github.com/pmonks/fake-tweets)

[Bug Tracker](https://github.com/pmonks/fake-tweets/issues)

## License

Copyright © 2020 Peter Monks Some Rights Reserved

[![Creative Commons License](https://i.creativecommons.org/l/by-nc-sa/4.0/88x31.png)](http://creativecommons.org/licenses/by-nc-sa/4.0/)

This work is licensed under a [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License](http://creativecommons.org/licenses/by-nc-sa/4.0/).

SPDX-License-Identifier: [CC-BY-NC-SA-4.0](https://spdx.org/licenses/CC-BY-NC-SA-4.0.html)
