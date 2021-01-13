(require '[clojure.string :as s])
(require '[clojure.java.io :as io])
(require '[fake-tweets.core :as ft] :reload-all)

(def tweets-file (io/file "hot-toxic-garbage.json"))

(def tweet-words (ft/words-from-file tweets-file))
(def markov-chain (ft/markov-chain tweet-words 3))   ; Degree 3 generally seems to give the best results

; Generate a fake tweet of up to 100 "words"
(println (ft/fake-tweet markov-chain 100))

; Simple vocabulary analysis
(def vocabulary (ft/vocabulary tweet-words))
(def vocab-freq (frequencies tweet-words))
(def sorted-vocab-freq (sort-by (comp - val) vocab-freq))

;(count vocabulary)
;(not (empty? (filter #(s/includes? (s/lower-case %) "covfefe") vocabulary)))
;(get vocab-freq "covfefe")
;(take 100 sorted-vocab-freq)

(println "\n\tTo generate more: (ft/fake-tweet markov-chain)")
