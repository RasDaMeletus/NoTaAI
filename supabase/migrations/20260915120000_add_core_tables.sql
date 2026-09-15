-- Supabase Production Migration for ViNote-2 (NoTa) — Idempotent Version
-- Created: 2026-09-15
-- Safe to re-run: uses IF NOT EXISTS / IF EXISTS everywhere

-- 1. WALLET ACCOUNTS TABLE
CREATE TABLE IF NOT EXISTS public.wallet_accounts (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    type TEXT NOT NULL DEFAULT 'EWALLET' CHECK (type IN ('EWALLET', 'BANK', 'CASH', 'INVESTMENT')),
    calculated_balance BIGINT NOT NULL DEFAULT 0,
    provider_reported_balance BIGINT,
    last_reconciled_at TIMESTAMPTZ,
    is_auto_detect_enabled BOOLEAN DEFAULT TRUE,
    icon_color_hex TEXT DEFAULT '#0057C2',
    account_number TEXT DEFAULT '',
    is_connected BOOLEAN DEFAULT TRUE,
    last_sync_timestamp TIMESTAMPTZ DEFAULT now(),
    gateway_type TEXT DEFAULT 'MANUAL' CHECK (gateway_type IN ('MIDTRANS', 'UNOFFICIAL', 'MANUAL')),
    linked_account_id TEXT DEFAULT '',
    gateway_access_token TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.wallet_accounts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own wallet accounts" ON public.wallet_accounts;
CREATE POLICY "Users can view own wallet accounts"
    ON public.wallet_accounts FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own wallet accounts" ON public.wallet_accounts;
CREATE POLICY "Users can insert own wallet accounts"
    ON public.wallet_accounts FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own wallet accounts" ON public.wallet_accounts;
CREATE POLICY "Users can update own wallet accounts"
    ON public.wallet_accounts FOR UPDATE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own wallet accounts" ON public.wallet_accounts;
CREATE POLICY "Users can delete own wallet accounts"
    ON public.wallet_accounts FOR DELETE USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_wallet_accounts_user_id ON public.wallet_accounts(user_id);

-- Add any missing columns
ALTER TABLE public.wallet_accounts ADD COLUMN IF NOT EXISTS gateway_type TEXT DEFAULT 'MANUAL';
ALTER TABLE public.wallet_accounts ADD COLUMN IF NOT EXISTS linked_account_id TEXT DEFAULT '';
ALTER TABLE public.wallet_accounts ADD COLUMN IF NOT EXISTS gateway_access_token TEXT DEFAULT '';


-- 2. TRANSACTIONS TABLE
CREATE TABLE IF NOT EXISTS public.transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    amount BIGINT NOT NULL,
    category TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('EXPENSE', 'INCOME')),
    tx_timestamp TIMESTAMPTZ DEFAULT now(),
    time_label TEXT DEFAULT 'Today',
    merchant TEXT DEFAULT '',
    source TEXT DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'VOICE', 'SCAN', 'E_WALLET', 'AUTO_DETECTED', 'BANK_SYNC')),
    wallet_name TEXT,
    fingerprint TEXT,
    confidence REAL DEFAULT 1.0,
    is_confirmed BOOLEAN DEFAULT TRUE,
    sync_state TEXT DEFAULT 'SYNCED',
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own transactions" ON public.transactions;
CREATE POLICY "Users can view own transactions"
    ON public.transactions FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own transactions" ON public.transactions;
CREATE POLICY "Users can insert own transactions"
    ON public.transactions FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own transactions" ON public.transactions;
CREATE POLICY "Users can update own transactions"
    ON public.transactions FOR UPDATE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own transactions" ON public.transactions;
CREATE POLICY "Users can delete own transactions"
    ON public.transactions FOR DELETE USING (auth.uid() = user_id);

-- Add any missing columns
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS tx_timestamp TIMESTAMPTZ DEFAULT now();
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS time_label TEXT DEFAULT 'Today';
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS merchant TEXT DEFAULT '';
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS wallet_name TEXT;
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS fingerprint TEXT;
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS confidence REAL DEFAULT 1.0;
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS is_confirmed BOOLEAN DEFAULT TRUE;
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS sync_state TEXT DEFAULT 'SYNCED';

CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON public.transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_type ON public.transactions(user_id, type);
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON public.transactions(tx_timestamp DESC);


-- 3. BUDGETS TABLE
CREATE TABLE IF NOT EXISTS public.budgets (
    id TEXT PRIMARY KEY DEFAULT 'default_budget',
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    monthly_limit BIGINT NOT NULL DEFAULT 3000000,
    daily_limit BIGINT NOT NULL DEFAULT 100000,
    warning_threshold_percent REAL DEFAULT 0.85,
    period_month_year TEXT NOT NULL DEFAULT '2026-08',
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT unique_user_period UNIQUE (user_id, period_month_year)
);

ALTER TABLE public.budgets ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own budgets" ON public.budgets;
CREATE POLICY "Users can view own budgets"
    ON public.budgets FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own budgets" ON public.budgets;
CREATE POLICY "Users can insert own budgets"
    ON public.budgets FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own budgets" ON public.budgets;
CREATE POLICY "Users can update own budgets"
    ON public.budgets FOR UPDATE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own budgets" ON public.budgets;
CREATE POLICY "Users can delete own budgets"
    ON public.budgets FOR DELETE USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_budgets_user_period ON public.budgets(user_id, period_month_year);


-- 4. UPDATED_AT TRIGGER (idempotent)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_wallet_accounts_updated_at ON public.wallet_accounts;
CREATE TRIGGER update_wallet_accounts_updated_at
    BEFORE UPDATE ON public.wallet_accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_transactions_updated_at ON public.transactions;
CREATE TRIGGER update_transactions_updated_at
    BEFORE UPDATE ON public.transactions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_budgets_updated_at ON public.budgets;
CREATE TRIGGER update_budgets_updated_at
    BEFORE UPDATE ON public.budgets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
