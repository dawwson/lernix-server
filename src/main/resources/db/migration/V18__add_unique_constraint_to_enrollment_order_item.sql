ALTER TABLE enrollments
    ADD CONSTRAINT uk_enrollment_order_item UNIQUE (order_item_id);
