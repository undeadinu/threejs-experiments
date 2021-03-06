;; Notes to self:
;; =============
;;
;; - Make sure the file browser-repl is in the path, the Emacs variable
;;   inferior-lisp-program is set to call it, and the launch command in
;;   project.clj is correct for my browser of the day.
;; - C-c C-z starts an inferior lisp and opens a browser page.
;; - C-x C-e executes the s-expression before the cursor.
;; - To reload this file, reload browser page and run (load-namespace "hello").
;; - Execute the "ns" instruction below after the first "load-namespace".
;; - To start the animation, execute (go).

(ns hello
  (:require [org.gavrog.cljs.vectormath :as v]
            [org.gavrog.cljs.threejs :as t]
            [enfocus.core :as ef])
  (:require-macros [enfocus.macros :as em]))

(defn- stick
  ([p q radius segments]
     (let [n segments
           d (v/normalized (map - q p))
           u (v/cross d (if (> (v/dot d [1 0 0]) 0.9) [0 1 0] [1 0 0]))
           v (v/cross d u)
           a (-> Math/PI (* 2) (/ n))
           corner #(let [x (* a %), c (Math/cos x), s (Math/sin x)]
                     (v/scaled radius (map + (v/scaled c u) (v/scaled s v))))
           section (map corner (range n))]
       (t/geometry
        (concat (map #(map + % p) section) (map #(map + % q) section))
        (for [i (range n) :let [j (-> i (+ 1) (mod n))]]
          [i j (+ j n) (+ i n)]))))
  ([p q radius]
     (stick p q radius 8)))

(defn- ball-and-stick [name positions edges ball-material stick-material]
  (let [balls (for [[k p] positions]
                (t/mesh (pr-str k) p (t/sphere 10 8 8) (ball-material)))
        sticks (for [[u v] edges]
                 (t/mesh (pr-str [u v]) [0 0 0]
                         (stick (positions u) (positions v) 5)
                         (stick-material)))]
    (apply t/group (concat [name [0 0 0]] balls sticks))))

(def ^{:private true} viewport {:width 400 :height 300})

(def ^{:private true} camera
  (let [{:keys [width height]} viewport]
    (t/camera "camera" [0 0 350] {:aspect (/ width height)})))

(def ^{:private true} group
  (t/group "group" [0 0 0]
           (t/mesh "center" [0 0 0] (t/sphere 50 16 16)
                   (t/phong {:color 0xFFDD40}))
           (ball-and-stick "graph"
                           {:--- [-50 -50 -50] :--+ [-50 -50  50]
                            :-+- [-50  50 -50] :-++ [-50  50  50]
                            :+-- [ 50 -50 -50] :+-+ [ 50 -50  50]
                            :++- [ 50  50 -50] :+++ [ 50  50  50]}
                           [[:--- :--+] [:-+- :-++] [:+-- :+-+] [:++- :+++]
                            [:--- :-+-] [:--+ :-++] [:+-- :++-] [:+-+ :+++]
                            [:--- :+--] [:--+ :+-+] [:-+- :++-] [:-++ :+++]]
                           #(t/phong {:color 0xCC2020 :shininess 100})
                           #(t/phong {:color 0x2020CC :shininess 100}))))

(def ^{:private true} scene
  (t/scene group
           (t/light "main" [150 300 1000] 0xCCCCCC)
           (t/light "fill" [-300 -100 1000] 0x444444)
           (t/light "back" [300 300 -1000] 0x8080FF)
           camera))

(def ^{:private true} renderer
  (let [{:keys [width height]} viewport]
    (t/renderer width height {:antialias true :precision "highp"})))

(def ^{:private true} x-mouse (atom nil))
(def ^{:private true} y-mouse (atom nil))
(def ^{:private true} selected (atom nil))

(defn- mouse-position [event elem]
  (let [doc-body (.-body js/document)
        doc-elem (.-documentElement js/document)
        rect (.getBoundingClientRect elem)]
    [(+ (.-clientX event) (.-scrollLeft doc-body) (.-scrollLeft doc-elem)
        (- (.-left rect)))
     (+ (.-clientY event) (.-scrollTop doc-body) (.-scrollTop doc-elem)
        (- (.-top rect)))]))

(defn- to-viewport [[x y] elem]
  (let [rect (.getBoundingClientRect elem)
        wd (- (.-right rect) (.-left rect))
        ht (- (.-bottom rect) (.-top rect))]
    [(-> x (/ wd) (* 2) (- 1))
     (-> y (/ ht) (* -2) (+ 1))]))

(defn- update-mouse [event elem]
  (let [[x-elem y-elem] (mouse-position event elem)
        [x-cam y-cam] (to-viewport [x-elem y-elem] elem)]
    (reset! x-mouse x-cam)
    (reset! y-mouse y-cam)))

(em/defaction report-status []
  ["#status"] (em/content (when-let [[elem _] @selected]
                            (str (t/name elem) " is selected."))))

(defn- picked-objects [[x y]]
  (t/pick [x y] camera (for [group (t/children scene)
                             :when (= "group" (t/name group))
                             graph (t/children group)
                             :when (= "graph" (t/name graph))
                             child (t/children graph)]
                         child)))

(defn- highlight-selected []
  (let [[elem old-color] @selected
        found (first (picked-objects [@x-mouse @y-mouse]))]
    (if (not= found elem)
      (do (when elem (t/set-color! elem old-color))
          (if found
            (do
              (reset! selected [found (t/color found)])
              (t/set-color! found 0x00ff00))
            (reset! selected nil))))))

(defn- render []
  (do
    (t/set-rotation! group [0 (* (.now js/Date) 0.0001) 0])
    (t/render renderer scene camera)
    (highlight-selected)
    (report-status)))

(defn- animate []
  (.requestAnimationFrame js/window animate)
  (render))

(defn- go []
  (let [elem (.-domElement renderer)]
    (em/at js/document
           ["#container"] (em/append elem)
           ["#container"] (em/listen :mousemove #(update-mouse % elem)))
    (.log js/console "Starting animation")
    (animate)))

(set! (.-onload js/window) go)
