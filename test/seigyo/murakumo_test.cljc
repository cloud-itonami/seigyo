(ns seigyo.murakumo-test
  (:require [clojure.test :refer [deftest is]]
            [seigyo.murakumo :as seigyo]))

(def full-attestations
  (into {}
        (map (fn [gate] [gate (str "attested-" (name gate))]))
        (distinct (mapcat :required-gates (vals seigyo/cell-specs)))))

(deftest maps-all-legacy-seigyo-cells
  (is (= #{"seigyo_historian_aggregate"
           "seigyo_interlock_attestation"
           "seigyo_opcua_bridge"
           "seigyo_plc_program_lifecycle"
           "seigyo_scada_gateway"}
         (set (map :legacy-cell (vals seigyo/cell-specs))))))

(deftest r0-gates-block-control-effects
  (let [plan (seigyo/cell-plan :plc-program-lifecycle
                               {:site-id "site-001"
                                :controller-id "plc-001"
                                :program-cid "bafkreiprogram"})]
    (is (= :blocked (:status plan)))
    (is (= [:council-fleet-attestation
            :silen-seigyo-baseline-review
            :charter-rider-scan-baseline
            :site-authority-baseline
            :qualified-engineer-review-baseline
            :runtime-hash-attestation-baseline
            :setpoint-envelope-baseline
            :lockout-tagout-baseline
            :hardwired-safety-path-baseline
            :human-override-baseline
            :murakumo-only-inference-baseline
            :kotoba-only-substrate-baseline
            :open-implementation-baseline
            :no-commercial-scada-hmi-runtime-baseline
            :iec-61131-3-static-check-baseline
            :openplc-simulation-baseline
            :program-cid-baseline
            :engineer-signoff-baseline
            :no-safety-path-membership-baseline]
           (:missing-gates plan)))
    (is (empty? (:effects plan)))))

(deftest plc-program-plan-is-not-safety-path
  (let [plan (seigyo/cell-plan :plc-program-lifecycle
                               {:attestations full-attestations
                                :site-id "site-001"
                                :controller-id "plc-001"
                                :program-cid "bafkreiprogram"
                                :runtime-hash "sha256:runtime"
                                :envelope-cid "bafkreienvelope"
                                :engineer-did "did:example:engineer"})
        effect (first (:effects plan))]
    (is (= :ready (:status plan)))
    (is (= "com.etzhayyim.seigyo.plcProgramAttestation" (:collection effect)))
    (is (= false (get-in effect [:record :controlWriteAuthorized])))
    (is (= false (get-in effect [:record :hardwiredSafetyPathMember])))))

(deftest interlock-attestation-requires-physical-evidence
  (let [attestations (dissoc full-attestations :photo-measurement-evidence-cid-baseline)
        plan (seigyo/cell-plan :interlock-attestation
                               {:attestations attestations
                                :evidence-cid "bafkreievidence"})]
    (is (= :blocked (:status plan)))
    (is (= [:photo-measurement-evidence-cid-baseline] (:missing-gates plan)))))

(deftest scada-gateway-rejects-proprietary-runtime
  (let [attestations (dissoc full-attestations :no-proprietary-hmi-runtime-baseline)
        plan (seigyo/cell-plan :scada-gateway
                               {:attestations attestations
                                :alarm-id "alarm-001"})]
    (is (= :blocked (:status plan)))
    (is (= [:no-proprietary-hmi-runtime-baseline] (:missing-gates plan)))))

(deftest opcua-bridge-requires-readback-and-envelope
  (let [attestations (-> full-attestations
                         (dissoc :envelope-precheck-baseline)
                         (dissoc :readback-confirm-baseline))
        plan (seigyo/cell-plan :opcua-bridge
                               {:attestations attestations
                                :dispatch-id "dispatch-001"})]
    (is (= :blocked (:status plan)))
    (is (= [:envelope-precheck-baseline :readback-confirm-baseline]
           (:missing-gates plan)))))

(deftest historian-keeps-full-rate-data-onsite
  (let [plan (seigyo/cell-plan :historian-aggregate
                               {:attestations full-attestations
                                :aggregate-id "agg-001"
                                :bucket-start "2026-06-29T00:00:00Z"
                                :bucket-end "2026-06-29T00:01:00Z"})
        effect (first (:effects plan))]
    (is (= :ready (:status plan)))
    (is (= "com.etzhayyim.seigyo.telemetryAggregateRecord" (:collection effect)))
    (is (= true (get-in effect [:record :northboundAggregateOnly])))
    (is (= false (get-in effect [:record :fullRateNorthbound])))))

(deftest all-cell-plans-ready-when-attested
  (let [plans (seigyo/all-cell-plans {:attestations full-attestations
                                      :site-id "site-001"
                                      :controller-id "plc-001"
                                      :program-cid "bafkreiprogram"
                                      :runtime-hash "sha256:runtime"
                                      :envelope-cid "bafkreienvelope"
                                      :engineer-did "did:example:engineer"
                                      :evidence-cid "bafkreievidence"
                                      :alarm-id "alarm-001"
                                      :io-point-id "io-001"
                                      :dispatch-id "dispatch-001"
                                      :aggregate-id "agg-001"
                                      :bucket-start "2026-06-29T00:00:00Z"
                                      :bucket-end "2026-06-29T00:01:00Z"})]
    (is (= (set (keys seigyo/cell-specs)) (set (keys plans))))
    (is (every? #(= :ready (:status %)) (vals plans)))
    (is (= 6 (count (mapcat :effects (vals plans)))))))
