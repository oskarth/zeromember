(ns zeromember.snarks
  (:require
    ["fs" :as fs]
    ["snarkjs" :as zksnark]))

;; fs only available in node, not in browser
;; so this needs to be served from http server, or generated in browser

;; Load circuit
(def circuit-def
  (.parse js/JSON
          (fs/readFileSync "snarks/circuit.json" "utf-8")))

(def circuit
  (new zksnark/Circuit circuit-def))

;; Inspect circuit
;; (.-nConstraints circuit)

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

;; (trusted-setup! circuit)
