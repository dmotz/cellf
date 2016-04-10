(ns cellf.media
  (:require [cljs.core.async :refer [chan put!]]))

(def prefixes ["webkit" "moz" "ms"])

(defn find-native [obj fn-name]
  (let [cap-name (str (.toUpperCase (first fn-name)) (apply str (rest fn-name)))]
    (loop [names (cons fn-name (map #(str % cap-name) prefixes))]
      (when (seq names)
        (if-let [f (aget obj (first names))]
          (.bind f obj)
          (recur (rest names)))))))


(defn get-media
  ([]
   (get-media {:video true :audio false}))
  ([opts]
   (let [c (chan)]
     (if-let [native-gum (find-native js/navigator "getUserMedia")]
       (native-gum
         (clj->js opts)
         #(put! c {:status :success
                   :data   (.createObjectURL js/URL %)})
         #(put! c {:status :error :data :denied}))
       (put! c {:status :error :data :unsupported}))
     c)))
