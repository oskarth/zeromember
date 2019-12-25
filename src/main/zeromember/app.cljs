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

(rum/defc hello-world []
  [:div
   [:h1 (:text @app-state)]
   [:h3 "test ok"]
   [:button {:on-click #(println "Hi")} "Prove"]
   [:p (str (.-nConstraints (:circuit @app-state)))]])

(defn mount [el]
  (rum/mount (hello-world) el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (snarks/load-everything!
      #(swap! app-state assoc :circuit %))
    (mount el)))

(mount-app-element)
