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

(defn clean-tweet
  "Cleans one tweet (a string), by unescaping HTML entities, removing URLs, and replacing or removing a variety of punctuation and special characters."
  [tweet]
  (s/trim
    (replace-all (str " " tweet " ")
                 [["&amp;"                               "&"]         ; Unescape HTML entities
                  ["&lt;"                                "<"]         ;           "
                  ["&gt;"                                ">"]         ;           "
                  ["&nbsp;"                              " "]         ;           "
                  ["&quot;"                              "\""]        ;           "
                  ["&apos;"                              "'"]         ;           "
                  ["&mdash;"                             "-"]         ;           "
                  [#"https?\S+"                          ""]          ; Remove URLs
                  [#"[‘’`]"                              "'"]         ; Normalise single quote characters
                  [#"[“”]"                               "\""]        ; Normalise double quote characters
                  [#"[-‐‑‒–—―⸺⸻]"                    "-"]         ; Normalise Unicode hyphens
                  [#"-+"                                 "-"]         ; De-dupe hyphens
                  [#"(\s)-([a-zA-Z@])"                   "$1- $2"]    ; Spread out "orphaned" hyphens
                  [#"(\w)-(\s)"                          "$1 -$2"]    ;           "
                  [#"(\!|\?|:|;|,|/)"                    " $1 "]      ; Place whitespace around certain characters.  Note: adding '\p{So}|' to the regex would also separate out emojis etc.
                  [#"\&+"                                " & "]       ;           "
                  [#"\.+"                                " . "]       ;           "
                  [#"[~\|\*\(\)\[\]\{\}\<\>\\\"™…•]+"    " "]         ; Remove various other characters
                  [#"\s\s+"                              " "]])))     ; Finally, collapse repeated whitespace created by the replacements above

(defn load-tweets
  "Loads and parses a tweet archive (something that can be read via clojure.java.io/reader) in JSON format."
  [readable]
  (filter not-blank?
    (map clean-tweet
         (filter #(and (not (s/starts-with? % "RT"))         ; Remove retweets and direct tweets to others
                       (not (s/starts-with? % "rt"))
                       (not (s/starts-with? % "\""))
                       (not (s/starts-with? % "@")))
                 (map #(s/trim (if-let [text (:text %)]
                                 text
                                 (:full-text %)))
                      (ch/parse-stream (io/reader readable)
                                       clojurise-json-key))))))

(defn words
  "The sequence of all words, in order, in all tweets."
  [tweets]
  (let [tweets-str (s/join " \n " tweets)]
    (map s/trim (filter not-blank? (s/split tweets-str #"(\s|\p{javaSpaceChar})+")))))   ; Greedily split on whitespace, including Unicode whitespace


(defn vocabulary
  "The vocabulary of the given words - the unique set of words, sorted."
  [words]
  (sort (distinct words)))

(defn markov-chain
  "Construct a markov chain of the given degree, for the given words."
  [words degree]
  (mc/collate words degree))

(defn capitalise-word
  "Capitalises a single word, by capitalising the first character and leaving all other characters unchanged."
  [word]
  (str (s/capitalize (first word)) (s/join (rest word))))

(defn capitalise-sentences
  "Capitalises a set of sentences, by calling capitalise-word on the first word in each sentence.  NOTE: NOT YET FULLY IMPLEMENTED!"
  [words]
  (cons (capitalise-word (first words))   ; Capitalise first word in tweet
        (rest words)))                    ;####TODO: run through (rest words) and capitalise every word after a "."

(defn apply-substitutions
  [s]
  (s/trim
    (replace-all (str " " s " ")
                 [
                  ; Literal substitutions
                  [#"(?i) o \. k \. "       " O.K. "]
                  [#"(?i) u \. s \. a \. "  " U.S.A. "]
                  [#"(?i) u \. s \. "       " U.S. "]
                  [#"(?i) u \. k \. "       " U.K. "]
                  [#"(?i) u k "             " UK "]
                  [#"(?i) n \. a \. "       " N.A. "]
                  [#"(?i) d \. c \. "       " D.C. "]
                  [#"(?i) l \. a \. "       " L.A. "]
                  [#"(?i) w \. h \. "       " W.H. "]
                  [#"(?i) v \. p \. "       " V.P. "]
                  [#"(?i) p \. m \. "       " P.M. "]
                  [#"(?i) a \. m \. "       " A.M. "]
                  [#"(?i) p \. s \. "       " P.S. "]
                  [#"(?i) i \. d \. "       " I.D. "]
                  [#"(?i) i \. g \. "       " I.G. "]
                  [#"(?i) a \. c \. "       " A.C. "]
                  ; Number & time formatting
                  [#"(\d+)\s*,\s+(\d+)"     "$1,$2"]
                  [#"(\d+)\s*,\s+(\d+)"     "$1,$2"]  ; TODO: figure out how to not have to do this twice...
                  [#"(\d+)\s*\.\s+(\d+)"    "$1.$2"]
                  [#"(\d+)\s*:\s+(\d+)"     "$1:$2"]
                  ; Punctuation
                  [#"\s+([\.,:;!?])+"       "$1"]   ; Collapse whitespace before . , : ; ! ?
                  [#"\s*([/])+\s*"          "$1"]   ; Collapse whitespace on either side of /
                  [".."                     "."]    ; Collapse duplicate (but not triplicate or more!) .
                  [#",+"                    ","]    ; Collapse all sequences of ,
                  [#"[\.!?] - "             " - "]  ; Replace . ! ? before - with only -
                  [#"([!?]+)\."             "$1"]   ; Replace ! ? before . with only ! or ?
                 ])))

(defn fake-tweet
  "Generates a fake tweet from the given markov chain, containing approximately the given number of 'words'."
  [markov-chain num-words-in-tweet]
  (s/trim
    (apply-substitutions
      (s/join " "
        (capitalise-sentences
          (take num-words-in-tweet
            (mc/generate markov-chain)))))))