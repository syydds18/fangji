-- 建房记账 v2.0 新增3张表
-- 在 Supabase SQL Editor 中执行

-- 1. 电器大件表
CREATE TABLE IF NOT EXISTS appliances (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL DEFAULT '',
    brand TEXT DEFAULT '',
    model TEXT DEFAULT '',
    category TEXT DEFAULT '',
    purchase_channel TEXT DEFAULT '',
    purchase_date BIGINT DEFAULT 0,
    price DOUBLE PRECISION DEFAULT 0,
    warranty_years INT DEFAULT 0,
    warranty_expire_date BIGINT DEFAULT 0,
    install_date BIGINT DEFAULT 0,
    after_sale_phone TEXT DEFAULT '',
    serial_number TEXT DEFAULT '',
    note TEXT DEFAULT '',
    image_paths TEXT DEFAULT '',
    created_at BIGINT DEFAULT 0,
    user_id UUID REFERENCES auth.users(id)
);
ALTER TABLE appliances ENABLE ROW LEVEL SECURITY;
CREATE POLICY "users_own_appliances" ON appliances FOR ALL TO authenticated
    USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- 2. 证件资料表
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL DEFAULT '',
    category TEXT DEFAULT '',
    storage_location TEXT DEFAULT '',
    related_party TEXT DEFAULT '',
    issue_date BIGINT DEFAULT 0,
    expire_date BIGINT DEFAULT 0,
    note TEXT DEFAULT '',
    image_paths TEXT DEFAULT '',
    created_at BIGINT DEFAULT 0,
    user_id UUID REFERENCES auth.users(id)
);
ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
CREATE POLICY "users_own_documents" ON documents FOR ALL TO authenticated
    USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- 3. 维修记录表
CREATE TABLE IF NOT EXISTS maintenance (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL DEFAULT '',
    category TEXT DEFAULT '',
    location TEXT DEFAULT '',
    description TEXT DEFAULT '',
    repair_date BIGINT DEFAULT 0,
    cost DOUBLE PRECISION DEFAULT 0,
    worker_name TEXT DEFAULT '',
    worker_phone TEXT DEFAULT '',
    status TEXT DEFAULT '待维修',
    cause TEXT DEFAULT '',
    note TEXT DEFAULT '',
    image_paths TEXT DEFAULT '',
    created_at BIGINT DEFAULT 0,
    user_id UUID REFERENCES auth.users(id)
);
ALTER TABLE maintenance ENABLE ROW LEVEL SECURITY;
CREATE POLICY "users_own_maintenance" ON maintenance FOR ALL TO authenticated
    USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
