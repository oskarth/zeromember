(ns zeromember.app
  (:require
    [goog.dom :as gdom]
    [rum.core :as rum]))

(defonce app-state (atom {:text "Zeromember"}))

(defn init []
  (println "Hello world"))

(defn get-app-element []
  (gdom/getElement "app"))

(rum/defc hello-world []
  [:div
   [:h1 (:text @app-state)]
   [:h3 "test ok"]])

(defn mount [el]
  (rum/mount (hello-world) el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

(mount-app-element)
