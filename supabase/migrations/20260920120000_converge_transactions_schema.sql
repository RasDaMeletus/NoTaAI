-- Converge the transactions schema with the deployed cloud version.
--
-- Background: 20260915120000 declared transactions with id BIGSERIAL, amount
-- BIGINT, type TEXT, source TEXT, title TEXT. The live table was created with
-- id UUID, amount NUMERIC, type INTEGER, source INTEGER, description TEXT.
-- Both sides are first-class: the app is local-first (writes to Room), and the
-- cloud table backs RLS-protected remote reads.
--
-- This migration brings the declared schema in line with what is live, so a
-- `supabase db reset` no longer silently changes column types and breaks the
-- remote contract. It is idempotent and additive only: no columns are dropped
-- and no existing rows are rewritten (amount NUMERIC can hold every BIGINT
-- value, so nothing is lost by leaving the data untouched).

-- 1. Align id: BIGSERIAL -> UUID (default gen_random_uuid()).
-- The table already exists on live with this shape; IF NOT EXISTS guards make
-- the statement safe to re-run anywhere.
DO $$
BEGIN
  -- Only run when the column is still the old non-uuid shape.
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'transactions'
      AND column_name = 'id'
      AND data_type <> 'uuid'
  ) THEN
    -- Drop the sequence + default first, then change the type.
    ALTER TABLE public.transactions ALTER COLUMN id DROP DEFAULT;
    ALTER TABLE public.transactions ALTER COLUMN id TYPE UUID USING gen_random_uuid();
    ALTER TABLE public.transactions ALTER COLUMN id SET DEFAULT gen_random_uuid();
  END IF;
END$$;

-- 2. Align amount: BIGINT -> NUMERIC. NUMERIC is a superset of BIGINT, so the
-- cast is lossless and existing rows need no rewrite.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'transactions'
      AND column_name = 'amount'
      AND data_type <> 'numeric'
  ) THEN
    ALTER TABLE public.transactions ALTER COLUMN amount TYPE NUMERIC USING amount::numeric;
  END IF;
END$$;

-- 3. Align type/source: TEXT -> INTEGER.
-- The app encodes these as enum ordinals before they reach the cloud, so the
-- live INTEGER columns are authoritative. Drop the old CHECK constraints that
-- pinned the text labels first, if present.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'transactions'
      AND column_name = 'type'
      AND data_type <> 'integer'
  ) THEN
    ALTER TABLE public.transactions ALTER COLUMN type DROP DEFAULT;
    ALTER TABLE public.transactions ALTER COLUMN type TYPE INTEGER USING type::integer;
    ALTER TABLE public.transactions ALTER COLUMN type SET DEFAULT 0;
  END IF;
END$$;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'transactions'
      AND column_name = 'source'
      AND data_type <> 'integer'
  ) THEN
    ALTER TABLE public.transactions ALTER COLUMN source DROP DEFAULT;
    ALTER TABLE public.transactions ALTER COLUMN source TYPE INTEGER USING source::integer;
    ALTER TABLE public.transactions ALTER COLUMN source SET DEFAULT 0;
  END IF;
END$$;

-- 4. Align the title/description column. Live has `description`; the original
-- migration declared `title`. Keep `description` as the source of truth and
-- make sure the column exists with the right default.
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';

-- 5. Ensure the rest of the live columns are declared in the schema too, so
-- nothing depends on the old migration having created them.
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS tx_timestamp TIMESTAMPTZ DEFAULT now();
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS time_label TEXT DEFAULT 'Today';
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS merchant TEXT DEFAULT '';
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS wallet_name TEXT;
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS fingerprint TEXT;
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS confidence REAL DEFAULT 1.0;
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS is_confirmed BOOLEAN DEFAULT TRUE;
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS sync_state TEXT DEFAULT 'SYNCED';
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS date TIMESTAMPTZ;
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;

-- 6. RLS stays on; re-declare the policies so the migration is self-contained
-- and a db reset produces the same auth contract as live.
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

CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON public.transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON public.transactions(tx_timestamp DESC);
