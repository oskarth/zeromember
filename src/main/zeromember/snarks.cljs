(ns zeromember.snarks
  (:require
    [clojure.core.async :as async :refer [go put!]]
    ["fs" :as fs]
    ["snarkjs" :as zksnark]))

;; We need to separate what can happen in node and in browser

;; fs only available in node, not in browser
;; so this needs to be served from http server, or generated in browser

;; XXX: Mismatch between snarks and public/snarks for platforms

(defn in-browser? []
  (exists? js/window))

(defonce snarks-state (atom {}))

;; Trusted setup
;; Only possible in node right now
(defn trusted-setup! [circuit]
  (let [setup ((. zksnark/groth -setup) circuit)]
    (do
      (fs/writeFileSync
        "snarks/circuit.vk_proof"
        (.stringify js/JSON (zksnark/stringifyBigInts setup.vk_proof))
        "utf-8")
      (fs/writeFileSync
        "snarks/circuit.vk_verifier"
        (.stringify js/JSON (zksnark/stringifyBigInts setup.vk_verifier))
        "utf-8"))))

;; Load circuit
;;(def circuit-def
(defn load-circuit! [c]
  (if (in-browser?)
    (-> (js/fetch "snarks/circuit.json")
               (.then #(.json %))
;; For websnarks : (.then #(.arrayBuffer %))
               (.then #(async/put! c %)))
    (put! c (.parse js/JSON
                    (fs/readFileSync "snarks/circuit.json" "utf-8")))))

;; Load all circuit assets and pass back to app
;; This is async so will happen after
(defn load-everything! [success-fn]
  (let [c (async/chan)]
    (load-circuit! c)
    (async/go
      (let [v (async/<! c)
            circuit (new zksnark/Circuit v)]
        ;; XXX: Duplicate here, probably move to app-state only
        ;; And create data flow here
        (swap! snarks-state assoc :circuit circuit)
        (success-fn circuit)

        ;; TODO: Countinue here

        ))))

;; Generate proof
(def input
  (clj->js {"a" "123"
            "b" "456"}))

(comment

;; Assuming only done for node environment
;; (trusted-setup! circuit)

;; Once ready
;; (let [circuit (:circuit @snarks-state)])

;; Inspect circuit
;; (.-nConstraints circuit)

(def witness (. circuit calculateWitness input))

(def vk-proof
  (zksnark/unstringifyBigInts
    (if (in-browser?)
      nil
      (.parse js/JSON
              (fs/readFileSync "snarks/circuit.vk_proof" "utf-8")))))

(def raw-proof (. zksnark/groth genProof vk-proof witness))
(def proof (get (js->clj raw-proof) "proof"))
(def public-signals (get (js->clj raw-proof) "publicSignals"))

;; Verifier
(def vk-verifier
  (zksnark/unstringifyBigInts
    (if (in-browser?)
      nil
      (.parse js/JSON
              (fs/readFileSync "snarks/circuit.vk_verifier" "utf-8")))))

(. zksnark/groth isValid vk-verifier
   (clj->js proof)
   (clj->js public-signals))
;; => true

)
