(ns cellf.media
  (:require [cljs.core.async :refer [chan put!]]))

(defn get-media
  ([]
   (get-media {:video {:facingMode "user"} :audio false}))
  ([opts]
   (let [c (chan)]
     (if (.-mediaDevices js/navigator)
       (->
        (.getUserMedia (.-mediaDevices js/navigator) (clj->js opts))
        (.then  #(put! c {:status :success :data %}))
        (.catch #(put! c {:status :error :data :denied})))
       (put! c {:status :error :data :unsupported}))
     c)))
