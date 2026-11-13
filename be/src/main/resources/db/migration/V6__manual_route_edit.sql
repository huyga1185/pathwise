-- T28 (P-58): a stop the dispatcher took off its route with PUT /plans/{id}/routes, so PlanDetail says MANUAL instead
-- of INFEASIBLE. Optimize, PUT /plans/{id} and PATCH of a stop set it back to FALSE.
ALTER TABLE route_plan_stops
    ADD COLUMN manually_unassigned BOOLEAN NOT NULL DEFAULT FALSE AFTER planned_arrival;
