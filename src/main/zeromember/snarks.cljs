(ns zeromember.snarks
  (:require
    ["fs" :as fs]
    ["snarkjs" :as zksnark]))

;; fs only available in node, not in browser
;; so this needs to be served from http server, or generated in browser

(def circuit-def
  (.parse js/JSON
          (fs/readFileSync "snarks/circuit.json" "utf-8")))

(def circuit
  (new zksnark/Circuit circuit-def))

(.-nConstraints circuit)
