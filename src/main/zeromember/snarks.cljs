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

;; Trusted setup
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

;; Generate proof
(def input
  (clj->js {"a" "123"
            "b" "456"}))

(def witness (. circuit calculateWitness input))

(def vk-proof
  (zksnark/unstringifyBigInts
    (.parse js/JSON
            (fs/readFileSync "snarks/circuit.vk_proof" "utf-8"))))

(def raw-proof (. zksnark/groth genProof vk-proof witness))
(def proof (get (js->clj raw-proof) "proof"))
(def public-signals (get (js->clj raw-proof) "publicSignals"))

;; Verifier
(def vk-verifier
  (zksnark/unstringifyBigInts
    (.parse js/JSON
            (fs/readFileSync "snarks/circuit.vk_verifier" "utf-8"))))

(. zksnark/groth isValid vk-verifier
   (clj->js proof)
   (clj->js public-signals))
;; => true
