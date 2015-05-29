(ns ^:figwheel-always cellf.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [<! >! take! put! chan timeout]]
            [cellf.media :as media])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(def default-size 3)
(def capture-size 250)
(def tick-ms      150)
(def resize-ms    200)
(def source-url   "https://github.com/dmotz/cellf")

(defonce img-cache (atom []))

(defn sq [n]
  (* n n))

(defn t3d [x y]
  (str "translate3d(" x "%," y "%,0)"))

(defn promise-chan []
  (let [c (chan)]
    (take! c #(go-loop []
      (>! c %)
      (recur)))
    c))

(defn capture-move [{:keys [ctx vid-node canvas-node vid-w vid-h]} cells]
  (.drawImage ctx vid-node (/ (- vid-w vid-h) 2) 0 vid-h vid-h 0 0 capture-size capture-size)
  (let [img-data (.toDataURL canvas-node "image/jpeg" 1)
        img-el   (js/Image.)
        pc       (promise-chan)]
    (swap! img-cache conj pc)
    (doto img-el
      (aset "src" img-data)
      (aset "onload" (fn []
        (js-delete img-el "onload")
        (put! pc img-el))))
    {:cells cells :image img-data}))

(defn make-cell-list [size]
  (conj (vec (range (dec (sq size)))) :empty))

(defn make-cells [size win-state]
  (let [shuffled (zipmap (make-cell-list size) (shuffle (range (sq size))))]
    (if (= shuffled win-state) (recur size win-state) shuffled)))

(defn make-win-state [size]
  (zipmap (make-cell-list size) (range (sq size))))

(defn make-game
  ([app size]
    (make-game app size tick-ms))
  ([app size speed]
    (let [win-state (make-win-state size)
          cells     (make-cells size win-state)]
      {
        :moves     [(capture-move app cells)]
        :tick      0
        :tick-ms   speed
        :grid-size size
        :cells     cells
        :win-state win-state})))

(def get-cell-xy (juxt mod quot))

(defn get-cell-style [app i]
  (let [size  (:grid-size app)
        pct   (str (/ 100 size) \%)
        px    (/ (:grid-px app) size)
        [x y] (get-cell-xy i size)]
    #js {
      :transform  (t3d (* 100 x) (* 100 y))
      :width      pct
      :height     pct
      :lineHeight (str px "px")
      :fontSize   (/ px 10)}))


(defn get-bg-transform [app i]
  (if (= i :empty)
    nil
    (let [size  (:grid-size app)
          pct   (/ 100 size)
          [x y] (get-cell-xy i size)]
      #js {
        :height    (str (* size 100) \%)
        :transform (t3d
          (- (+ (/ (* x pct) (:vid-ratio app)) (:vid-offset app)))
          (- (* pct y)))})))


(defn adj? [app i]
  (let [{size :grid-size {emp :empty} :cells} app]
    (or
      (= i (- emp size))
      (and (= i (dec emp)) (pos? (mod emp size)))
      (and (= i (inc emp)) (pos? (mod (inc emp) size)))
      (= i (+ emp size)))))


(defn swap-cell [cells n]
  (let [current-idx (cells n)
        empty-idx   (:empty cells)]
    (into {} (map (fn [[k idx]]
      (if (= k :empty)
        [:empty current-idx]
        (if (= k n) [k empty-idx] [k idx]))) cells))))


(defn move! [{:keys [moves win-state] :as app} n]
  (let [new-layout (swap-cell (:cells app) n)]
    (om/update! app :cells new-layout)
    (om/update! app :moves (conj moves (capture-move app new-layout)))))


(defn new-game! [app size speed]
  (reset! img-cache [])
  (om/update! app (merge app (make-game app size speed))))


(defn cell [app [n idx]]
  (if (not= n :empty)
    (let [is-adj (adj? app idx)]
      (dom/div #js {
        :react-key n
        :className (str "cell" (when is-adj " adjacent"))
        :style     (get-cell-style app idx)
        :onClick   #(when is-adj (move! app n))}
          (dom/video #js {
            :src      (:stream app)
            :autoPlay "autoplay"
            :style    (get-bg-transform app n)})
          (dom/label nil (inc n))))))


(defn grid [{:keys [cells grid-px show-nums] :as app}]
  (om/component
    (apply dom/div #js {
      :className (str "grid" (when show-nums " show-nums"))
      :style     #js {:width grid-px :height grid-px}}

      (map (partial cell app) cells))))


