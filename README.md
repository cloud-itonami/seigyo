# seigyo — Industrial Control Actor

**DID**: `did:web:seigyo.etzhayyim.com`
**Namespace**: `com.etzhayyim.seigyo.*`
**Status**: migration boundary for industrial-control domain cells

## Migration Boundary

`src/seigyo/murakumo.cljc` is the Murakumo-facing cljc actor boundary for the
legacy seigyo kotoba-kotodama cells:

- `seigyo_plc_program_lifecycle` -> `plcProgramAttestation`
- `seigyo_interlock_attestation` -> `interlockVerificationRecord`
- `seigyo_scada_gateway` -> `scadaProjectAttestation`, `alarmEventRecord`
- `seigyo_opcua_bridge` -> `opcuaDispatchRecord`
- `seigyo_historian_aggregate` -> `telemetryAggregateRecord`

The actor never sits in the hardwired safety path. PLC, SCADA, and OPC UA
plans are blocked unless Council fleet attestation, silen-seigyo baseline
review, Charter Rider scan, runtime hash, setpoint envelope, qualified engineer
review, lockout-tagout, Murakumo-only, and kotoba-only attestations are present.
