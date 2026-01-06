-- 1. orders_seq 생성 (없을 경우)
CREATE SEQUENCE IF NOT EXISTS orders_seq
    START WITH 1
    INCREMENT BY 100;

-- 2. orders_seq increment 값 보정 (이미 존재하는 경우 대비)
ALTER SEQUENCE orders_seq
    INCREMENT BY 100;

-- 3. orders.id 기본값을 orders_seq로 강제
ALTER TABLE orders
    ALTER COLUMN id SET DEFAULT nextval('orders_seq');