# SpotBugs Defect Fixes

This document summarises some of the defects identified by SpotBugs static analysis
and the corrective actions applied.

---

## 1. Dead Store to `maxMoves`

**SpotBugs Code:** DLS_DEAD_LOCAL_STORE  
**Location:** `AvailabilityService.queryAvailableDrones`

### Issue
A local variable `maxMoves` was assigned but never used, indicating either
dead code or an incomplete implementation.

### Fix
The variable was integrated into the decision logic by enforcing a maximum
movement constraint on drones:

```

int moves = (int) Math.ceil(distance / STEP);
if (moves > maxMoves) {
    canHandleAll = false;
    break;
    

```
## 2. Incorrect Oddness Check for Negative Numbers 

Using value % 2 == 1 fails for negative integers due to Java’s modulo
semantics. changed to return (crossings % 2 != 0);

**Location** : DroneService.isInRegion(Position, Region)
## 3. Possible Null Pointer Dereference in REST Client
```
Map<String, Object> responseBody = response.getBody();
if (responseBody == null) {
    throw new IllegalStateException(
        "Face recognition service returned empty response body"
    );
}
```
ResponseEntity.getBody() may return null, leading to a potential runtime
null pointer dereference. SO i added a null check.

**Location** : client.FaceRecognitionClient.matchFaces(byte[], byte[]) 
