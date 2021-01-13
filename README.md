[![Build Status](https://github.com/pmonks/fake-tweets/workflows/build/badge.svg)](https://github.com/pmonks/fake-tweets/actions?query=workflow%3Abuild)
[![Dependencies](https://github.com/pmonks/fake-tweets/workflows/dependencies/badge.svg)](https://github.com/pmonks/fake-tweets/actions?query=workflow%3Adependencies)
[![Open Issues](https://img.shields.io/github/issues/pmonks/fake-tweets.svg)](https://github.com/pmonks/fake-tweets/issues)
[![License](https://img.shields.io/github/license/pmonks/fake-tweets.svg)](https://github.com/pmonks/fake-tweets/blob/master/LICENSE)

# fake-tweets

A small library that builds a Markov chain from a tweet archive in JSON format, then spits out any number of statistically similar, though fake, tweets.

## Trying it Out

### Providing a JSON Archive

Download a JSON tweet archive, for example via [Twitter's export function](https://help.twitter.com/en/managing-your-account/how-to-download-your-twitter-archive), using something like [this script](https://gist.github.com/manuchandel/bc8a6ca4b1527b7594945e5091013905), or from [this archive of hot, toxic garbage](https://www.thetrumparchive.com/).

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

### Running the Code

Clone this repo, symlink your tweet archive, then start a REPL using the provided init script:

```shell
$ git clone https://github.com/pmonks/fake-tweets.git
$ cd fake-tweets
$ ln -s /path/to/your/tweet-archive.json hot-toxic-garbage.json
$ clj -i init.clj -r
```

This will load and analyse the given tweet archive, then generate one fake tweet up to 100 "words" in length.  To generate more:

```clojure
(ft/fake-tweet markov-chain)
```

In addition to the `markov-chain` var, the init script also initialises vars named `vocabulary`, `vocab-freq` and
`sorted-vocab-freq` which allow primitive analysis of the tweeter's vocabulary using standard Clojure functions:

```clojure
; How bigly is the tweeter's vocabulary?  Note: punctuation, numbers, emojis, hashtags, @mentions etc. are all included in the count
(count vocabulary)

; Does the tweeter excrete covfefe?
(not (empty? (filter #(s/includes? (s/lower-case %) "covfefe") vocabulary)))   ; Not idiomatic Clojure, but I don't agree with the rationale...

; How much?
(get vocab-freq "covfefe")

; What are the tweeter's favourite 100 words?
(take 100 sorted-vocab-freq)
```

## Contributor Information

[GitHub project](https://github.com/pmonks/fake-tweets)

[Bug Tracker](https://github.com/pmonks/fake-tweets/issues)

## License

Copyright © 2020 Peter Monks Some Rights Reserved

[![Creative Commons License](https://i.creativecommons.org/l/by-nc-sa/4.0/88x31.png)](http://creativecommons.org/licenses/by-nc-sa/4.0/)

This work is licensed under a [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License](http://creativecommons.org/licenses/by-nc-sa/4.0/).

SPDX-License-Identifier: [CC-BY-NC-SA-4.0](https://spdx.org/licenses/CC-BY-NC-SA-4.0.html)
