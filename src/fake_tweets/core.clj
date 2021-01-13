;
; Copyright © 2020 Peter Monks Some Rights Reserved
;
; This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License.
; To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/ or send a
; letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
;

(ns fake-tweets.core
  (:require [clojure.string     :as s]
            [clojure.java.io    :as io]
            [markov-chains.core :as mc]
            [cheshire.core      :as ch]))

(def not-blank? (complement s/blank?))

(defn clojurise-json-key
  "Converts JSON string keys (e.g. \"fullName\") to Clojure keyword keys (e.g. :full-name)."
  [k]
  (keyword
    (s/replace
      (s/join "-"
              (map s/lower-case
                   (s/split k #"(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")))
      "_"
      "-")))

(defn replace-all
  "Takes a sequence of replacements, and applies all of them to the given string, in the order provided.  Each replacement in the sequence is a pair of values to be passed to clojure.string/replace (the 2nd and 3rd arguments)."
  [string replacements]
  (loop [s string
         f (first replacements)
         r (rest  replacements)]
    (if f
      (recur (s/replace s (first f) (second f))
             (first r)
             (rest  r))
      s)))

(defn tokenize
  "Convert the given tweets into tokens (~= words) suitable for collation into a Markov chain."
  [tweets]
  (filter not-blank?
    (s/split (replace-all (s/join " 🔚 " tweets)
                          [["&amp;"             "&"]         ; Unescape HTML entities
                           ["&quot;"            "\""]        ;           "
                           ["&lt;"              "<"]         ;           "
                           ["&gt;"              ">"]         ;           "
                           ["&nbsp;"            " "]         ;           "
                           ["&quot;"            "\""]        ;           "
                           ["&apos;"            "'"]         ;           "
                           ["&mdash;"           "-"]         ;           "
                           ["&#39;"             "'"]         ;           "
                           [#"https?\S+"        ""]          ; Remove URLs
                           ["’"                 "'"]         ; Single quotes
                           [#"[“”]"             "\""]        ; Double quotes
                           ["…"                 "..."]       ; Ellipses
                           [#"([!?:;,\"\*])"    " $1 "]      ; Place whitespace around certain punctuation
                           [#"\s&(\S)"          " & $1"]     ; Collapse whitespace around &
                           [#"(\S)&\s"          "$1 & "]     ;           "
                           [#"(\D)(\.+)\s"      "$1 $2 "]    ; Place whitespace after numbers
                           [#"\s-(\S)"          " - $1"]     ; Collapse whitespace around -
                          ])
             #"\s+")))

(defn load-tweets
  "Loads and parses a tweet archive (something that can be read via clojure.java.io/reader) in JSON format."
  [readable]
  (tokenize
       (filter #(and (not (s/starts-with? % "RT"))         ; Remove retweets and direct tweets to others
                     (not (s/starts-with? % "rt"))
                     (not (s/starts-with? % "\""))
                     (not (s/starts-with? % "@")))
               (map #(s/trim (if-let [text (:text %)]
                               text
                               (:full-text %)))
                    (ch/parse-stream (io/reader readable)
                                     clojurise-json-key)))))

(defn words-from-file
  [readable]
  (load-tweets readable))

(defn vocabulary
  "The vocabulary of the given words - the unique set of words, sorted."
  [words]
  (sort (distinct words)))

(defn markov-chain
  "Construct a markov chain of the given degree, for the given words."
  [words degree]
  (mc/collate words degree))

(defn markov-chain-from-file
  "Convenience fn that chains together all of the steps from JSON file to markov-chain."
  [readable degree]
  (markov-chain (words-from-file readable) degree))

(defn fake-tweet
  ([chain] (fake-tweet chain 100))
  ([chain max-words]
   (replace-all (s/join " "
                        (take max-words
                              (take-while (partial not= "🔚")
                                          (drop-while #(or (= "🔚" %) (re-matches #"(\p{Punct})+" %))   ; Drop leading title breaks and punctuation
                                                      (mc/generate chain)))))
                [[#"\s+([!?:;,\"…\*\.])" "$1"]])))  ; Collapse whitespace before punctuation
