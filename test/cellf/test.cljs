(ns cellf.test
  (:require [cljs.test :refer-macros [deftest is run-all-tests]]
            [cellf.core :refer [make-win-state solvable?]]))

(enable-console-print!)

(deftest win-states
  (is (= (make-win-state 2) {0 0 1 1 2 2 :empty 3}))
  (is (= (make-win-state 3) {0 0 1 1 2 2 3 3 4 4 5 5 6 6 7 7 :empty 8}))
  (is (= (make-win-state 4) {0 0 1 1 2 2 3 3 4 4 5 5 6 6 7 7 8 8 9 9 10 10 11 11 12 12 13 13 14 14 :empty 15}))
  (is (= (make-win-state 5) {0 0 1 1 2 2 3 3 4 4 5 5 6 6 7 7 8 8 9 9 10 10 11 11 12 12 13 13 14 14 :empty 24 15 15 16 16 17 17 18 18 19 19 20 20 21 21 22 22 23 23})))

(deftest solvability
  (is (false? (solvable? {0 1 1 0 2 2 :empty 3} 2)))
  (is (false? (solvable? {0 1 1 0 2 3 :empty 2} 2)))
  (is (false? (solvable? {0 2 1 3 2 0 :empty 1} 2)))
  (is (false? (solvable? {0 1 1 2 2 3 :empty 0} 2)))
  (is (false? (solvable? {0 0 1 2 2 3 :empty 1} 2)))
  (is (false? (solvable? {0 0 1 3 2 1 :empty 2} 2)))
  (is (false? (solvable? {0 2 1 1 2 0 :empty 3} 2)))
  (is (false? (solvable? {0 3 1 1 2 2 :empty 0} 2)))
  (is (false? (solvable? {0 3 1 1 2 2 :empty 0} 2)))
  (is (false? (solvable? {0 1 1 2 2 3 :empty 0} 2)))
  (is (true?  (solvable? {0 3 1 2 2 1 :empty 0} 2)))
  (is (true?  (solvable? {0 0 1 3 2 2 :empty 1} 2)))
  (is (true?  (solvable? {0 3 1 0 2 1 :empty 2} 2)))
  (is (true?  (solvable? {0 2 1 1 2 3 :empty 0} 2)))
  (is (true?  (solvable? {0 1 1 3 2 0 :empty 2} 2)))

  (is (false? (solvable? {0 2 1 4 2 8 3 5 4 1 5 6 6 3 7 7 :empty 0} 3)))
  (is (false? (solvable? {0 6 1 7 2 0 3 5 4 4 5 8 6 2 7 3 :empty 1} 3)))
  (is (false? (solvable? {0 2 1 3 2 1 3 4 4 7 5 0 6 8 7 6 :empty 5} 3)))
  (is (false? (solvable? {0 8 1 2 2 7 3 4 4 6 5 3 6 0 7 5 :empty 1} 3)))
  (is (false? (solvable? {0 7 1 4 2 2 3 8 4 1 5 3 6 6 7 5 :empty 0} 3)))
  (is (false? (solvable? {0 8 1 7 2 1 3 4 4 0 5 5 6 3 7 6 :empty 2} 3)))
  (is (true?  (solvable? {0 6 1 2 2 3 3 7 4 4 5 5 6 0 7 1 :empty 8} 3)))
  (is (true?  (solvable? {0 7 1 5 2 2 3 1 4 4 5 0 6 6 7 8 :empty 3} 3)))
  (is (true?  (solvable? {0 6 1 8 2 0 3 3 4 5 5 2 6 1 7 7 :empty 4} 3)))
  (is (true?  (solvable? {0 5 1 0 2 1 3 8 4 4 5 6 6 3 7 2 :empty 7} 3)))

  (is (false? (solvable? {0 2 1 4 2 8 3 11 4 0 5 1 6 3 7 7 8 15 9 9 10 13 11 6 12 12 13 10 14 14 :empty 5} 4)))
  (is (false? (solvable? {0 11 1 12 2 6 3 1 4 7 5 14 6 9 7 4 8 10 9 0 10 3 11 13 12 15 13 8 14 5 :empty 2} 4)))
  (is (false? (solvable? {0 8 1 4 2 9 3 12 4 1 5 7 6 5 7 0 8 13 9 6 10 2 11 10 12 14 13 3 14 15 :empty 11} 4)))
  (is (false? (solvable? {0 6 1 14 2 12 3 11 4 10 5 3 6 9 7 5 8 1 9 8 10 2 11 7 12 0 13 4 14 13 :empty 15} 4)))
  (is (false? (solvable? {0 11 1 9 2 8 3 7 4 5 5 12 6 15 7 4 8 2 9 3 10 0 11 13 12 14 13 6 14 1 :empty 10} 4)))
  (is (false? (solvable? {0 1 1 4 2 3 3 14 4 13 5 5 6 11 7 15 8 10 9 0 10 7 11 12 12 2 13 8 14 9 :empty 6} 4)))
  (is (true?  (solvable? {0 6 1 2 2 10 3 1 4 4 5 14 6 5 7 7 8 11 9 8 10 12 11 13 12 0 13 3 14 9 :empty 15} 4)))
  (is (true?  (solvable? {0 11 1 2 2 13 3 15 4 6 5 14 6 7 7 1 8 10 9 8 10 12 11 3 12 9 13 0 14 5 :empty 4} 4))))


(defn ^:export run []
  (run-all-tests #"cellf.*-test"))
