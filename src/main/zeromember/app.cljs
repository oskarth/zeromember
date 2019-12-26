(ns zeromember.app
  (:require
    [goog.dom :as gdom]
    [rum.core :as rum]
    [zeromember.snarks :as snarks]))

(defonce app-state (atom {:text "Zeromember" :circuit "test"}))

(defn init []
  (println "Hello world"))

(defn get-app-element []
  (gdom/getElement "app"))

;; When making it reactive we get the following error:
;; Warning: componentWillReceiveProps has been renamed, and is not recommended for use.
(rum/defc hello-world < rum/reactive []
  [:div
   [:h1 (:text @app-state)]
   [:h3 "test ok"]
   [:button {:on-click #(println "Hi")} "Prove"]
   [:p (str (.-nConstraints (:circuit (rum/react app-state))))]])

(defn mount [el]
  (rum/mount (hello-world) el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (snarks/load-everything!
      #(swap! app-state assoc :circuit %))
    (mount el)))

(mount-app-element)
