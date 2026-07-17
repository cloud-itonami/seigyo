(require '[clojure.test :as t])

(doseq [ns-sym '[seigyo.methods.test-charter-gates
                  seigyo.repository-contract-test]]
  (require ns-sym))

(let [result (apply t/run-tests
                    '[seigyo.methods.test-charter-gates
                      seigyo.repository-contract-test])]
  (System/exit (if (zero? (+ (:fail result) (:error result))) 0 1)))