(defn set-grid-size! [app size]
  {:pre [(integer? size) (> size 1) (< size 10)]}
  (new-game! app size (:tick-ms @app)))

(defn set-tick-ms! [app ms]
  (om/update! app :tick-ms ms))

(defn get-max-grid-px []
  (min (- js/innerWidth (* capture-size 1.2)) js/innerHeight))

(declare playback-ctx)

(defn paint-canvas! [{:keys [moves grid-size]} tick]
  (go
    (let [img (<! (@img-cache tick))
          s   (/ capture-size grid-size)]
      (.fillRect playback-ctx 0 0 capture-size capture-size)
      (doseq [[idx pos] (:cells (moves tick))]
        (if-not (= idx :empty)
          (let [[x1 y1] (get-cell-xy idx grid-size)
                [x2 y2] (get-cell-xy pos grid-size)]
            (.drawImage playback-ctx img (* x1 s) (* y1 s) s s (* x2 s) (* y2 s) s s))))
      (.drawImage playback-ctx img 0 capture-size))))

(defonce app-state (atom {}))

(defn raf-step! [c]
  (js/requestAnimationFrame #(put! c true (partial raf-step! c))))

(defonce raf-chan
  (let [c (chan)]
    (raf-step! c)
    c))

(defn start! []
  (swap! app-state merge (make-game @app-state default-size))
  (defonce tick-loop
    (go-loop [tick 0]
      (<! (timeout (:tick-ms @app-state)))
      (<! raf-chan)
      (let [app        @app-state
            moves      (:moves app)
            move-count (count moves)
            tick       (if (>= tick move-count) 0 tick)]
        (swap! app-state assoc :tick tick)
        (<! (paint-canvas! app tick))
        (if (or (zero? move-count) (= tick (dec move-count)))
          (recur 0)
          (recur (inc tick)))))))


(defn get-camera! []
  (take! (media/get-media) (fn [{:keys [status data]}]
    (if (= status :success)
      (let [video  (.createElement js/document "video")
            canvas (.createElement js/document "canvas")]
        (doto canvas
          (aset "width"  capture-size)
          (aset "height" capture-size))
        (doto video
          (aset "autoplay" "autoplay")
          (aset "onplaying" (fn []
            (let [vw (.-videoWidth video)
                  vh (.-videoHeight video)]
              (js-delete video "onplaying")
              (swap! app-state merge {
                :stream      data
                :grid-px     (get-max-grid-px)
                :vid-node    video
                :vid-w       vw
                :vid-h       vh
                :vid-ratio   (/ vw vh)
                :vid-offset  (* 100 (/ (max 1 (.abs js/Math (- vw vh))) (max 1 (* vw 2))))
                :canvas-node canvas
                :ctx         (.getContext canvas "2d")
                :show-about? true})
              (js/setTimeout start! 500))))
          (aset "src" data)))
      (swap! app-state assoc :media-error data)))))


(defn make-gif [app ms]
  (om/update! app :gif-building? true)
  (let [gif    (js/GIF. #js {:workerScript "/js/gif.worker.js" :quality 1})
        canvas (.-canvas playback-ctx)]
    (go
      (dotimes [idx (count (:moves app))]
        (<! (paint-canvas! app idx))
        (.addFrame gif canvas #js {:delay ms :copy true}))
      (doto gif
        (.on "finished" (fn [data]
          (om/update! app :result-gif (.createObjectURL js/URL data))
          (om/update! app :gif-building? false)))
        (.render)))))


(defn modal [{:keys [stream media-error show-about? result-gif cells win-state grid-size tick-ms gif-building?] :as app}]
  (let [winner?    (and cells (= cells win-state))
        no-stream? (not stream)]
    (dom/div #js {
      :className (str "modal"
        (when (or no-stream? media-error show-about? result-gif winner?) " active"))}
      (cond
        media-error
          (dom/div nil
            (dom/h1 nil "!!!")
            (if (= media-error :denied)
              "Sorry, Cellf doesn't work without camera access."
              (str
                "Sorry, it looks like your device or browser doesn't support camera access. "
                "Try Cellf using Chrome or Firefox on a device that supports WebRTC."))
            (dom/button #js {:onClick get-camera!} "try again")
            (dom/button #js {:onClick #(js/open source-url)} "view cellf source"))

        no-stream?
          (dom/div nil
            (dom/h1 nil "Hi")
            (dom/p nil
              (str
                "Cellf is an interactive experiment that reflects the player and their "
                "surroundings as they play. When you click OK, Cellf will ask for camera access."))
            (dom/p nil
              (str
                "There's no server or multiplayer component to this: "
                "your image stays on your device."))
            (dom/button #js {:onClick get-camera!} "✔ ok"))

        result-gif
          (dom/div nil
            (dom/h1 nil "Your Cellf")
            (dom/img #js {:src result-gif})
            "Save this gif and share your Cellf with the world."
            (dom/button #js {:onClick #(om/update! app :result-gif nil)} "✔ done"))

        winner?
          (dom/div nil
            (dom/h1 nil "You win!")
            "For more of a challenge, drag the slider to create a bigger grid."
            (apply dom/button
              (if gif-building?
                [#js {:className "wait"} "hold on"]
                [#js {:onClick #(make-gif app tick-ms)} "make gif replay"]))
            (dom/button #js {:onClick #(set-grid-size! app grid-size)} "new game"))

        show-about?
          (dom/div nil
            (dom/h1 nil "How to play")
            (dom/p nil
              (str
                "Simply click a cell next to the empty cell to move it. "
                "When you shuffle them into the correct order, you win."))
            (dom/p nil
              (str
                "You can also export a replay of your moves to an animated gif by "
                "clicking the 'make gif' button."))

            (dom/h1 nil "About Cellf")
            (dom/p nil
              "Cellf was created by Dan Motzenbecker and is "
              (dom/a #js {:href source-url} "open source")
              " on Github. "
              "For more experiments like this, visit "
              (dom/a #js {:href "http://oxism.com"} "oxism.com")
              \.)

            (dom/button #js {:onClick #(om/update! app :show-about? false)} "✔ got it"))))))


(om/root
  (fn [{:keys [stream moves tick tick-ms grid-size show-nums gif-building?] :as app} owner]
    (reify
      om/IDidMount
      (did-mount [_]
        (def playback-ctx
          (let [ctx (-> (om/get-node owner "playback") (.getContext "2d"))]
            (aset ctx "fillStyle" "#fff")
            ctx))

        (defonce resize-loop
          (let [resize-chan (chan)]
            (.addEventListener js/window "resize" #(put! resize-chan true))
            (go-loop [open true]
              (when open (<! resize-chan))
              (let [throttle (timeout resize-ms)]
                (if (= throttle (last (alts! [throttle resize-chan])))
                  (do
                    (swap! app-state assoc :grid-px (get-max-grid-px))
                    (recur true))
                  (recur false)))))))

      om/IRender
      (render [_]
        (dom/div nil
          (modal app)
          (dom/div #js {
            :id        "sidebar"
            :className (when-not stream "hidden")
            :style     #js {:width capture-size}}

            (dom/h1 nil "cellf")
            (dom/h2 nil "find yourself")
            (dom/canvas #js {
              :ref    "playback"
              :width  capture-size
              :height (* capture-size 2)})

            (when stream
              (dom/div nil
                (dom/label
                  #js {:className "move-count"}
                  (str (inc tick) \/ (count moves)))

                (dom/label #js {:htmlFor "show-nums"} "show numbers?")
                (dom/input #js {
                  :id       "show-nums"
                  :type     "checkbox"
                  :checked  show-nums
                  :onChange #(om/update! app :show-nums (not show-nums))})

                (dom/label nil
                  (str "grid size (" grid-size \× grid-size ")")
                  (dom/em nil "(starts new game)"))
                (dom/input #js {
                  :type     "range"
                  :value    grid-size
                  :min      "2"
                  :max      "9"
                  :step     "1"
                  :onChange #(set-grid-size! app (js/parseInt (.. % -target -value)))})

                (dom/label nil "playback speed")
                (dom/input #js {
                  :type     "range"
                  :value    (- tick-ms)
                  :min      "-1000"
                  :max      "-30"
                  :step     "10"
                  :onChange #(set-tick-ms! app (- (js/parseInt (.. % -target -value))))})

                (apply dom/button
                  (if gif-building?
                    [#js {:className "wait"} "hold on"]
                    [#js {:onClick #(make-gif app tick-ms)} "make gif"]))

                (dom/button #js {:onClick #(om/update! app :show-about? true)} "help"))))

          (when stream (om/build grid app))))))
  app-state
  {:target (.getElementById js/document "app")})
