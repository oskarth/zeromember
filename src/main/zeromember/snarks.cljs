(ns zeromember.snarks
  (:require
    [clojure.core.async :as async
     :refer [go chan put! alts!]]
    ["fs" :as fs]
    ["snarkjs" :as zksnark]
;;    ["worker_threads" :as worker_threads]
;;    ["websnark" :as websnark]

    ))

;; Error in frontend compilation when including websnarks
;; No such JS dependency in npm
;;    The required JS dependency "worker_threads" is not available, it was required by "node_modules/websnark/src/bn128.js".
;;    ["websnark" :as websnark]
;;
;; Possible we can "--experimental-worker" flag to enable it. Same error for node v122, where it is no longer experimental.
;; But also not clear how that'd help with browser usage
;; It is an internal dependency https://nodejs.org/docs/latest-v12.x/api/worker_threads.html but I don't know how to include it.

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

;; XXX: Naive error handling
;; NOTE: For websnarks .json should be .arrayBufffer
(defn load-resource!
  "Loads a resource, either from filesystem or HTTP server.
  Returns a vector [k loaded-resource] in a channel."
  [resource k c]
  (if (in-browser?)
    (-> (js/fetch resource)
        (.then #(.json %))
        (.then #(async/put! c [k %])))
    (put! c [k (->> (fs/readFileSync resource "utf-8")
                    (.parse js/JSON))])))

;; {:circuit 1 :vk-proof 1 :vk-verifier}

;; Load all circuit assets and pass back to app
;; This is async so will happen after
(defn load-everything! [success-fn]
  (let [c1 (chan)
        c2 (chan)
        c3 (chan)
        cs [c1 c2 c3]]
    (load-resource! "snarks/circuit.json" :circuit c1)
    (load-resource! "snarks/circuit.vk_proof" :vk-proof c2)
    ;; TODO: load vk-verifier
    (go
      (dotimes [i 3]
        (let [[v c] (alts! cs)]
          (cond
            (= (first v) :circuit)
            (do (println "got a circuit")
                (let [circuit (new zksnark/Circuit (second v))]
                  (success-fn [:circuit circuit])))

            (= (first v) :vk-proof)
            (do (println "got a vk-proof")
                (let [vk-proof (zksnark/unstringifyBigInts (second v))]
                  (success-fn [:vk-proof vk-proof])))

            :else (println "Unknown resource")))))))

;; Generate proof
(def static-input
  (clj->js {"a" "123"
            "b" "456"}))

;;     window.groth16GenProof

;; XXX: witness probably different
;; pkey.slice is not a function, so probably diff here
;; TODO: HERE ATM https://github.com/iden3/websnark#important-please-be-sure-you-run-your-setup-with---protocol-groth--websnark-only-generates-groth16-proofs
;; I'd like to run this in node at least, to get the binary rep
;; Especially as it is a witness, f(input)
(defn prove-wasm [circuit vk-proof]
  (let [input static-input
        witness (. circuit calculateWitness input)]
    (-> (.groth16GenProof js/window witness vk-proof)
        (.then #(println "wasm proof" %)))))

(defn prove [circuit vk-proof]
  (let [input static-input
        witness (. circuit calculateWitness input)
        raw-proof (. zksnark/groth genProof vk-proof witness)
        proof (get (js->clj raw-proof) "proof")
        public-signals (get (js->clj raw-proof) "publicSignals") ]

  (println "snarks/prove input" input)
  (println "snarks/prove witness" witness)
  (println "snarks/prove proof" proof)
  (println "snarks/prove public-signals" public-signals)
  proof))



(comment

;; Assuming only done for node environment
;; (trusted-setup! circuit)

;; Once ready
;; (let [circuit (:circuit @snarks-state)])

;; Inspect circuit
;; (.-nConstraints circuit)

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
