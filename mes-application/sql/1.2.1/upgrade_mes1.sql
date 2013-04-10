-- Table: materialflow_transfer
-- changed: 25.02.2013

ALTER TABLE materialflow_transfer ADD COLUMN fromdelivery_id bigint;

ALTER TABLE materialflow_transfer
  ADD CONSTRAINT transfer_delivery_fkey FOREIGN KEY (fromdelivery_id)
      REFERENCES deliveries_delivery (id) DEFERRABLE;

-- end


-- Table: masterorders_masterorder
-- changed: 26.02.2013

CREATE TABLE masterorders_masterorder
(
  id bigint NOT NULL,
  "number" character varying(255),
  name character varying(1024),
  description character varying(2048),
  externalnumber character varying(255),
  deadline timestamp without time zone,
  addmasterprefixtonumber boolean,
  masterorderquantity numeric(12,5),
  cumulatedorderquantity numeric(12,5),
  masterordertype character varying(255) DEFAULT '01undefined'::character varying,
  masterorderstate character varying(255),
  company_id bigint,
  product_id bigint,
  technology_id bigint,
  externalsynchronized boolean DEFAULT true,
  CONSTRAINT masterorders_masterorder_pkey PRIMARY KEY (id),
  CONSTRAINT masterorder_company_fkey FOREIGN KEY (company_id)
      REFERENCES basic_company (id) DEFERRABLE,
  CONSTRAINT masterorder_product_fkey FOREIGN KEY (product_id)
      REFERENCES basic_product (id) DEFERRABLE,
  CONSTRAINT masterorder_technology_fkey FOREIGN KEY (technology_id)
      REFERENCES technologies_technology (id) DEFERRABLE
);

-- end


-- Table: masterorders_masterorderproduct
-- changed: 26.02.2013

CREATE TABLE masterorders_masterorderproduct
(
  id bigint NOT NULL,
  product_id bigint,
  technology_id bigint,
  masterorder_id bigint,
  masterorderquantity numeric(12,5),
  cumulatedorderquantity numeric(12,5),
  CONSTRAINT masterorders_masterorderproduct_pkey PRIMARY KEY (id),
  CONSTRAINT fk215b549b4a728bc8 FOREIGN KEY (masterorder_id)
      REFERENCES masterorders_masterorder (id) DEFERRABLE,
  CONSTRAINT masterorderproduct_product_fkey FOREIGN KEY (product_id)
      REFERENCES basic_product (id) DEFERRABLE,
  CONSTRAINT masterorderproduct_technology_fkey FOREIGN KEY (technology_id)
      REFERENCES technologies_technology (id) DEFERRABLE
);

-- end


-- Table: orders_order
-- changed: 26.02.2013

ALTER TABLE orders_order ADD COLUMN masterorder_id bigint;
ALTER TABLE orders_order
  ADD CONSTRAINT order_masterorder_fkey FOREIGN KEY (masterorder_id)
      REFERENCES masterorders_masterorder (id) DEFERRABLE;

-- end


-- Table: orders_order
-- changed: 26.02.2013

ALTER TABLE orders_order DROP COLUMN batchnumber;

-- end


-- Table: orders_order
-- changed: 26.02.2013

ALTER TABLE orders_order DROP COLUMN ordergroup_id;

-- end


-- Table: ordergroups_ordergroup
-- changed: 26.02.2013

DROP TABLE ordergroups_ordergroup;

-- end


-- Table: orders_typeofcorrectioncauses
-- changed: 07.03.2013

CREATE TABLE orders_typeofcorrectioncauses
(
  id bigint NOT NULL,
  reasontype character varying(255),
  order_id bigint,
  CONSTRAINT orders_typeofcorrectioncauses_pkey PRIMARY KEY (id),
  CONSTRAINT typeofcorrectioncauses_order_fkey FOREIGN KEY (order_id)
      REFERENCES orders_order (id) DEFERRABLE
);

-- end


-- Table: orders_order
-- changed: 07.03.2013

ALTER TABLE orders_order ADD COLUMN commissionedplannedquantity numeric(10,5);
ALTER TABLE orders_order ADD COLUMN commissionedcorrectedquantity numeric(10,5);
ALTER TABLE orders_order ADD COLUMN amountofproductproduced numeric(10,5);
ALTER TABLE orders_order ADD COLUMN remainingamountofproducttoproduce numeric(10,5);
ALTER TABLE orders_order ADD COLUMN commentreasontypedeviationsquantity character varying(255);

-- end


-- Table: orders_order
-- changed: 07.03.2013

CREATE TABLE orders_reasontypecorrectiondatefrom
(
  id bigint NOT NULL,
  order_id bigint,
  reasontypeofchaningorderstate character varying(255),
  CONSTRAINT orders_reasontypecorrectiondatefrom_pkey PRIMARY KEY (id),
  CONSTRAINT reasontypecorrectiondatefrom_fkey FOREIGN KEY (order_id)
      REFERENCES orders_order (id) DEFERRABLE
);

-- end


-- Table: orders_reasontypecorrectiondateto
-- changed: 11.03.2013

CREATE TABLE orders_reasontypecorrectiondateto
(
  id bigint NOT NULL,
  order_id bigint,
  reasontypeofchaningorderstate character varying(255),
  CONSTRAINT orders_reasontypecorrectiondateto_pkey PRIMARY KEY (id),
  CONSTRAINT reasontypecorrectiondateto_order_fkey FOREIGN KEY (order_id)
      REFERENCES orders_order (id) DEFERRABLE
);

-- end


-- Table: orders_reasontypeofchaningorderstate
-- changed: 11.03.2013

CREATE TABLE orders_reasontypeofchaningorderstate
(
  id bigint NOT NULL,
  orderstatechange_id bigint,
  reasontypeofchaningorderstate character varying(255),
  productionpershift_id bigint,
  CONSTRAINT orders_reasontypeofchaningorderstate_pkey PRIMARY KEY (id),
  CONSTRAINT reasontypeofchaningorderstate_order_fkey FOREIGN KEY (productionpershift_id)
      REFERENCES productionpershift_productionpershift (id) DEFERRABLE,
  CONSTRAINT reasontypeofchaningorderstate_orderstatechange_fkey FOREIGN KEY (orderstatechange_id)
      REFERENCES orders_orderstatechange (id) DEFERRABLE
);

-- end


-- Table: productioncounting_recordoperationproductincomponent
-- changed: 29.03.2013

ALTER TABLE  productioncounting_recordoperationproductincomponent 
	ALTER  COLUMN plannedquantity SET DATA TYPE numeric(12,5);
ALTER TABLE  productioncounting_recordoperationproductincomponent 
	ALTER  COLUMN usedquantity SET DATA TYPE numeric(12,5);
 
-- end


-- Table: productioncounting_recordoperationproductoutcomponent
-- changed: 29.03.2013

ALTER TABLE  productioncounting_recordoperationproductoutcomponent 
	ALTER  COLUMN plannedquantity SET DATA TYPE numeric(12,5);
ALTER TABLE  productioncounting_recordoperationproductoutcomponent 
	ALTER  COLUMN usedquantity SET DATA TYPE numeric(12,5);
 
-- end
 
 
-- Table: deliveries_delivery
-- changed: 09.04.2013
 
ALTER TABLE deliveries_delivery ADD COLUMN relateddelivery_id bigint;

ALTER TABLE deliveries_delivery
 	ADD CONSTRAINT delivery_delivery_fkey FOREIGN KEY (relateddelivery_id)
 	REFERENCES deliveries_delivery (id) DEFERRABLE;
 	
-- end
