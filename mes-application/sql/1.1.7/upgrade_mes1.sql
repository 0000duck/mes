-- Table: basic_parameter
-- changed: 21.05.2012

ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenCorrectingDateFrom TYPE boolean;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenCorrecringDateFrom SET DEFAULT false;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenCorrectingDateTo TYPE boolean;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenCorrectingDateTo SET DEFAULT false;

ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenChangingStateToDeclined TYPE boolean;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenChangingStateToDeclined SET DEFAULT false;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenChangingStateToInterrupted TYPE boolean;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenChangingStateToInterrupted SET DEFAULT false;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenChangingStateToAbandoned TYPE boolean;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenChangingStateToAbandoned SET DEFAULT false;

ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenDelayedEffectiveDateFrom TYPE boolean;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenDelayedEffectiveDateFrom SET DEFAULT false;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenEarliedEffectiveDateFrom  TYPE boolean;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenEarliedEffectiveDateFrom SET DEFAULT false;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenDelayedEffectiveDateTo TYPE boolean;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenDelayedEffectiveDateTo SET DEFAULT false;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenEarliedEffectiveDateTo  TYPE boolean;
ALTER TABLE basic_parameter ALTER COLUMN reasonNeededWhenEarliedEffectiveDateTo SET DEFAULT false;

ALTER TABLE basic_parameter ALTER COLUMN delayedEffectiveDateFrom  TYPE integer;
ALTER TABLE basic_parameter ALTER COLUMN earliedEffectiveDateFrom TYPE integer;
ALTER TABLE basic_parameter ALTER COLUMN delayedEffectiveDateTo TYPE integer;
ALTER TABLE basic_parameter ALTER COLUMN earliedEffectiveDateTo TYPE integer;

-- end
