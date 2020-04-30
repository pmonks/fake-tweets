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
    (s/join "-"
            (map s/lower-case
                 (s/split k #"(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")))))

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

(defn clean-tweet
  "Cleans one tweet (a string), by unescaping HTML entities, removing URLs, correcting some common abbreviations, and replacing or removing a variety of punctuation and special characters."
  [tweet]
  (s/trim
    (replace-all tweet
                 [["&amp;"                               "&"]      ; Unescape HTML entities
                  ["&lt;"                                "<"]      ;           "
                  ["&gt;"                                ">"]      ;           "
                  ["&nbsp;"                              " "]      ;           "
                  ["&quot;"                              "\""]     ;           "
                  ["&apos;"                              "'"]      ;           "
                  ["&mdash;"                             "-"]      ;           "
                  [#"https?\S+"                          ""]       ; Remove URLs
                  [#"[‘’`]"                              "'"]      ; Normalise single quote characters
                  [#"[“”]"                               "\""]     ; Normalise double quote characters
                  [#"[—–―-]"                             "-"]      ; Normalise hyphens
                  [#"(\!|\?|:|,|/)"                      " $1 "]   ; Place whitespace around certain characters.  Note: adding '\p{So}|' to the regex would also separate out emojis etc.
                  [#"\&+"                                " & "]    ;           "
                  [#"\.+"                                " . "]    ;           "
                  [#"[;~\|\*\(\)\[\]\{\}\<\>\\\"™…•]+"   " "]      ; Remove various other characters
                  [#"\s\s+"                              " "]])))  ; Finally, collapse repeated whitespace created by the replacements above

(defn load-tweets
  "Loads and parses a tweet archive (something that can be read via clojure.java.io/reader) in JSON format."
  [readable]
  (filter not-blank?
    (map clean-tweet
         (filter #(and (not (s/starts-with? % "RT"))         ; Remove retweets and direct tweets to others
                       (not (s/starts-with? % "rt"))
                       (not (s/starts-with? % "\""))
                       (not (s/starts-with? % "@")))
                 (map #(s/trim (:text %))
                      (ch/parse-stream (io/reader readable)
                                       clojurise-json-key))))))

(defn tweet-words
  "Returns a sequence of all words, in order, in all tweets."
  [tweets]
  (let [tweets-str (s/join "\n" tweets)]
    (map s/trim (filter not-blank? (s/split tweets-str #"(\s|\p{javaSpaceChar})+")))))   ; Greedily split on whitespace, including Unicode whitespace


(defn vocabulary
  "Returns the vocabulary of the given tweets - the unique set of words, sorted."
  [tweets]
  (sort (distinct (tweet-words tweets))))

(defn markov-chain
  "Construct a markov chain of the given degree, for the given words."
  [tweets degree]
  (mc/collate (tweet-words tweets) degree))

(defn substitutions
  [s]
  (s/trim
    (replace-all (str " " s " ")
                 [
                  ; Literal substitutions
                  [#"(?i) o \. k \. "    " O.K. "]
                  [#"(?i) u . s . a . "  " U.S.A. "]
                  [#"(?i) u . s . "      " U.S. "]
                  [#"(?i) u . k . "      " U.K. "]
                  [#"(?i) u k "          " UK "]
                  [#"(?i) n . a . "      " N.A. "]
                  [#"(?i) d . c . "      " D.C. "]
                  [#"(?i) l . a . "      " L.A. "]
                  [#"(?i) w . h . "      " W.H. "]
                  [#"(?i) v . p . "      " V.P. "]
                  [#"(?i) p . m . "      " P.M. "]
                  [#"(?i) a . m . "      " A.M. "]
                  [#"(?i) p . s . "      " P.S. "]
                  [#"(?i) i . d . "      " I.D. "]
                  [#"(?i) i . g . "      " I.G. "]
                  [#"(?i) a . c . "      " A.C. "]
                  ; Number & time formatting
                  [#"(\d+)\s*,\s+(\d+)"  "$1,$2"]
                  [#"(\d+)\s*,\s+(\d+)"  "$1,$2"]  ; TODO: figure out how to not do this twice...
                  [#"(\d+)\s*\.\s+(\d+)" "$1.$2"]
                  [#"(\d+)\s*:\s+(\d+)"  "$1:$2"]
                  ; Punctuation
                  [#"\s+([\.,:!?])+"     "$1"]   ; Collapse whitespace before . , : ! ?
                  [#"\s*([/])+\s*"       "$1"]   ; Collapse whitespace on either side of /
                  [".."                  "."]    ; Collapse duplicate (but not triplicate or more!) .
                  [#",+"                 ","]    ; Collapse all sequences of ,
                  [#"[\.!?] - "          " - "]  ; Replace . ! ? before - with only -
                  [#"([!?]+)\."          "$1"]   ; Replace ! ? before . with only ! or ?
                  [#"-[\s-]*-"           "-"]    ; Replace repeated - (with or without whitespace in between) with a single -
                 ])))

(defn fake-tweet
  "Generates a fake tweet from the given markov chain, containing approximately the given number of 'words'."
  [markov-chain num-words-in-tweet]
  (substitutions (s/join " " (take num-words-in-tweet (mc/generate markov-chain)))))

